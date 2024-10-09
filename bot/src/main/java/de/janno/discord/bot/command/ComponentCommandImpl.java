package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.BaseCommandUtils;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.reroll.RerollAnswerHandler;
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
        if (guildId == null) {
            BotMetrics.outsideGuildCounter("button");
        }
        final boolean isLegacyMessage = BottomCustomIdUtils.isLegacyCustomId(event.getCustomId());
        if (isLegacyMessage) {
            BotMetrics.incrementLegacyButtonMetricCounter(getCommandId());
            log.info("{}: Legacy id {}", event.getRequester().toLogString(), event.getCustomId());
            return event.reply(I18n.getMessage("base.reply.legacyButtonId", event.getRequester().getUserLocal()), false);
        }
        final String buttonValue = BottomCustomIdUtils.getButtonValueFromCustomId(event.getCustomId());
        final Optional<UUID> configUUIDFromCustomID = BottomCustomIdUtils.getConfigUUIDFromCustomId(event.getCustomId());
        BotMetrics.incrementButtonUUIDUsageMetricCounter(getCommandId(), configUUIDFromCustomID.isPresent());
        final Optional<MessageConfigDTO> messageConfigDTO = getMessageConfigDTO(configUUIDFromCustomID.orElse(null), channelId, eventMessageId);
        final Optional<ConfigAndState<C, S>> fallbackConfigAndState = createNewConfigAndStateIfMissing(buttonValue);
        if (messageConfigDTO.isEmpty() && fallbackConfigAndState.isEmpty()) {
            log.warn("{}: Missing messageData for channelId: {}, messageId: {} and commandName: {} ", event.getRequester().toLogString(), channelId, eventMessageId, getCommandId());
            return event.reply(I18n.getMessage("base.reply.missingConfig", event.getRequester().getUserLocal(), I18n.getMessage(getCommandId() + ".name", event.getRequester().getUserLocal())), false);
        }
        if (event.isPinned()) {
            BotMetrics.incrementPinnedButtonMetricCounter();
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

        //reply to acknowledge the event, no defer -> reply directly
        return replyToEvent(event, config, state, configUUID, guildId, channelId, userId, answerTargetChannelId, timer)
                .then(
                        //send answer messages
                        Mono.defer(() -> sendAnswerMessage(event, config, state, guildId, channelId, userId, answerTargetChannelId, timer))
                                //send new button message after the answer message, the new buttons message should be at the end
                                .then(Mono.defer(() -> sendNewButtonMessageAndUpdateOldButtonMessage(event, config, state, configUUID, eventMessageId, guildId, channelId, answerTargetChannelId, timer))))
                //delete old message
                .then(Mono.defer(() -> furtherAction(event, config, state, timer)))
                .doAfterTerminate(() -> {
                    //todo only on answer or button message or further
                    log.info("{}: '{}'={} in {}",
                            event.getRequester().toLogString(),
                            event.getCustomId().replace(CUSTOM_ID_DELIMITER, ":"),
                            state.toShortString(),
                            timer.toLog()
                    );
                })
                ;
    }

    private @NonNull Mono<Void> replyToEvent(final ButtonEventAdaptor event,
                                             final C config,
                                             final State<S> state,
                                             final UUID configUUID,
                                             final Long guildId,
                                             final long channelId,
                                             final long userId,
                                             final Long answerTargetChannelId, //todo move?
                                             final Timer timer
    ) {
        final Optional<String> editMessage;
        final Optional<List<ComponentRowDefinition>> editMessageComponents;
        final boolean keepExistingButtonMessage = shouldKeepExistingButtonMessage(event);
        if (keepExistingButtonMessage || answerTargetChannelId != null) {
            //if the old button is pined or the result is copied to another channel, the old message will be edited or reset to the slash default
            editMessage = getCurrentMessageContentChange(config, state)
                    .or(() -> createNewButtonMessage(configUUID, config, null, guildId, channelId).map(EmbedOrMessageDefinition::getDescriptionOrContent));
            editMessageComponents = Optional.of(getCurrentMessageComponentChange(configUUID, config, state, channelId, userId)
                    .orElse(createNewButtonMessage(configUUID, config, null, guildId, channelId)
                            .map(EmbedOrMessageDefinition::getComponentRowDefinitions).orElse(List.of())));
        } else {
            //edit the current message if the command changes it or mark it as processing
            editMessage = getCurrentMessageContentChange(config, state);
            editMessageComponents = getCurrentMessageComponentChange(configUUID, config, state, channelId, userId);
        }
        if (editMessage.isPresent() || editMessageComponents.isPresent()) {
            return event.editMessage(editMessage.orElse(null), editMessageComponents.orElse(null))
                    .doOnSuccess(v -> timer.stopReplyOrAcknowledge());
        } else {
            return event.acknowledge();
        }
    }

    private @NonNull Mono<Void> sendAnswerMessage(final ButtonEventAdaptor event,
                                                  final C config,
                                                  final State<S> state,
                                                  final Long guildId,
                                                  final long channelId,
                                                  final long userId,
                                                  final Long answerTargetChannelId,
                                                  final Timer timer) {
        final Optional<RollAnswer> answer = getAnswer(config, state, channelId, userId);
        if (answer.isPresent()) {
            BotMetrics.incrementAnswerFormatCounter(config.getAnswerFormatType(), getCommandId());
            EmbedOrMessageDefinition baseAnswer = RollAnswerConverter.toEmbedOrMessageDefinition(answer.get());
            final EmbedOrMessageDefinition answerMessage;
            if (config.getAnswerInteractionType() == AnswerInteractionType.reroll &&
                    baseAnswer.getType() == EmbedOrMessageDefinition.Type.EMBED &&
                    baseAnswer.getComponentRowDefinitions().isEmpty()) {
                answerMessage = RerollAnswerHandler.createConfigAndApplyToAnswer(config, answer.get(), baseAnswer, event.getInvokingGuildMemberName(), guildId, channelId, persistenceManager, uuidSupplier.get());
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
    private @NonNull Mono<Void> sendNewButtonMessageAndUpdateOldButtonMessage(final ButtonEventAdaptor event,
                                                                              final C config,
                                                                              final State<S> state,
                                                                              final UUID configUUID,
                                                                              final long eventMessageId,
                                                                              final Long guildId,
                                                                              final long channelId,
                                                                              final Long answerTargetChannelId,
                                                                              final Timer timer) {

        Optional<EmbedOrMessageDefinition> newButtonMessage = createNewButtonMessage(configUUID, config, state, guildId, channelId);


        if (newButtonMessage.isPresent() && answerTargetChannelId == null) {
            final boolean deleteCurrentButtonMessage = !shouldKeepExistingButtonMessage(event);
            return Mono.defer(() -> event.sendMessage(newButtonMessage.get())
                    .doOnNext(newMessageId -> createEmptyMessageData(configUUID, guildId, channelId, newMessageId))
                    //delete has a delays therefore we write the metrics after the message creation
                    .doOnNext(newMessageId -> timer.stopNewButton())
                    .flatMap(newMessageId -> MessageDeletionHelper.deleteOldMessageAndData(persistenceManager, newMessageId, event.getMessageId(), configUUID, channelId, event))
                    //todo move out and combine with the other updateCurrentMessageStateData
                    .then(Mono.defer(() -> {
                        if (deleteCurrentButtonMessage) {
                            return event.deleteMessageById(eventMessageId)
                                    .then(MessageDeletionHelper.deleteMessageDataWithDelay(persistenceManager, channelId, eventMessageId));
                        } else {
                            //update the message if the old message is kept, for example reset it to start
                            updateCurrentMessageStateData(configUUID, guildId, channelId, eventMessageId, config, state);
                            return Mono.empty();
                        }
                    }))
                    .delaySubscription(calculateDelay(event)));
        }
        //update message if no new message is sent
        updateCurrentMessageStateData(configUUID, guildId, channelId, eventMessageId, config, state);
        return Mono.empty();
    }

    private @NonNull Optional<MessageConfigDTO> getMessageConfigDTO(@Nullable UUID configId, long channelId, long messageId) {
        if (configId != null) {
            return persistenceManager.getMessageConfig(configId);
        }
        //todo remove option without configId
        return persistenceManager.getConfigFromMessage(channelId, messageId);
    }

    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID configUUID, C config, State<S> state, long channelId, long userId) {
        return Optional.empty();
    }

    /**
     * Creates a config and state if there is no saved config for a button event
     */
    protected Optional<ConfigAndState<C, S>> createNewConfigAndStateIfMissing(String buttonValue) {
        return Optional.empty();
    }

    private @NonNull MessageDataDTO getMessageDataDTOOrCreateNew(@NonNull UUID configId, @Nullable Long guildId, long channelId, long messageId) {
        Optional<MessageDataDTO> loadedData = persistenceManager.getMessageData(channelId, messageId);
        //if the messageData is missing we need to create a new one so we know the message exists and we can remove it later, even on concurrent actions
        return loadedData.orElseGet(() -> createEmptyMessageData(configId, guildId, channelId, messageId));
    }


    protected abstract ConfigAndState<C, S> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
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
    public @NonNull Optional<String> getCurrentMessageContentChange(C config, State<S> state) {
        return Optional.empty();
    }

    /**
     * The new button message, after a button event. The state can be null if the origin message was pinned
     */
    protected abstract @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessage(@NonNull UUID configId,
                                                                                          @NonNull C config,
                                                                                          @Nullable State<S> state,
                                                                                          @Nullable Long guildId,
                                                                                          long channelId);

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

    protected abstract @NonNull Optional<RollAnswer> getAnswer(C config, State<S> state, long channelId, long userId);

    /**
     * update the saved state if the current button message is not deleted. StateData need to be set to null if the there is an answer message
     */
    protected void updateCurrentMessageStateData(UUID configUUID, @Nullable Long guildId, long channelId, long messageId, @NonNull C config, @NonNull State<S> state) {
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
