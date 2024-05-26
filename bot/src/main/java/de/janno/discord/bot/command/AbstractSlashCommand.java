package de.janno.discord.bot.command;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.reroll.Config;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Nullable;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.janno.discord.bot.command.BaseCommandOptions.*;

@Slf4j
public abstract class AbstractSlashCommand<C extends Config, S extends StateData> implements SlashCommand {

    private static final String START_OPTION_NAME = "start";
    private static final String HELP_OPTION_NAME = "help";

    protected final PersistenceManager persistenceManager;

    protected AbstractSlashCommand(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }


    @Override
    public final @NonNull CommandDefinition getCommandDefinition() {
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
        //todo maybe optional
        baseOptions.add(ANSWER_INTERACTION_COMMAND_OPTION);

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

    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of();
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
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest autoCompleteRequest, @NonNull Locale userLocale, long channelId, long userId) {
        return BaseCommandOptions.autoCompleteColorOption(autoCompleteRequest, userLocale);
    }

    /**
     * On the creation of a message an empty state need to be saved so we know the message exists and we can remove it later, even on concurrent actions
     */
    protected MessageDataDTO createEmptyMessageData(@NonNull UUID configUUID,
                                                 @Nullable Long guildId,
                                                 long channelId,
                                                 long messageId) {
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), Mapper.NO_PERSISTED_STATE, null);
        //should not be needed but sometimes there is a retry ect and then there is already a state
        persistenceManager.deleteStateForMessage(channelId, messageId);
        persistenceManager.saveMessageData(messageDataDTO);
        return messageDataDTO;
    }


    @Override
    public final @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocale) {
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
                log.info("{}:same answer channel for {}", event.getRequester().toLogString(), commandString.replace("\n", " "));
                return event.reply(I18n.getMessage("base.reply.targetChannel.same", userLocale), true);
            }
            if (answerTargetChannelId.isPresent() && !event.isValidAnswerChannel(answerTargetChannelId.get())) {
                log.info("{}: Invalid answer target channel for {}", event.getRequester().toLogString(), commandString.replace("\n", " "));
                return event.reply(I18n.getMessage("base.reply.targetChannel.invalid", userLocale), true);
            }
            final Locale userOrConfigLocale = BaseCommandOptions.getLocaleOptionFromStartCommandOption(options)
                    .orElse(event.getRequester().getUserLocal());
            Optional<String> validationMessage = getStartOptionsValidationMessage(options, event.getChannelId(), event.getUserId(), userOrConfigLocale);
            if (validationMessage.isPresent()) {
                log.info("{}: Validation message: {} for {}", event.getRequester().toLogString(),
                        validationMessage.get().replace("\n", " "),
                        commandString.replace("\n", " "));
                //todo i18n?
                return event.reply(String.format("%s\n%s", commandString, validationMessage.get()), true);
            }
            final C config = getConfigFromStartOptions(options, userOrConfigLocale);
            final UUID configUUID = uuidSupplier.get();
            BotMetrics.incrementSlashStartMetricCounter(getCommandId());

            final long channelId = event.getChannelId();
            final Long guildId = event.getGuildId();
            if (guildId == null) {
                BotMetrics.outsideGuildCounter("slash");
            }
            log.info("{}: '{}'",
                    event.getRequester().toLogString(),
                    commandString.replace("`", "").replace("\n", " "));
            String replayMessage = Stream.of(commandString, getConfigWarnMessage(config, userLocale).orElse(null))
                    .filter(s -> !Strings.isNullOrEmpty(s))
                    .collect(Collectors.joining(" "));

            return event.reply(replayMessage, false)
                    .then(Mono.defer(() -> {
                        final Optional<MessageConfigDTO> newMessageConfig = createMessageConfig(configUUID, guildId, channelId, config);
                        newMessageConfig.ifPresent(persistenceManager::saveMessageConfig);
                        return event.createMessageWithoutReference(createSlashResponseMessage(configUUID, config, channelId))
                                .doOnNext(messageId -> createEmptyMessageData(configUUID, guildId, channelId, messageId))
                                .then();
                    }));

        } else if (event.getOption(HELP_OPTION_NAME).isPresent()) {
            BotMetrics.incrementSlashHelpMetricCounter(getCommandId());
            return event.replyWithEmbedOrMessageDefinition(getHelpMessage(event.getRequester().getUserLocal()), true);
        }
        return Mono.empty();
    }

    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options, long channelId, long userId, @NonNull Locale userLocale) {
        //standard is no validation
        return Optional.empty();
    }

    protected abstract @NonNull C getConfigFromStartOptions(@NonNull CommandInteractionOption options, @NonNull Locale userLocale);

    protected @NonNull Optional<String> getConfigWarnMessage(C config, Locale userLocale) {
        return Optional.empty();
    }

    //visible for welcome command
    public abstract Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID,
                                                                   @Nullable Long guildId,
                                                                   long channelId,
                                                                   @NonNull C config);

    public abstract @NonNull EmbedOrMessageDefinition createSlashResponseMessage(@NonNull UUID configId, @NonNull C config, long channelId);


    protected abstract @NonNull EmbedOrMessageDefinition getHelpMessage(Locale userLocale);

}
