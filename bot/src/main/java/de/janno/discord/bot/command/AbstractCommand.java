package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static de.janno.discord.bot.command.DefaultCommandOptions.*;
import static de.janno.discord.connector.api.BottomCustomIdUtils.CUSTOM_ID_DELIMITER;

@Slf4j
public abstract class AbstractCommand<C extends Config, S extends StateData> implements SlashCommand, ComponentInteractEventHandler {

    private static final String ACTION_START = "start";
    private static final String ACTION_HELP = "help";

    private static final int MIN_MS_DELAY_BETWEEN_BUTTON_MESSAGES = 1000;
    private final static ConcurrentSkipListSet<Long> MESSAGE_DATA_IDS_TO_DELETE = new ConcurrentSkipListSet<>();
    protected final PersistenceManager persistenceManager;
    private Duration delayMessageDataDeletion = Duration.ofSeconds(10);

    protected AbstractCommand(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @VisibleForTesting
    public void setMessageDataDeleteDuration(Duration delayMessageDataDeletion) {
        this.delayMessageDataDeletion = delayMessageDataDeletion;
    }

    protected Set<String> getStartOptionIds() {
        return Set.of(ACTION_START);
    }


    protected ResultImage defaultResultImage() {
        return ResultImage.polyhedral_3d_red_and_white;
    }

    protected AnswerFormatType defaultAnswerFormat() {
        return AnswerFormatType.full;
    }

    @Override
    public boolean matchingComponentCustomId(@NonNull String buttonCustomId) {
        if (BottomCustomIdUtils.isLegacyCustomId(buttonCustomId)) {
            return BottomCustomIdUtils.matchesLegacyCustomId(buttonCustomId, getCommandId());
        }
        return Objects.equals(getCommandId(), BottomCustomIdUtils.getCommandNameFromCustomIdWithPersistence(buttonCustomId));
    }

    @Override
    public CommandDefinition getCommandDefinition() {
        List<CommandDefinitionOption> baseOptions = new ArrayList<>();
        if (supportsTargetChannel()) {
            baseOptions.add(ANSWER_TARGET_CHANNEL_COMMAND_OPTION);
        }
        if (supportsAnswerFormat()) {
            baseOptions.add(ANSWER_FORMAT_COMMAND_OPTION);
        }
        if (supportsResultImages()) {
            baseOptions.add(RESULT_IMAGE_COMMAND_OPTION);
        }
        return CommandDefinition.builder()
                .name(getCommandId())
                .description(getCommandDescription())
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_START)
                        .description("Start")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .options(getStartOptions())
                        .options(baseOptions)
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_HELP)
                        .description("Help")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .build())
                .options(additionalCommandOptions())
                .build();
    }

    protected boolean supportsResultImages() {
        return true;
    }

    protected boolean supportsAnswerFormat() {
        return true;
    }

    protected boolean supportsTargetChannel() {
        return true;
    }

    protected Collection<CommandDefinitionOption> additionalCommandOptions() {
        return Collections.emptyList();
    }

    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID configUUID, C config, State<S> state) {
        return Optional.empty();
    }

    protected abstract Optional<ConfigAndState<C, S>> getMessageDataAndUpdateWithButtonValue(@Nullable UUID configId,
                                                                                             long channelId,
                                                                                             long messageId,
                                                                                             @NonNull String buttonValue,
                                                                                             @NonNull String invokingUserName);

    protected @NonNull Optional<MessageConfigDTO> getMessageConfigDTO(@Nullable UUID configId, long channelId, long messageId) {
        if (configId != null) {
            return persistenceManager.getConfig(configId);
        }
        return persistenceManager.getConfigFromMessage(channelId, messageId);
    }

    //visible for welcome command
    public abstract Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID,
                                                                   long guildId,
                                                                   long channelId,
                                                                   @NonNull C config);

    /**
     * update the saved state if the current button message is not deleted. StateData need to be set to null if the there is a answer message
     */
    protected void updateCurrentMessageStateData(UUID configUUID, long guildId, long channelId, long messageId, @NonNull C config, @NonNull State<S> state) {
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        final long messageId = event.getMessageId();
        final long channelId = event.getChannelId();
        final long guildId = event.getGuildId();
        final boolean isLegacyMessage = BottomCustomIdUtils.isLegacyCustomId(event.getCustomId());
        final C config;
        final State<S> state;
        final UUID configUUID;
        if (isLegacyMessage) {
            BotMetrics.incrementLegacyButtonMetricCounter(getCommandId());
            log.info("{}: Legacy id {}", event.getRequester().toLogString(), event.getCustomId());
            return event.reply("The button uses an old format that isn't supported anymore. Please delete it and create a new button message with a slash command.", false);
        } else {
            final String buttonValue = BottomCustomIdUtils.getButtonValueFromCustomId(event.getCustomId());
            final Optional<UUID> configUUIDFromCustomID = BottomCustomIdUtils.getConfigUUIDFromCustomIdWithPersistence(event.getCustomId());
            //todo metrics
            final Optional<ConfigAndState<C, S>> messageData = getMessageDataAndUpdateWithButtonValue(configUUIDFromCustomID.orElse(null), channelId,
                    messageId,
                    buttonValue,
                    event.getInvokingGuildMemberName());
            if (messageData.isPresent()) {
                config = messageData.get().getConfig();
                state = messageData.get().getState();
                configUUID = messageData.get().getConfigUUID();
            } else {
                log.warn("{}: Missing messageData for channelId: {}, messageId: {} and commandName: {} ", event.getRequester().toLogString(), channelId, messageId, getCommandId());
                return event.reply(String.format("Configuration for the message is missing, please create a new message with the slash command `/%s start`", getCommandId()), false);
            }
        }
        final Long answerTargetChannelId = config.getAnswerTargetChannelId();
        Optional<String> checkPermissions = event.checkPermissions(answerTargetChannelId);
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
            //if the old button is pined or the result is copied to another channel, the old message will be edited or reset to the slash default
            editMessage = getCurrentMessageContentChange(config, state).orElse(createNewButtonMessage(configUUID, config).getContent());
            editMessageComponents = Optional.ofNullable(getCurrentMessageComponentChange(configUUID, config, state)
                    .orElse(createNewButtonMessage(configUUID, config).getComponentRowDefinitions()));
        } else {
            //edit the current message if the command changes it or mark it as processing
            editMessage = getCurrentMessageContentChange(config, state).orElse("processing ...");
            editMessageComponents = getCurrentMessageComponentChange(configUUID, config, state);
        }
        //Todo check if message/button are the same. If the message will deleted it should always be "processing...".
        //Todo Remove buttons on set to "processing ..."?
        actions.add(Mono.defer(() -> event.editMessage(editMessage, editMessageComponents.orElse(null))));

        Optional<RollAnswer> answer = getAnswer(config, state);
        if (answer.isPresent()) {
            BotMetrics.incrementButtonMetricCounter(getCommandId(), config.toShortString());
            BotMetrics.incrementAnswerFormatCounter(config.getAnswerFormatType(), getCommandId());

            actions.add(Mono.defer(() -> event.createResultMessageWithEventReference(RollAnswerConverter.toEmbedOrMessageDefinition(answer.get()), answerTargetChannelId)
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
        Optional<MessageDefinition> newButtonMessage = createNewButtonMessageWithState(configUUID, config, state, guildId, channelId);

        final boolean deleteCurrentButtonMessage;
        if (newButtonMessage.isPresent() && answerTargetChannelId == null) {
            actions.add(Mono.defer(() -> event.createButtonMessage(newButtonMessage.get()))
                    .flatMap(newMessageId -> deleteOldAndConcurrentMessageAndData(newMessageId, configUUID, channelId, event))
                    .delaySubscription(calculateDelay(event))
                    .doOnSuccess(v -> BotMetrics.timerNewButtonMessageMetricCounter(getCommandId(), stopwatch.elapsed()))
                    .then());
            deleteCurrentButtonMessage = !keepExistingButtonMessage;
        } else {
            deleteCurrentButtonMessage = false;
        }

        if (deleteCurrentButtonMessage) {
            actions.add(Mono.defer(() -> event.deleteMessageById(messageId)
                    .then(deleteMessageDataWithDelay(channelId, messageId))));
        } else {
            //don't update the state data async or there will be racing conditions
            updateCurrentMessageStateData(configUUID, guildId, channelId, messageId, config, state);
        }

        return Flux.merge(1, actions.toArray(new Mono<?>[0]))
                .parallel()
                .then();
    }

    private Duration calculateDelay(ButtonEventAdaptor event) {
        long milliBetween = ChronoUnit.MILLIS.between(event.getMessageCreationTime(), OffsetDateTime.now());
        if (milliBetween < MIN_MS_DELAY_BETWEEN_BUTTON_MESSAGES) {
            BotMetrics.incrementDelayCounter(getCommandId(), true);
            long delay = MIN_MS_DELAY_BETWEEN_BUTTON_MESSAGES - milliBetween;
            BotMetrics.delayTimer(getCommandId(), Duration.ofMillis(delay));
            log.info("{}: Delaying button message creation for {}ms", event.getRequester().toLogString(), delay);
            return Duration.ofMillis(delay);
        }
        BotMetrics.incrementDelayCounter(getCommandId(), false);
        return Duration.ZERO;
    }

    private Mono<Void> deleteMessageDataWithDelay(long channelId, long messageId) {
        MESSAGE_DATA_IDS_TO_DELETE.add(messageId);
        return Mono.defer(() -> Mono.just(0)
                .delayElement(delayMessageDataDeletion)
                .doOnNext(v -> {
                    MESSAGE_DATA_IDS_TO_DELETE.remove(messageId);
                    persistenceManager.deleteStateForMessage(channelId, messageId);
                }).ofType(Void.class));
    }

    @VisibleForTesting
    Mono<Void> deleteOldAndConcurrentMessageAndData(
            long newMessageId,
            @NonNull UUID configUUID,
            long channelId,
            @NonNull ButtonEventAdaptor event) {

        Set<Long> ids = persistenceManager.getAllMessageIdsForConfig(configUUID).stream()
                //this will already delete directly
                .filter(id -> id != event.getMessageId())
                //we don't want to delete the new message
                .filter(id -> id != newMessageId)
                //we don't want to check the state of messages where the data is already scheduled to be deleted
                .filter(id -> !MESSAGE_DATA_IDS_TO_DELETE.contains(id))
                .collect(Collectors.toSet());

        if (ids.size() > 5) { //there should be not many old message data
            log.warn(String.format("ConfigUUID %s had %d to many messageData persisted", configUUID, ids.size()));
        }

        if (ids.isEmpty()) {
            return Mono.empty();
        }

        return event.getMessagesState(ids)
                .flatMap(ms -> {
                    if (ms.isCanBeDeleted() && !ms.isPinned() && ms.isExists() && ms.getCreationTime() != null) {
                        return event.deleteMessageById(ms.getMessageId())
                                .then(deleteMessageDataWithDelay(channelId, ms.getMessageId()));
                    } else if (!ms.isExists()) {
                        return deleteMessageDataWithDelay(channelId, ms.getMessageId());
                    } else {
                        return Mono.empty();
                    }
                }).then();
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event) {
        Optional<String> checkPermissions = event.checkPermissions();
        if (checkPermissions.isPresent()) {
            return event.reply(checkPermissions.get(), false);
        }

        String commandString = event.getCommandString();
        Optional<CommandInteractionOption> startOption = getStartOptionIds().stream()
                .sorted() //for deterministic tests
                .map(event::getOption)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        if (startOption.isPresent()) {
            CommandInteractionOption options = startOption.get();

            Optional<Long> answerTargetChannelId = DefaultCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options);
            if (answerTargetChannelId.isPresent() && !event.isValidAnswerChannel(answerTargetChannelId.get())) {
                log.info("{}: Invalid answer target channel for {}", event.getRequester().toLogString(), commandString);
                return event.reply("The target channel is not a valid message channel", true);
            }

            Optional<String> validationMessage = getStartOptionsValidationMessage(options);
            if (validationMessage.isPresent()) {
                log.info("{}: Validation message: {} for {}", event.getRequester().toLogString(),
                        validationMessage.get(),
                        commandString);
                return event.reply(String.format("%s\n%s", commandString, validationMessage.get()), true);
            }
            C config = getConfigFromStartOptions(options);
            UUID configUUID = UUID.randomUUID();
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), config.toShortString());

            long channelId = event.getChannelId();
            log.info("{}: '{}'",
                    event.getRequester().toLogString(),
                    commandString.replace("`", ""));
            return event.reply(commandString, false)
                    .then(Mono.defer(() -> {
                        final Optional<MessageConfigDTO> newMessageConfig = createMessageConfig(configUUID, event.getGuildId(), channelId, config);
                        newMessageConfig.ifPresent(persistenceManager::saveConfig);
                        return event.createButtonMessage(createNewButtonMessage(configUUID, config)).then();
                    }));

        } else if (event.getOption(ACTION_HELP).isPresent()) {
            BotMetrics.incrementSlashHelpMetricCounter(getCommandId());
            return event.replyEmbed(getHelpMessage(), true);
        }
        return Mono.empty();
    }

    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of();
    }

    protected abstract @NonNull String getCommandDescription();

    protected abstract @NonNull EmbedOrMessageDefinition getHelpMessage();

    /**
     * The text content for the old button message, after a button event. Returns null means no editing should be done.
     */
    @VisibleForTesting
    public @NonNull Optional<String> getCurrentMessageContentChange(C config, State<S> state) {
        return Optional.empty();
    }

    /**
     * The new button message, after a button event
     */
    protected abstract @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(UUID configId, C config, State<S> state, long guildId, long channelId);

    protected abstract @NonNull Optional<RollAnswer> getAnswer(C config, State<S> state);

    /**
     * The new button message, after a slash event
     */
    public abstract @NonNull MessageDefinition createNewButtonMessage(UUID configId, C config);

    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options) {
        //standard is no validation
        return Optional.empty();
    }

    protected boolean shouldKeepExistingButtonMessage(@NonNull ButtonEventAdaptor event) {
        return event.isPinned();
    }

    protected abstract @NonNull C getConfigFromStartOptions(@NonNull CommandInteractionOption options);
}
