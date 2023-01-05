package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
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

import static de.janno.discord.connector.api.BottomCustomIdUtils.CUSTOM_ID_DELIMITER;

@Slf4j
public abstract class AbstractCommand<C extends Config, S extends StateData> implements SlashCommand, ComponentInteractEventHandler {

    protected static final String ACTION_START = "start";
    protected static final String ACTION_HELP = "help";
    protected static final String ANSWER_TARGET_CHANNEL_OPTION = "target_channel";
    protected static final String ANSWER_FORMAT_OPTION = "answer_format";
    protected static final String RESULT_IMAGE_OPTION = "result_image";

    protected static final CommandDefinitionOption ANSWER_TARGET_CHANNEL_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(ANSWER_TARGET_CHANNEL_OPTION)
            .description("The channel where the answer will be given")
            .type(CommandDefinitionOption.Type.CHANNEL)
            .build();

    protected static final CommandDefinitionOption ANSWER_FORMAT_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(ANSWER_FORMAT_OPTION)
            .description("How the answer will be displayed")
            .type(CommandDefinitionOption.Type.STRING)
            .choices(Arrays.stream(AnswerFormatType.values())
                    .map(answerFormatType -> CommandDefinitionOptionChoice.builder()
                            .name(answerFormatType.name())
                            .value(answerFormatType.name())
                            .build())
                    .collect(Collectors.toList()))
            .build();

    protected static final CommandDefinitionOption RESULT_IMAGE_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(RESULT_IMAGE_OPTION)
            .description("If the dice should be shown as image in the result")
            .type(CommandDefinitionOption.Type.STRING)
            .choices(Arrays.stream(ResultImage.values())
                    .map(ri -> CommandDefinitionOptionChoice.builder()
                            .name(ri.name())
                            .value(ri.name())
                            .build())
                    .collect(Collectors.toList()))
            .build();

    private static final int MIN_MS_DELAY_BETWEEN_BUTTON_MESSAGES = 1000;
    private final static ConcurrentSkipListSet<Long> MESSAGE_DATA_IDS_TO_DELETE = new ConcurrentSkipListSet<>();
    protected final MessageDataDAO messageDataDAO;
    private Duration delayMessageDataDeletion = Duration.ofSeconds(10);

    protected AbstractCommand(MessageDataDAO messageDataDAO) {
        this.messageDataDAO = messageDataDAO;
    }

    @VisibleForTesting
    public void setMessageDataDeleteDuration(Duration delayMessageDataDeletion) {
        this.delayMessageDataDeletion = delayMessageDataDeletion;
    }

    protected Set<String> getStartOptionIds() {
        return Set.of(ACTION_START);
    }

    protected Optional<Long> getAnswerTargetChannelIdFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getChannelIdSubOptionWithName(ANSWER_TARGET_CHANNEL_OPTION);
    }

    protected AnswerFormatType getAnswerTypeFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getStringSubOptionWithName(ANSWER_FORMAT_OPTION)
                .map(AnswerFormatType::valueOf)
                .orElse(defaultAnswerFormat());
    }

    protected ResultImage getResultImageOptionFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getStringSubOptionWithName(RESULT_IMAGE_OPTION)
                .map(ResultImage::valueOf)
                .orElse(defaultResultImage());
    }

    protected ResultImage defaultResultImage() {
        return ResultImage.image_alie_v1;
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
        return CommandDefinition.builder()
                .name(getCommandId())
                .description(getCommandDescription())
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_START)
                        .description("Start")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .options(getStartOptions())
                        .option(ANSWER_TARGET_CHANNEL_COMMAND_OPTION)
                        .option(ANSWER_FORMAT_COMMAND_OPTION)
                        .option(RESULT_IMAGE_COMMAND_OPTION)
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_HELP)
                        .description("Help")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .build())
                .options(additionalCommandOptions())
                .build();
    }

    protected Collection<CommandDefinitionOption> additionalCommandOptions() {
        return Collections.emptyList();
    }

    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(C config, State<S> state) {
        return Optional.empty();
    }

    protected abstract Optional<ConfigAndState<C, S>> getMessageDataAndUpdateWithButtonValue(long channelId,
                                                                                             long messageId,
                                                                                             @NonNull String buttonValue,
                                                                                             @NonNull String invokingUserName);

    //visible for welcome message
    public abstract Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID,
                                                                            long guildId,
                                                                            long channelId,
                                                                            long messageId,
                                                                            @NonNull C config,
                                                                            @Nullable State<S> state);

    /**
     * update the saved state if the current button message is not deleted. StateData need to be set to null if the there is a answer message
     */
    protected void updateCurrentMessageStateData(long channelId, long messageId, @NonNull C config, @NonNull State<S> state) {
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        final long messageId = event.getMessageId();
        final long channelId = event.getChannelId();
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
            final Optional<ConfigAndState<C, S>> messageData = getMessageDataAndUpdateWithButtonValue(channelId,
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
            editMessage = getCurrentMessageContentChange(config, state).orElse(createNewButtonMessage(config).getContent());
            editMessageComponents = Optional.ofNullable(getCurrentMessageComponentChange(config, state)
                    .orElse(createNewButtonMessage(config).getComponentRowDefinitions()));
        } else {
            //edit the current message if the command changes it or mark it as processing
            editMessage = getCurrentMessageContentChange(config, state).orElse("processing ...");
            editMessageComponents = getCurrentMessageComponentChange(config, state);
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
        Optional<MessageDefinition> newButtonMessage = createNewButtonMessageWithState(config, state);

        final boolean deleteCurrentButtonMessage;
        if (newButtonMessage.isPresent() && answerTargetChannelId == null) {
            actions.add(Mono.defer(() -> event.createButtonMessage(newButtonMessage.get())
                            .flatMap(newMessageId -> {
                                final Optional<MessageDataDTO> nextMessageData = createMessageDataForNewMessage(configUUID, event.getGuildId(), channelId, newMessageId, config, state);
                                nextMessageData.ifPresent(messageDataDAO::saveMessageData);
                                return deleteOldAndConcurrentMessageAndData(newMessageId, configUUID, channelId, event);
                            })).delaySubscription(calculateDelay(event))
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
            updateCurrentMessageStateData(channelId, messageId, config, state);
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
                    messageDataDAO.deleteDataForMessage(channelId, messageId);
                }).ofType(Void.class));
    }

    @VisibleForTesting
    Mono<Void> deleteOldAndConcurrentMessageAndData(
            long newMessageId,
            @NonNull UUID configUUID,
            long channelId,
            @NonNull ButtonEventAdaptor event) {

        Set<Long> ids = messageDataDAO.getAllMessageIdsForConfig(configUUID).stream()
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

            Optional<Long> answerTargetChannelId = getAnswerTargetChannelIdFromStartCommandOption(options);
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
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), config.toShortString());

            long channelId = event.getChannelId();
            log.info("{}: '{}'",
                    event.getRequester().toLogString(),
                    commandString.replace("`", ""));
            return event.reply(commandString, false)
                    .then(event.createButtonMessage(createNewButtonMessage(config))
                            .map(newMessageId -> {
                                final Optional<MessageDataDTO> newMessageData = createMessageDataForNewMessage(UUID.randomUUID(), event.getGuildId(), channelId, newMessageId, config, null);
                                newMessageData.ifPresent(messageDataDAO::saveMessageData);
                                return newMessageId;
                            })
                            .then()
                    );

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
    protected abstract @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(C config, State<S> state);

    protected abstract @NonNull Optional<RollAnswer> getAnswer(C config, State<S> state);

    /**
     * The new button message, after a slash event
     */
    public abstract @NonNull MessageDefinition createNewButtonMessage(C config);

    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options) {
        //standard is no validation
        return Optional.empty();
    }

    protected boolean shouldKeepExistingButtonMessage(@NonNull ButtonEventAdaptor event) {
        return event.isPinned();
    }

    protected abstract @NonNull C getConfigFromStartOptions(@NonNull CommandInteractionOption options);
}
