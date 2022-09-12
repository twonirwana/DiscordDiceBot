package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static de.janno.discord.connector.api.BottomCustomIdUtils.CUSTOM_ID_DELIMITER;

@Slf4j
public abstract class AbstractCommand<C extends Config, S extends StateData> implements SlashCommand, ComponentInteractEventHandler {

    protected static final String ACTION_START = "start";
    protected static final String ACTION_HELP = "help";
    protected static final String ANSWER_TARGET_CHANNEL_OPTION = "target_channel";

    private static final CommandDefinitionOption ANSWER_TARGET_CHANNEL_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(ANSWER_TARGET_CHANNEL_OPTION)
            .description("The channel where the answer will be given")
            .type(CommandDefinitionOption.Type.CHANNEL)
            .build();
    protected final MessageDataDAO messageDataDAO;

    protected AbstractCommand(MessageDataDAO messageDataDAO) {
        this.messageDataDAO = messageDataDAO;
    }

    public static Long getOptionalLongFromArray(@NonNull String[] optionArray, int index) {
        if (optionArray.length >= index + 1 && !Strings.isNullOrEmpty(optionArray[index])) {
            return Long.parseLong(optionArray[index]);
        }
        return null;
    }

    protected Optional<Long> getAnswerTargetChannelIdFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getChannelIdSubOptionWithName(ANSWER_TARGET_CHANNEL_OPTION);
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
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_HELP)
                        .description("Help")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .build())
                .build();
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
            config = getConfigFromEvent(event);
            state = getStateFromEvent(event);
            configUUID = UUID.randomUUID();
            //we need to save the current config/state or the update will not work
            createMessageDataForNewMessage(configUUID, channelId, messageId, config, state).ifPresent(messageDataDAO::saveMessageData);
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
                log.info("Missing messageData for channelId: {}, messageId: {} and commandName: {} ", channelId, messageId, getCommandId());
                return event.reply(String.format("Configuration for the message is missing, please create a new message with the slash command `/%s start`", getCommandId()));
            }
        }
        final Long answerTargetChannelId = config.getAnswerTargetChannelId();
        Optional<String> checkPermissions = event.checkPermissions(answerTargetChannelId);
        if (checkPermissions.isPresent()) {
            return event.editMessage(checkPermissions.get(), null);
        }

        //all the answer actions
        List<Mono<Void>> actions = new ArrayList<>();
        actions.add(event.acknowledge());
        //the delete action must be the last action
        Mono<Void> createNewButtonMessageAndOptionalDeleteOld = Mono.empty();
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
        // Remove buttons on set to "processing ..."?
        actions.add(event.editMessage(editMessage, editMessageComponents.orElse(null)));

        Optional<EmbedDefinition> answer = getAnswer(config, state);
        if (answer.isPresent()) {
            BotMetrics.incrementButtonMetricCounter(getCommandId(), config.toShortString());

            actions.add(event.createResultMessageWithEventReference(answer.get(), answerTargetChannelId).then(
                    event.getRequester()
                            .doOnNext(requester -> log.info("'{}'.'{}': '{}'={} -> {} in {}ms",
                                            requester.getGuildName(),
                                            requester.getChannelName(),
                                            event.getCustomId().replace(CUSTOM_ID_DELIMITER, ":"),
                                            state.toShortString(),
                                            answer.get().toShortString(),
                                            stopwatch.elapsed(TimeUnit.MILLISECONDS)
                                    )
                            ).ofType(Void.class)));
        }
        Optional<MessageDefinition> newButtonMessage = createNewButtonMessageWithState(config, state);

        if (newButtonMessage.isPresent() && answerTargetChannelId == null) {
            Mono<Long> newMessageIdMono = event.createButtonMessage(newButtonMessage.get())
                    .map(newMessageId -> {
                        final Optional<MessageDataDTO> nextMessageData = createMessageDataForNewMessage(configUUID, channelId, newMessageId, config, state);
                        nextMessageData.ifPresent(messageDataDAO::saveMessageData);
                        return newMessageId;
                    });
            if (!keepExistingButtonMessage) {
                if (isLegacyMessage) {
                    createNewButtonMessageAndOptionalDeleteOld = event.deleteMessage(messageId, false)
                            .then(newMessageIdMono)
                            .then();
                } else {
                    //delete all other button messages with the same config, retain only the new message
                    createNewButtonMessageAndOptionalDeleteOld = deleteMessageAndData(newMessageIdMono, null, configUUID, channelId, event);
                }
            } else {
                //delete all other button messages with the same config, retain only the new and the current message
                createNewButtonMessageAndOptionalDeleteOld = deleteMessageAndData(newMessageIdMono, messageId, configUUID, channelId, event);
            }
        }
        //don't update the state data async or there will be racing conditions
        //todo update only the message if the message is not going to be deleted
        updateCurrentMessageStateData(channelId, messageId, config, state);
        return Flux.merge(1, actions.toArray(new Mono<?>[0]))
                .parallel()
                .then()
                .then(createNewButtonMessageAndOptionalDeleteOld);
    }

    private Mono<Void> deleteMessageAndData(@NonNull Mono<Long> newMessageIdMono,
                                            @Nullable Long retainMessageId,
                                            @NonNull UUID configUUID,
                                            long channelId,
                                            @NonNull ButtonEventAdaptor event) {
        return newMessageIdMono
                .flux()
                .flatMap(newMessageId -> {
                    Set<Long> ids = messageDataDAO.getAllMessageIdsForConfig(configUUID);
                    if (ids.size() > 3) { //expected one old, one new messageData and one sometimes one parallel or from the legacy migration
                        log.warn(String.format("ConfigUUID %s had %d to many messageData persisted", configUUID, ids.size() - 2));
                    }
                    return Flux.fromIterable(ids)
                            .filter(id -> filterWithOptionalSecondId(id, newMessageId, retainMessageId));
                })
                .flatMap(oldMessageId -> event.deleteMessage(oldMessageId, false)
                        .filter(Objects::nonNull)
                        .doOnNext(l -> messageDataDAO.deleteDataForMessage(channelId, l)))
                .then();
    }

    private boolean filterWithOptionalSecondId(long input, long filterId, @Nullable Long optionalFilterId) {
        if (optionalFilterId != null) {
            return !Objects.equals(input, filterId) && !Objects.equals(input, optionalFilterId);
        }
        return !Objects.equals(input, filterId);
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event) {
        Optional<String> checkPermissions = event.checkPermissions();
        if (checkPermissions.isPresent()) {
            return event.reply(checkPermissions.get());
        }

        String commandString = event.getCommandString();
        Optional<CommandInteractionOption> startOption = event.getOption(ACTION_START);
        if (startOption.isPresent()) {
            CommandInteractionOption options = startOption.get();

            Optional<Long> answerTargetChannelId = getAnswerTargetChannelIdFromStartCommandOption(options);
            if (answerTargetChannelId.isPresent() && !event.isValidAnswerChannel(answerTargetChannelId.get())) {
                log.info("Invalid answer target channel for {}", commandString);
                return event.reply("The target channel is not a valid message channel");
            }

            Optional<String> validationMessage = getStartOptionsValidationMessage(options);
            if (validationMessage.isPresent()) {
                log.info("Validation message: {} for {}", validationMessage.get(), commandString);
                return event.reply(String.format("%s\n%s", commandString, validationMessage.get()));
            }
            C config = getConfigFromStartOptions(options);
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), config.toShortString());

            long channelId = event.getChannelId();

            return event.reply(commandString)
                    .then(event.createButtonMessage(createNewButtonMessage(config))
                            .map(newMessageId -> {
                                final Optional<MessageDataDTO> newMessageData = createMessageDataForNewMessage(UUID.randomUUID(), channelId, newMessageId, config, null);
                                newMessageData.ifPresent(messageDataDAO::saveMessageData);
                                return newMessageId;
                            })
                            .then()
                    )
                    .then(event.getRequester()
                            .doOnNext(requester -> log.info("'{}'.'{}': '{}'",
                                    requester.getGuildName(),
                                    requester.getChannelName(),
                                    commandString
                            ))
                            .ofType(Void.class));

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

    protected abstract @NonNull EmbedDefinition getHelpMessage();

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

    protected abstract @NonNull Optional<EmbedDefinition> getAnswer(C config, State<S> state);

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

    /**
     * will be removed when almost all users have switched to the persisted button id
     */
    protected abstract @NonNull C getConfigFromEvent(@NonNull ButtonEventAdaptor event);

    /**
     * will be removed when almost all users have switched to the persisted button id
     */
    protected abstract @NonNull State<S> getStateFromEvent(@NonNull ButtonEventAdaptor event);

}
