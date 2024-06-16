package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.BaseCommandUtils;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.reroll.Config;
import de.janno.discord.bot.command.reroll.RerollAnswerHandler;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.ComponentInteractEventHandler;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static de.janno.discord.connector.api.BottomCustomIdUtils.CUSTOM_ID_DELIMITER;

@Slf4j
public abstract class AbstractComponentInteractEventHandler<C extends Config, S extends StateData> implements ComponentInteractEventHandler {

    protected final PersistenceManager persistenceManager;
    protected final Supplier<UUID> uuidSupplier;


    protected AbstractComponentInteractEventHandler(PersistenceManager persistenceManager, Supplier<UUID> uuidSupplier) {
        this.persistenceManager = persistenceManager;
        this.uuidSupplier = uuidSupplier;
    }

    @Override
    public final Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        final long messageId = event.getMessageId();
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
        final Optional<MessageConfigDTO> messageConfigDTO = getMessageConfigDTO(configUUIDFromCustomID.orElse(null), channelId, messageId);
        final Optional<ConfigAndState<C, S>> fallbackConfigAndState = createNewConfigAndStateIfMissing(buttonValue);
        if (messageConfigDTO.isEmpty() && fallbackConfigAndState.isEmpty()) {
            log.warn("{}: Missing messageData for channelId: {}, messageId: {} and commandName: {} ", event.getRequester().toLogString(), channelId, messageId, getCommandId());
            return event.reply(I18n.getMessage("base.reply.missingConfig", event.getRequester().getUserLocal(), I18n.getMessage(getCommandId() + ".name", event.getRequester().getUserLocal())), false);
        }
        final ConfigAndState<C, S> configAndState;
        final UUID configUUID;
        if (messageConfigDTO.isEmpty() && fallbackConfigAndState.isPresent()) {
            configAndState = fallbackConfigAndState.get();
            configUUID = configAndState.getConfigUUID();
        } else {
            configUUID = messageConfigDTO.get().getConfigUUID();
            final MessageDataDTO messageDataDTO = getMessageDataDTOOrCreateNew(configUUID, guildId, channelId, messageId);
            configAndState = getMessageDataAndUpdateWithButtonValue(messageConfigDTO.get(), messageDataDTO, buttonValue, event.getInvokingGuildMemberName());
        }
        final C config = configAndState.getConfig();
        final State<S> state = configAndState.getState();

        final Long answerTargetChannelId = config.getAnswerTargetChannelId();
        Optional<String> checkPermissions = event.checkPermissions(answerTargetChannelId, event.getRequester().getUserLocal());
        if (checkPermissions.isPresent()) {
            return event.editMessage(checkPermissions.get(), null);
        }

        //all the answer actions
        List<Mono<Void>> actions = new ArrayList<>();
        //the delete action must be the last action
        boolean keepExistingButtonMessage = shouldKeepExistingButtonMessage(event);
        String editMessage;
        Optional<List<ComponentRowDefinition>> editMessageComponents;
        if (keepExistingButtonMessage || answerTargetChannelId != null) {
            if (event.isPinned()) {
                BotMetrics.incrementPinnedButtonMetricCounter();
            }
            //if the old button is pined or the result is copied to another channel, the old message will be edited or reset to the slash default
            editMessage = getCurrentMessageContentChange(config, state).orElse(createNewButtonMessage(configUUID, config, null, guildId, channelId)
                    .map(EmbedOrMessageDefinition::getDescriptionOrContent).orElse("")); //todo empty correct?
            editMessageComponents = Optional.of(getCurrentMessageComponentChange(configUUID, config, state, channelId, userId)
                    .orElse(createNewButtonMessage(configUUID, config, null, guildId, channelId)
                            .map(EmbedOrMessageDefinition::getComponentRowDefinitions).orElse(List.of())));
        } else {
            //edit the current message if the command changes it or mark it as processing
            editMessage = getCurrentMessageContentChange(config, state).orElse(I18n.getMessage("base.edit.processing", config.getConfigLocale()));
            editMessageComponents = getCurrentMessageComponentChange(configUUID, config, state, channelId, userId);
        }
        //Todo check if message/button are the same. If the message will deleted it should always be "processing...".
        //Todo Remove buttons on set to "processing ..."?
        actions.add(Mono.defer(() -> event.editMessage(editMessage, editMessageComponents.orElse(null))));

        //todo make getAnswer a genereal EmbedOrMessage
        Optional<RollAnswer> answer = getAnswer(config, state, channelId, userId);
        if (answer.isPresent()) {
            BotMetrics.incrementButtonMetricCounter(getCommandId());
            BotMetrics.incrementAnswerFormatCounter(config.getAnswerFormatType(), getCommandId());
            EmbedOrMessageDefinition baseAnswer = RollAnswerConverter.toEmbedOrMessageDefinition(answer.get());
            final EmbedOrMessageDefinition answerMessage;
            if (config.getAnswerInteractionType() == AnswerInteractionType.reroll &&
                    baseAnswer.getType() == EmbedOrMessageDefinition.Type.EMBED &&
                    baseAnswer.getComponentRowDefinitions().isEmpty()) {
                answerMessage = RerollAnswerHandler.createConfigAndApplyToAnswer(config, answer.get(), baseAnswer, event.getInvokingGuildMemberName(), guildId, channelId, persistenceManager, uuidSupplier.get());
       /*     } else if (config.getAnswerInteractionType() == AnswerInteractionType.hidden) {
                //todo answer ephemeral, only possile with reply
                answerMessage = HiddenAnswerHandler.applyToAnswer(baseAnswer, config.getConfigLocale());*/
            } else {
                answerMessage = baseAnswer;
            }
            actions.add(Mono.defer(() -> event.sendMessage(answerMessage.toBuilder().sendToOtherChannelId(answerTargetChannelId).build()).then()
                    .doOnSuccess(v -> {
                        BotMetrics.timerAnswerMetricCounter(getCommandId(), stopwatch.elapsed());
                        log.info("{}: '{}'={} -> {} in {}ms",
                                event.getRequester().toLogString(),
                                event.getCustomId().replace(CUSTOM_ID_DELIMITER, ":"),
                                state.toShortString(),
                                answer.get().toShortString(),
                                stopwatch.elapsed(TimeUnit.MILLISECONDS)
                        );
                    })));

        }
        Optional<EmbedOrMessageDefinition> newButtonMessage = createNewButtonMessage(configUUID, config, state, guildId, channelId);

        final boolean deleteCurrentButtonMessage;
        if (newButtonMessage.isPresent() && answerTargetChannelId == null) {
            actions.add(Mono.defer(() -> event.sendMessage(newButtonMessage.get()))
                    .doOnNext(newMessageId -> createEmptyMessageData(configUUID, guildId, channelId, newMessageId))
                    .flatMap(newMessageId -> MessageDeletionHelper.deleteOldMessageAndData(persistenceManager, newMessageId, event.getMessageId(), configUUID, channelId, event))
                    .delaySubscription(calculateDelay(event))
                    .doOnSuccess(v -> {
                        BotMetrics.timerNewButtonMessageMetricCounter(getCommandId(), stopwatch.elapsed());
                        if (answer.isEmpty()) {
                            log.info("{}: '{}'={} in {}ms",
                                    event.getRequester().toLogString(),
                                    event.getCustomId().replace(CUSTOM_ID_DELIMITER, ":"),
                                    state.toShortString(),
                                    stopwatch.elapsed(TimeUnit.MILLISECONDS)
                            );
                        }
                    })
                    .then());
            deleteCurrentButtonMessage = !keepExistingButtonMessage;
        } else {
            deleteCurrentButtonMessage = false;
        }

        if (deleteCurrentButtonMessage) {
            actions.add(Mono.defer(() -> event.deleteMessageById(messageId)
                    .then(MessageDeletionHelper.deleteMessageDataWithDelay(persistenceManager, channelId, messageId))));
        } else {
            //don't update the state data async or there will be racing conditions
            updateCurrentMessageStateData(configUUID, guildId, channelId, messageId, config, state);
        }
        addFurtherActions(actions, event, config, state);
        return Flux.merge(1, actions.toArray(new Mono<?>[0]))
                .parallel()
                .then();
    }

    private @NonNull Optional<MessageConfigDTO> getMessageConfigDTO(@Nullable UUID configId, long channelId, long messageId) {
        if (configId != null) {
            return persistenceManager.getMessageConfig(configId);
        }
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
        return BaseCommandUtils.createEmptyMessageData(configUUID, guildId, channelId, messageId, getCommandId(), persistenceManager);
    }

    protected abstract @NonNull Optional<RollAnswer> getAnswer(C config, State<S> state, long channelId, long userId);

    /**
     * update the saved state if the current button message is not deleted. StateData need to be set to null if the there is a answer message
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
            log.info("{}: Delaying button message creation for {}ms", event.getRequester().toLogString(), delay);
            return Duration.ofMillis(delay);
        }
        BotMetrics.incrementDelayCounter(getCommandId(), false);
        return Duration.ZERO;
    }

    protected void addFurtherActions(List<Mono<Void>> actions, ButtonEventAdaptor event, C config, State<S> state) {

    }
}
