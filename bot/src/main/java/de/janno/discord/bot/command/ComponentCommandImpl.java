package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.BaseCommandUtils;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.reroll.RerollAnswerHandler;
import de.janno.discord.bot.command.starter.StarterCommand;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.ComponentCommand;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static de.janno.discord.connector.api.BottomCustomIdUtils.CUSTOM_ID_DELIMITER;

@Slf4j
public abstract class ComponentCommandImpl<C extends RollConfig, S extends StateData> implements ComponentCommand {

    protected final PersistenceManager persistenceManager;
    protected final Supplier<UUID> uuidSupplier;

    protected ComponentCommandImpl(PersistenceManager persistenceManager, Supplier<UUID> uuidSupplier) {
        this.persistenceManager = persistenceManager;
        this.uuidSupplier = uuidSupplier;
    }

    @Override
    public final Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event) {
        final Timer timer = new Timer(getCommandId());
        final long eventMessageId = event.getMessageId();
        final long channelId = event.getChannelId();
        final long userId = event.getUserId();
        final Long guildId = event.getGuildId();
        final boolean isLegacyMessage = BottomCustomIdUtils.isLegacyCustomId(event.getCustomId());
        if (isLegacyMessage) {
            BotMetrics.incrementLegacyButtonMetricCounter(getCommandId());
            log.info("{}: Legacy id {}", event.getRequester().toLogString(), event.getCustomId());
            return event.reply(I18n.getMessage("base.reply.legacyButtonId", event.getRequester().getUserLocal()), false);
        }
        final String buttonValue = BottomCustomIdUtils.getButtonValueFromCustomId(event.getCustomId());
        final Optional<UUID> configUUIDFromCustomID = BottomCustomIdUtils.getConfigUUIDFromCustomId(event.getCustomId());
        BotMetrics.incrementButtonMetricCounter(getCommandId());
        if (guildId == null) {
            BotMetrics.outsideGuildCounter("button");
        }
        if (event.isPinned()) {
            BotMetrics.incrementPinnedButtonMetricCounter();
        }
        BotMetrics.incrementButtonUUIDUsageMetricCounter(getCommandId(), configUUIDFromCustomID.isPresent());

        final Optional<MessageConfigDTO> messageConfigDTO = getMessageConfigDTO(configUUIDFromCustomID.orElse(null), channelId, eventMessageId);
        final Optional<ConfigAndState<C, S>> fallbackConfigAndState = createNewConfigAndStateIfMissing(buttonValue);
        if (messageConfigDTO.isEmpty() && fallbackConfigAndState.isEmpty()) {
            log.warn("{}: Missing messageData for channelId: {}, messageId: {} and commandName: {} ", event.getRequester().toLogString(), channelId, eventMessageId, getCommandId());
            return event.reply(I18n.getMessage("base.reply.missingConfig", event.getRequester().getUserLocal(), I18n.getMessage(getCommandId() + ".name", event.getRequester().getUserLocal())), false);
        }

        final ConfigAndState<C, S> configAndState;
        final UUID configUUID;
        if (messageConfigDTO.isEmpty() && fallbackConfigAndState.isPresent()) {
            configAndState = fallbackConfigAndState.get();
            configUUID = configAndState.getConfigUUID();
        } else {
            configUUID = messageConfigDTO.get().getConfigUUID();
            final MessageDataDTO messageDataDTO = getMessageDataDTOOrCreateNew(configUUID, guildId, channelId, eventMessageId);
            configAndState = getMessageDataAndUpdateWithButtonValue(messageConfigDTO.get(), messageDataDTO, buttonValue, event.getInvokingGuildMemberName());
        }
        final C config = configAndState.getConfig();
        final State<S> state = configAndState.getState();

        final Long answerTargetChannelId = config.getAnswerTargetChannelId();
        Optional<String> checkPermissions = event.checkPermissions(answerTargetChannelId, event.getRequester().getUserLocal());
        if (checkPermissions.isPresent()) {
            return event.editMessage(checkPermissions.get(), null);
        }

        //update state bevor editing the event message
        updateCurrentMessageStateData(configUUID, guildId, channelId, eventMessageId, config, state);
        //reply/edit/acknowledge to the event message, no defer but directly call
        return replyToEvent(event, config, state, configUUID, channelId, userId, answerTargetChannelId, timer)
                //send answer messages
                .then(Mono.defer(() -> sendAnswerMessage(event, config, state, guildId, channelId, userId, answerTargetChannelId, timer))
                        //create an optional new button message
                        .then(Mono.defer(() -> createNewButtonMessage(configUUID, config, state, guildId, channelId, userId).map(Mono::just).orElse(Mono.empty())
                                //update the new button message if a starter uuid is set
                                .flatMap(bm -> {
                                    if (config.getCallStarterConfigAfterFinish() != null) {
                                        return StarterCommand.getStarterMessage(persistenceManager, config.getCallStarterConfigAfterFinish()).map(Mono::just).orElse(Mono.empty());
                                    }
                                    return Mono.just(bm);
                                })
                                //send the new button message, flatMap because we do it only if a new buttonMessage should be created
                                .flatMap(nbm -> Mono.defer(() -> sendNewButtonMessage(event, nbm, configUUID, guildId, channelId, answerTargetChannelId, timer))
                                        //delete the event message and event data if a new buttonMessage was created, flatMap because we do it only if a new buttonMessage is created
                                        .flatMap(newButtonMessageId -> Mono.defer(() -> deleteEventMessageAndData(event, configUUID, eventMessageId, channelId, answerTargetChannelId, newButtonMessageId))))

                        )))
                //do further actions last
                .then(Mono.defer(() -> furtherAction(event, config, state, timer)))
                .doAfterTerminate(() -> {
                    //only logging if there is an answer or new button message, and not if the event message was only edited
                    if (timer.getAnswerFinished() != null || timer.getNewButtonFinished() != null || timer.getFurtherActionFinished() != null) {
                        log.info("{}: '{}'={} in {}",
                                event.getRequester().toLogString(),
                                event.getCustomId().replace(CUSTOM_ID_DELIMITER, ":"),
                                state.toShortString(),
                                timer.toLog()
                        );
                    }
                });
    }

    /**
     * reply to the event by simply acknowledge, edit the message or sending a reply message
     */
    private @NonNull Mono<Void> replyToEvent(@NonNull final ButtonEventAdaptor event,
                                             @NonNull final C config,
                                             @NonNull final State<S> state,
                                             @NonNull final UUID configUUID,
                                             final long channelId,
                                             final long userId,
                                             final Long answerTargetChannelId,
                                             @NonNull final Timer timer) {
        final Optional<String> editMessage;
        final Optional<List<ComponentRowDefinition>> editMessageComponents;
        final boolean keepExistingButtonMessage = shouldKeepExistingButtonMessage(event) || answerTargetChannelId != null;

        //edit the current message if the command changes it or mark it as processing
        editMessage = getCurrentMessageContentChange(config, state, keepExistingButtonMessage);
        editMessageComponents = getCurrentMessageComponentChange(configUUID, config, state, channelId, userId, keepExistingButtonMessage);

        timer.stopStartAcknowledge();
        if (editMessage.isPresent() || editMessageComponents.isPresent()) {
            return event.editMessage(editMessage.orElse(null), editMessageComponents.orElse(null))
                    .doOnSuccess(v -> timer.stopReplyFinished());
        } else {
            return event.acknowledge()
                    .doOnSuccess(v -> timer.stopAcknowledgeFinished());
        }
    }

    private @NonNull Mono<Void> sendAnswerMessage(@NonNull final ButtonEventAdaptor event,
                                                  @NonNull final C config,
                                                  @NonNull final State<S> state,
                                                  final Long guildId,
                                                  final long channelId,
                                                  final long userId,
                                                  final Long answerTargetChannelId,
                                                  @NonNull final Timer timer) {
        final Optional<RollAnswer> answer = getAnswer(config, state, channelId, userId);
        if (answer.isPresent()) {
            BotMetrics.incrementAnswerFormatCounter(config.getAnswerFormatType(), getCommandId());
            EmbedOrMessageDefinition baseAnswer = RollAnswerConverter.toEmbedOrMessageDefinition(answer.get());
            final EmbedOrMessageDefinition answerMessage;
            if (config.getAnswerInteractionType() == AnswerInteractionType.reroll &&
                    baseAnswer.getType() == EmbedOrMessageDefinition.Type.EMBED &&
                    baseAnswer.getComponentRowDefinitions().isEmpty()) {
                answerMessage = RerollAnswerHandler.createConfigAndApplyToAnswer(config, answer.get(), baseAnswer, event.getInvokingGuildMemberName(), guildId, channelId, userId, persistenceManager, uuidSupplier.get());
            } else {
                answerMessage = baseAnswer;
            }
            return event.sendMessage(answerMessage.toBuilder().sendToOtherChannelId(answerTargetChannelId).build()).then()
                    .doOnSuccess(v -> timer.stopAnswer());

        }
        return Mono.empty();
    }

    /**
     * Sends a new button message if needed. If a new button message was send then it returns true otherwise false
     */
    private @NonNull Mono<Long> sendNewButtonMessage(@NonNull final ButtonEventAdaptor event,
                                                     @NonNull final EmbedOrMessageDefinition newButtonMessage,
                                                     @NonNull final UUID configUUID,
                                                     final Long guildId,
                                                     final long channelId,
                                                     final Long answerTargetChannelId,
                                                     @NonNull final Timer timer) {


        if (answerTargetChannelId == null) {
            return Mono.defer(() -> event.sendMessage(newButtonMessage)
                    .doOnNext(newMessageId -> createEmptyMessageData(configUUID, guildId, channelId, newMessageId))
                    .doOnNext(newMessageId -> timer.stopNewButton())
                    .delaySubscription(calculateDelay(event)));
        }
        return Mono.empty();
    }

    /**
     * deletes the old eventMessage and deletes it data
     */
    private @NonNull Mono<Void> deleteEventMessageAndData(@NonNull final ButtonEventAdaptor event,
                                                          @NonNull final UUID configUUID,
                                                          final long eventMessageId,
                                                          final long channelId,
                                                          final Long answerTargetChannelId,
                                                          final long newButtonMessageId) {

        return Mono.defer(() -> MessageDeletionHelper.deleteOldMessageAndData(persistenceManager, newButtonMessageId, event.getMessageId(), configUUID, channelId, event))
                .then(Mono.defer(() -> {
                    if (answerTargetChannelId == null && !shouldKeepExistingButtonMessage(event)) {
                        return Mono.defer(() -> event.deleteMessageById(eventMessageId))
                                .then(Mono.defer(() -> MessageDeletionHelper.deleteMessageDataWithDelay(persistenceManager, channelId, eventMessageId)));
                    } else {
                        return Mono.empty();
                    }
                }));
    }

    private @NonNull Optional<MessageConfigDTO> getMessageConfigDTO(@Nullable UUID configId, long channelId,
                                                                    long messageId) {
        if (configId != null) {
            return persistenceManager.getMessageConfig(configId);
        }
        //todo remove option without configId
        return persistenceManager.getConfigFromMessage(channelId, messageId);
    }

    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID configUUID, C config, State<S> state, long channelId, long userId, boolean keepExistingButtonMessage) {
        return Optional.empty();
    }

    /**
     * Creates a config and state if there is no saved config for a button event
     */
    protected Optional<ConfigAndState<C, S>> createNewConfigAndStateIfMissing(String buttonValue) {
        return Optional.empty();
    }

    private @NonNull MessageDataDTO getMessageDataDTOOrCreateNew(@NonNull UUID configId, @Nullable Long guildId,
                                                                 long channelId, long messageId) {
        Optional<MessageDataDTO> loadedData = persistenceManager.getMessageData(channelId, messageId);
        //if the messageData is missing we need to create a new one so we know the message exists and we can remove it later, even on concurrent actions
        return loadedData.orElseGet(() -> createEmptyMessageData(configId, guildId, channelId, messageId));
    }


    protected abstract ConfigAndState<C, S> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO
                                                                                           messageConfigDTO,
                                                                                   @NonNull MessageDataDTO messageDataDTO,
                                                                                   @NonNull String buttonValue,
                                                                                   @NonNull String invokingUserName);

    protected boolean shouldKeepExistingButtonMessage(@NonNull ButtonEventAdaptor event) {
        return event.isPinned();
    }

    /**
     * The text content for the old button message, after a button event. Returns null means no editing should be done.
     */
    @VisibleForTesting
    public @NonNull Optional<String> getCurrentMessageContentChange(C config, State<S> state, boolean keepExistingButtonMessage) {
        return Optional.empty();
    }

    /**
     * The new button message, after a button event. The state can be null if the origin message was pinned
     */
    protected abstract @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessage(@NonNull UUID configId,
                                                                                          @NonNull C config,
                                                                                          @Nullable State<S> state,
                                                                                          @Nullable Long guildId,
                                                                                          long channelId,
                                                                                          long userId
    );

    /**
     * On the creation of a message an empty state need to be saved so we know the message exists and we can remove it later, even on concurrent actions
     */
    @VisibleForTesting
    public MessageDataDTO createEmptyMessageData(@NonNull UUID configUUID,
                                                 @Nullable Long guildId,
                                                 long channelId,
                                                 long messageId) {
        return BaseCommandUtils.createCleanupAndSaveEmptyMessageData(configUUID, guildId, channelId, messageId, getCommandId(), persistenceManager);
    }

    protected abstract @NonNull Optional<RollAnswer> getAnswer(C config, State<S> state, long channelId,
                                                               long userId);

    /**
     * update the saved state if the current button message is not deleted. StateData need to be set to null if the there is an answer message
     */
    protected void updateCurrentMessageStateData(UUID configUUID, @Nullable Long guildId, long channelId,
                                                 long messageId, @NonNull C config, @NonNull State<S> state) {
    }

    private Duration calculateDelay(ButtonEventAdaptor event) {
        OffsetDateTime now = OffsetDateTime.now();
        //if for some reason the creation time is after now we set it to 0
        long milliBetween = Math.max(ChronoUnit.MILLIS.between(event.getMessageCreationTime(), now), 0);
        final int delayBetweenButtonMessages = io.avaje.config.Config.getInt("command.minDelayBetweenButtonMessagesMs", 1000);
        if (milliBetween < delayBetweenButtonMessages) {
            BotMetrics.incrementDelayCounter(getCommandId(), true);
            long delay = delayBetweenButtonMessages - milliBetween;
            BotMetrics.delayTimer(getCommandId(), Duration.ofMillis(delay));
            if (delay < 300) {
                log.trace("{}: Delaying button message creation for {}ms", event.getRequester().toLogString(), delay);
            } else if (delay < 600) {
                log.debug("{}: Delaying button message creation for {}ms", event.getRequester().toLogString(), delay);
            } else {
                log.info("{}: Delaying button message creation for {}ms", event.getRequester().toLogString(), delay);
            }
            return Duration.ofMillis(delay);
        }
        BotMetrics.incrementDelayCounter(getCommandId(), false);
        return Duration.ZERO;
    }

    protected Mono<Void> furtherAction(ButtonEventAdaptor event, C config, State<S> state, Timer timer) {
        return Mono.empty();
    }
}
