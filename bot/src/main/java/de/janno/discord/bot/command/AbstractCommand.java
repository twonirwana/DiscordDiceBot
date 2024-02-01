package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.janno.discord.bot.command.BaseCommandOptions.*;
import static de.janno.discord.connector.api.BottomCustomIdUtils.CUSTOM_ID_DELIMITER;

@Slf4j
public abstract class AbstractCommand<C extends Config, S extends StateData> implements SlashCommand, ComponentInteractEventHandler {

    private static final String START_OPTION_NAME = "start";
    private static final String HELP_OPTION_NAME = "help";

    private static final int MIN_MS_DELAY_BETWEEN_BUTTON_MESSAGES = io.avaje.config.Config.getInt("command.minDelayBetweenButtonMessagesMs", 1000);
    protected final PersistenceManager persistenceManager;

    protected AbstractCommand(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    protected AnswerFormatType defaultAnswerFormat() {
        return AnswerFormatType.full;
    }

    @Override
    public boolean matchingComponentCustomId(@NonNull String buttonCustomId) {
        if (BottomCustomIdUtils.isLegacyCustomId(buttonCustomId)) {
            return BottomCustomIdUtils.matchesLegacyCustomId(buttonCustomId, getCommandId());
        }
        return Objects.equals(getCommandId(), BottomCustomIdUtils.getCommandNameFromCustomId(buttonCustomId));
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        List<CommandDefinitionOption> baseOptions = new ArrayList<>();
        if (supportsTargetChannel()) {
            baseOptions.add(ANSWER_TARGET_CHANNEL_COMMAND_OPTION);
        }
        if (supportsAnswerFormat()) {
            baseOptions.add(ANSWER_FORMAT_COMMAND_OPTION);
        }
        if (supportsResultImages()) {
            baseOptions.add(DICE_IMAGE_STYLE_COMMAND_OPTION);
            baseOptions.add(DICE_IMAGE_COLOR_COMMAND_OPTION);
        }
        if (supportsLocale()) {
            baseOptions.add(LOCALE_COMMAND_OPTION);
        }
        return CommandDefinition.builder()
                .name(getCommandId())
                .nameLocales(I18n.allNoneEnglishMessagesNames("%s.name".formatted(getCommandId())))
                .description(I18n.getMessage("%s.description".formatted(getCommandId()), Locale.ENGLISH)) //not visible, because the description of the first option will be shown
                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("%s.description".formatted(getCommandId()))) //not visible, because the description of the first option will be shown
                .option(CommandDefinitionOption.builder()
                        .name(START_OPTION_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("base.option.start"))
                        .description(I18n.getMessage("%s.description".formatted(getCommandId()), Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("%s.description".formatted(getCommandId())))
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .options(getStartOptions())
                        .options(baseOptions)
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(HELP_OPTION_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("base.option.help"))
                        .description(I18n.getMessage("base.help.description", Locale.ENGLISH, (I18n.getMessage("%s.name".formatted(getCommandId()), Locale.ENGLISH))))
                        .descriptionLocales(I18n.allNoneEnglishDescriptionsWithKeys("base.help.description", "%s.name".formatted(getCommandId())))
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

    protected boolean supportsLocale() {
        return true;
    }


    protected boolean supportsTargetChannel() {
        return true;
    }

    protected Collection<CommandDefinitionOption> additionalCommandOptions() {
        return Collections.emptyList();
    }

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest autoCompleteRequest, @NonNull Locale userLocale) {
        return BaseCommandOptions.autoCompleteColorOption(autoCompleteRequest, userLocale);
    }

    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID configUUID, C config, State<S> state, long channelId, long userId) {
        return Optional.empty();
    }

    protected abstract ConfigAndState<C, S> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                   @NonNull MessageDataDTO messageDataDTO,
                                                                                   @NonNull String buttonValue,
                                                                                   @NonNull String invokingUserName);

    protected @NonNull Optional<MessageConfigDTO> getMessageConfigDTO(@Nullable UUID configId, long channelId, long messageId) {
        if (configId != null) {
            return persistenceManager.getMessageConfig(configId);
        }
        return persistenceManager.getConfigFromMessage(channelId, messageId);
    }

    private @NonNull MessageDataDTO getMessageDataDTOOrCreateNew(@NonNull UUID configId, long guildId, long channelId, long messageId) {
        Optional<MessageDataDTO> loadedData = persistenceManager.getMessageData(channelId, messageId);
        //if the messageData is missing we need to create a new one so we know the message exists and we can remove it later, even on concurrent actions
        return loadedData.orElseGet(() -> createEmptyMessageData(configId, guildId, channelId, messageId));
    }

    /**
     * On the creation of a message an empty state need to be saved so we know the message exists and we can remove it later, even on concurrent actions
     */
    @VisibleForTesting
    public MessageDataDTO createEmptyMessageData(@NonNull UUID configUUID,
                                                 long guildId,
                                                 long channelId,
                                                 long messageId) {
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), Mapper.NO_PERSISTED_STATE, null);
        //should not be needed but sometimes there is a retry ect and then there is already a state
        persistenceManager.deleteStateForMessage(channelId, messageId);
        persistenceManager.saveMessageData(messageDataDTO);
        return messageDataDTO;
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

    /**
     * Creates a config and state if there is no saved config for a button event
     */
    protected Optional<ConfigAndState<C, S>> createNewConfigAndStateIfMissing(String buttonValue) {
        return Optional.empty();
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        final long messageId = event.getMessageId();
        final long channelId = event.getChannelId();
        final long userId = event.getUserId();
        final long guildId = event.getGuildId();
        final boolean isLegacyMessage = BottomCustomIdUtils.isLegacyCustomId(event.getCustomId());
        final C config;
        final State<S> state;
        final UUID configUUID;
        if (isLegacyMessage) {
            BotMetrics.incrementLegacyButtonMetricCounter(getCommandId());
            log.info("{}: Legacy id {}", event.getRequester().toLogString(), event.getCustomId());
            return event.reply(I18n.getMessage("base.reply.legacyButtonId", event.getRequester().getUserLocal()), false);
        } else {
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
            if (messageConfigDTO.isEmpty() && fallbackConfigAndState.isPresent()) {
                configAndState = fallbackConfigAndState.get();
                configUUID = configAndState.getConfigUUID();
            } else {
                configUUID = messageConfigDTO.get().getConfigUUID();
                final MessageDataDTO messageDataDTO = getMessageDataDTOOrCreateNew(configUUID, guildId, channelId, messageId);
                configAndState = getMessageDataAndUpdateWithButtonValue(messageConfigDTO.get(), messageDataDTO, buttonValue, event.getInvokingGuildMemberName());
            }
            config = configAndState.getConfig();
            state = configAndState.getState();
        }
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
            //if the old button is pined or the result is copied to another channel, the old message will be edited or reset to the slash default
            editMessage = getCurrentMessageContentChange(config, state).orElse(createNewButtonMessage(configUUID, config).getDescriptionOrContent());
            editMessageComponents = Optional.of(getCurrentMessageComponentChange(configUUID, config, state, channelId, userId)
                    .orElse(createNewButtonMessage(configUUID, config).getComponentRowDefinitions()));
        } else {
            //edit the current message if the command changes it or mark it as processing
            editMessage = getCurrentMessageContentChange(config, state).orElse(I18n.getMessage("base.edit.processing", config.getConfigLocale()));
            editMessageComponents = getCurrentMessageComponentChange(configUUID, config, state, channelId, userId);
        }
        //Todo check if message/button are the same. If the message will deleted it should always be "processing...".
        //Todo Remove buttons on set to "processing ..."?
        actions.add(Mono.defer(() -> event.editMessage(editMessage, editMessageComponents.orElse(null))));

        Optional<RollAnswer> answer = getAnswer(config, state, channelId, userId);
        if (answer.isPresent()) {
            BotMetrics.incrementButtonMetricCounter(getCommandId(), config.toShortString());
            BotMetrics.incrementAnswerFormatCounter(config.getAnswerFormatType(), getCommandId());

            actions.add(Mono.defer(() -> event.createResultMessageWithReference(RollAnswerConverter.toEmbedOrMessageDefinition(answer.get()), answerTargetChannelId)
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
        Optional<EmbedOrMessageDefinition> newButtonMessage = createNewButtonMessageWithState(configUUID, config, state, guildId, channelId);

        final boolean deleteCurrentButtonMessage;
        if (newButtonMessage.isPresent() && answerTargetChannelId == null) {
            actions.add(Mono.defer(() -> event.createMessageWithoutReference(newButtonMessage.get()))
                    .doOnNext(newMessageId -> createEmptyMessageData(configUUID, guildId, channelId, newMessageId))
                    .flatMap(newMessageId -> MessageDeletionHelper.deleteOldMessageAndData(persistenceManager, newMessageId, event.getMessageId(), configUUID, channelId, event))
                    .delaySubscription(calculateDelay(event))
                    .doOnSuccess(v -> BotMetrics.timerNewButtonMessageMetricCounter(getCommandId(), stopwatch.elapsed()))
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

    protected void addFurtherActions(List<Mono<Void>> actions, ButtonEventAdaptor event, C config, State<S> state) {

    }

    private Duration calculateDelay(ButtonEventAdaptor event) {
        OffsetDateTime now = OffsetDateTime.now();
        //if for some reason the creation time is after now we set it to 0
        long milliBetween = Math.max(ChronoUnit.MILLIS.between(event.getMessageCreationTime(), now), 0);
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

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocale) {
        Optional<String> checkPermissions = event.checkPermissions(userLocale);
        if (checkPermissions.isPresent()) {
            return event.reply(checkPermissions.get(), false);
        }

        final String commandString = event.getCommandString();
        Optional<CommandInteractionOption> startOption = event.getOption(START_OPTION_NAME);

        if (startOption.isPresent()) {
            CommandInteractionOption options = startOption.get();

            Optional<Long> answerTargetChannelId = BaseCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options);
            if (answerTargetChannelId.isPresent() && answerTargetChannelId.get().equals(event.getChannelId())) {
                log.info("{}:same answer channel for {}", event.getRequester().toLogString(), commandString);
                return event.reply(I18n.getMessage("base.reply.targetChannel.same", userLocale), true);
            }
            if (answerTargetChannelId.isPresent() && !event.isValidAnswerChannel(answerTargetChannelId.get())) {
                log.info("{}: Invalid answer target channel for {}", event.getRequester().toLogString(), commandString);
                return event.reply(I18n.getMessage("base.reply.targetChannel.invalid", userLocale), true);
            }
            final Locale userOrConfigLocale = BaseCommandOptions.getLocaleOptionFromStartCommandOption(options)
                    .orElse(event.getRequester().getUserLocal());
            Optional<String> validationMessage = getStartOptionsValidationMessage(options, event.getChannelId(), event.getUserId(), userOrConfigLocale);
            if (validationMessage.isPresent()) {
                log.info("{}: Validation message: {} for {}", event.getRequester().toLogString(),
                        validationMessage.get().replace("\n"," "),
                        commandString);
                //todo i18n?
                return event.reply(String.format("%s\n%s", commandString, validationMessage.get()), true);
            }
            final C config = getConfigFromStartOptions(options, userOrConfigLocale);
            final UUID configUUID = uuidSupplier.get();
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), config.toShortString());

            final long channelId = event.getChannelId();
            final long guildId = event.getGuildId();
            log.info("{}: '{}'",
                    event.getRequester().toLogString(),
                    commandString.replace("`", ""));
            String replayMessage = Stream.of(commandString, getConfigWarnMessage(config, userLocale).orElse(null))
                    .filter(s -> !Strings.isNullOrEmpty(s))
                    .collect(Collectors.joining(" "));

            return event.reply(replayMessage, false)
                    .then(Mono.defer(() -> {
                        final Optional<MessageConfigDTO> newMessageConfig = createMessageConfig(configUUID, guildId, channelId, config);
                        newMessageConfig.ifPresent(persistenceManager::saveMessageConfig);
                        return event.createMessageWithoutReference(createNewButtonMessage(configUUID, config))
                                .doOnNext(messageId -> createEmptyMessageData(configUUID, guildId, channelId, messageId))
                                .then();
                    }));

        } else if (event.getOption(HELP_OPTION_NAME).isPresent()) {
            BotMetrics.incrementSlashHelpMetricCounter(getCommandId());
            return event.replyWithEmbedOrMessageDefinition(getHelpMessage(event.getRequester().getUserLocal()), true);
        }
        return Mono.empty();
    }

    protected @NonNull Optional<String> getConfigWarnMessage(C config, Locale userLocale) {
        return Optional.empty();
    }

    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of();
    }

    protected abstract @NonNull EmbedOrMessageDefinition getHelpMessage(Locale userLocale);

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
    protected abstract @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessageWithState(@NonNull UUID configId,
                                                                                                   @NonNull C config,
                                                                                                   @NonNull State<S> state,
                                                                                                   long guildId,
                                                                                                   long channelId);

    protected abstract @NonNull Optional<RollAnswer> getAnswer(C config, State<S> state, long channelId, long userId);

    /**
     * The new button message, after a slash event
     */
    public abstract @NonNull EmbedOrMessageDefinition createNewButtonMessage(@NonNull UUID configId, @NonNull C config);

    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options, long channelId, long userId, @NonNull Locale userLocale) {
        //standard is no validation
        return Optional.empty();
    }

    protected boolean shouldKeepExistingButtonMessage(@NonNull ButtonEventAdaptor event) {
        return event.isPinned();
    }

    protected abstract @NonNull C getConfigFromStartOptions(@NonNull CommandInteractionOption options, @NonNull Locale userLocale);
}
