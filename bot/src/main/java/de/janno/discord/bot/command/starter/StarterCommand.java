package de.janno.discord.bot.command.starter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.BotEmojiUtil;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.command.channelConfig.AliasConfig;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.help.RpgSystemCommandPreset;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class StarterCommand implements SlashCommand, ComponentCommand {

    public static final String COMMAND_NAME = "starter";
    static final String COMMAND_CREATE_OPTION = "create";
    static final String COMMAND_NAME_OPTION = "command_name";
    static final String COMMAND_MESSAGE_OPTION = "message";
    static final String COMMAND_OPEN_IN_NEW_MESSAGE_OPTION = "open_in_new_message";
    private static final String COMMAND_WELCOME_OPTION = "welcome";
    private static final String CONFIG_TYPE_ID = "StarterConfig";
    private static final List<String> COMMAND_IDs = IntStream.range(1, 11).boxed()
            .map(i -> COMMAND_NAME_OPTION + "_" + i)
            .toList();
    private static final Set<String> COMMAND_ID_SET = ImmutableSet.copyOf(COMMAND_IDs);
    private static final Set<String> SUPPORTED_COMMANDS = Set.of(CustomDiceCommand.COMMAND_NAME, CustomParameterCommand.COMMAND_NAME, SumCustomSetCommand.COMMAND_NAME);
    private final static int MAX_AUTOCOMPLETE_OPTIONS = 5;
    private static final String HELP_OPTION_NAME = "help";
    private final static List<RpgSystemCommandPreset.PresetId> WELCOME_COMMANDS = List.of(
            RpgSystemCommandPreset.PresetId.DND5_IMAGE,
            RpgSystemCommandPreset.PresetId.SHADOWRUN_IMAGE,
            RpgSystemCommandPreset.PresetId.FATE_IMAGE,
            RpgSystemCommandPreset.PresetId.COIN,
            RpgSystemCommandPreset.PresetId.OWOD,
            RpgSystemCommandPreset.PresetId.NWOD,
            RpgSystemCommandPreset.PresetId.DICE_CALCULATOR,
            RpgSystemCommandPreset.PresetId.DND5);
    private final CustomParameterCommand customParameterCommand;
    private final CustomDiceCommand customDiceCommand;
    private final SumCustomSetCommand sumCustomSetCommand;
    private final Supplier<UUID> uuidSupplier;
    private final PersistenceManager persistenceManager;

    public StarterCommand(PersistenceManager persistenceManager,
                          CustomParameterCommand customParameterCommand,
                          CustomDiceCommand customDiceCommand,
                          SumCustomSetCommand sumCustomSetCommand) {
        this(persistenceManager, UUID::randomUUID, customParameterCommand, customDiceCommand, sumCustomSetCommand);
    }

    @VisibleForTesting
    public StarterCommand(PersistenceManager persistenceManager,
                          Supplier<UUID> uuidSupplier,
                          CustomParameterCommand customParameterCommand,
                          CustomDiceCommand customDiceCommand,
                          SumCustomSetCommand sumCustomSetCommand) {
        this.persistenceManager = persistenceManager;
        this.uuidSupplier = uuidSupplier;
        this.customParameterCommand = customParameterCommand;
        this.sumCustomSetCommand = sumCustomSetCommand;
        this.customDiceCommand = customDiceCommand;
    }

    private static CommandDefinitionOption getButtonNameOption(String name) {
        return CommandDefinitionOption.builder()
                .type(CommandDefinitionOption.Type.STRING)
                //no i18n for the names, they are numbered
                .name(name)
                .description(I18n.getMessage("starter.option.command.name.description", Locale.ENGLISH))
                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("starter.option.command.name.description"))
                .required(false)
                .autoComplete(true)
                .build();
    }

    public static Optional<EmbedOrMessageDefinition> getStarterMessage(PersistenceManager persistenceManager, UUID configUUID) {
        Optional<MessageConfigDTO> messageConfigDTO = persistenceManager.getMessageConfig(configUUID);
        if (messageConfigDTO.isPresent()) {
            StarterConfig config = Mapper.deserializeObject(messageConfigDTO.get().getConfig(), StarterConfig.class);
            return Optional.of(createButtonMessage(config));
        }
        return Optional.empty();
    }

    private static EmbedOrMessageDefinition createButtonMessage(StarterConfig config) {

        List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions = config.getCommands().stream()
                .map(c -> {
                    BotEmojiUtil.LabelAndEmoji labelAndEmoji = BotEmojiUtil.splitLabel(c.getName());
                    return new ButtonIdLabelAndDiceExpression(c.getConfigUUID().toString(), labelAndEmoji.labelWithoutLeadingEmoji(), "", false, false, labelAndEmoji.emoji());
                })
                .toList();

        return EmbedOrMessageDefinition.builder()
                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                .descriptionOrContent(config.getMessage())
                .componentRowDefinitions(ButtonHelper.createButtonLayout(COMMAND_NAME, config.getId(), buttonIdLabelAndDiceExpressions))
                .build();
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event) {
        final UUID startedCommandConfigUUID = UUID.fromString(BottomCustomIdUtils.getButtonValueFromCustomId(event.getCustomId()));
        final Optional<SavedNamedConfig> optionalConfigToStart = getConfigForNamedCommand(startedCommandConfigUUID);
        final long userId = event.getUserId();
        final Long guildId = event.getGuildId();
        final long channelId = event.getChannelId();
        final Optional<UUID> starterUUID = BottomCustomIdUtils.getConfigUUIDFromCustomId(event.getCustomId());
        final Optional<StarterConfig> starterConfig = starterUUID
                .flatMap(persistenceManager::getMessageConfig)
                .flatMap(this::deserializeStarterMessage);
        final boolean createNewMessage = event.isPinned() || starterConfig.map(StarterConfig::isStartInNewMessage).orElse(false);

        //this is empty if the message is not in start phase
        if (optionalConfigToStart.isPresent()) {
            BotMetrics.incrementButtonMetricCounter(getCommandId());
            final NamedConfig configToStart = optionalConfigToStart.get().namedConfig();
            final UUID config2StartUUID;

            final Config updatedConfig2Start;
            if (starterUUID.isPresent()) {
                final RollConfig rollConfig = (RollConfig) configToStart.config();
                final String commandId = configToStart.commandId();
                final String configClassId = configToStart.configClassId();
                if (createNewMessage && rollConfig.getCallStarterConfigAfterFinish() != null) {
                    SavedNamedConfig savedUpdatedConfig = saveConfigToStart(rollConfig, null, guildId, channelId, userId, commandId, configClassId);
                    updatedConfig2Start = savedUpdatedConfig.namedConfig().config();
                    config2StartUUID = savedUpdatedConfig.configUUID();
                } else if (!createNewMessage && rollConfig.getCallStarterConfigAfterFinish() == null) {
                    SavedNamedConfig savedUpdatedConfig = saveConfigToStart(rollConfig, null, guildId, channelId, userId, commandId, configClassId);
                    updatedConfig2Start = savedUpdatedConfig.namedConfig().config();
                    config2StartUUID = savedUpdatedConfig.configUUID();
                } else {
                    updatedConfig2Start = configToStart.config();
                    config2StartUUID = optionalConfigToStart.get().configUUID();
                }
            } else {
                updatedConfig2Start = configToStart.config();
                config2StartUUID = optionalConfigToStart.get().configUUID();
            }

            EmbedOrMessageDefinition embedOrMessageDefinition = getMessage(updatedConfig2Start, config2StartUUID, channelId);
            if (createNewMessage) {
                return event
                        .reply("`/%s start %s`".formatted(configToStart.commandId(), updatedConfig2Start.toCommandOptionsString()), false)
                        .then(event.sendMessage(embedOrMessageDefinition))
                        .then();
            }
            return event.editMessage(embedOrMessageDefinition.getDescriptionOrContent(), embedOrMessageDefinition.getComponentRowDefinitions());
        }
        log.error("Invalid state in: {} ", event.getCustomId());
        return event.reply("invalid config, recreate command", false);

    }

    private Optional<StarterConfig> deserializeStarterMessage(MessageConfigDTO messageConfigDTO) {
        if (CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId())) {
            return Optional.of(Mapper.deserializeObject(messageConfigDTO.getConfig(), StarterConfig.class));
        }
        return Optional.empty();
    }

    private Optional<SavedNamedConfig> getConfigForNamedCommand(UUID configUUID) {
        Optional<MessageConfigDTO> optionalMessageConfigDTO = persistenceManager.getMessageConfig(configUUID);
        if (optionalMessageConfigDTO.isPresent() && optionalMessageConfigDTO.get().getName() != null) {
            MessageConfigDTO messageConfigDTO = optionalMessageConfigDTO.get();
            if (customDiceCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
                return Optional.of(new SavedNamedConfig(configUUID, new NamedConfig(messageConfigDTO.getName(), messageConfigDTO.getCommandId(), messageConfigDTO.getConfigClassId(), CustomDiceCommand.deserializeConfig(messageConfigDTO))));
            } else if (customParameterCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
                return Optional.of(new SavedNamedConfig(configUUID, new NamedConfig(messageConfigDTO.getName(), messageConfigDTO.getCommandId(), messageConfigDTO.getConfigClassId(), CustomParameterCommand.deserializeConfig(messageConfigDTO))));
            } else if (sumCustomSetCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
                return Optional.of(new SavedNamedConfig(configUUID, new NamedConfig(messageConfigDTO.getName(), messageConfigDTO.getCommandId(), messageConfigDTO.getConfigClassId(), SumCustomSetCommand.deserializeConfig(messageConfigDTO))));
            }
        }
        return Optional.empty();

    }

    private EmbedOrMessageDefinition getMessage(Config genericConfig, UUID configUUID, long channelId) {
        if (genericConfig instanceof CustomDiceConfig config) {
            return customDiceCommand.createSlashResponseMessage(configUUID, config, channelId);
        } else if (genericConfig instanceof CustomParameterConfig config) {
            return customParameterCommand.createSlashResponseMessage(configUUID, config, channelId);
        } else if (genericConfig instanceof SumCustomSetConfig config) {
            return sumCustomSetCommand.createSlashResponseMessage(configUUID, config, channelId);
        }
        throw new IllegalArgumentException("Unknown config: " + genericConfig);
    }

    private Config updateCallStarterConfigAfterFinish(Config genericConfig, UUID starterConfigAfterFinish) {
        return switch (genericConfig) {
            case CustomDiceConfig config -> config.toBuilder()
                    .callStarterConfigAfterFinish(starterConfigAfterFinish)
                    .build();
            case CustomParameterConfig config -> config.toBuilder()
                    .callStarterConfigAfterFinish(starterConfigAfterFinish)
                    .build();
            case SumCustomSetConfig config -> config.toBuilder()
                    .callStarterConfigAfterFinish(starterConfigAfterFinish)
                    .build();
            case null, default ->
                    throw new IllegalStateException("command not supported:  %s".formatted(genericConfig));
        };
    }

    private SavedNamedConfig saveConfigToStart(Config genericConfig, UUID starterConfigAfterFinish, Long guildId, long channelId, Long userId, String commandId, String configClassId) {
        final Config updatedConfig = updateCallStarterConfigAfterFinish(genericConfig, starterConfigAfterFinish);
        final UUID newConfigUUID = uuidSupplier.get();
        persistenceManager.saveMessageConfig(new MessageConfigDTO(newConfigUUID,
                guildId,
                channelId,
                commandId,
                configClassId,
                Mapper.serializedObject(updatedConfig),
                updatedConfig.getName(),
                userId));
        return new SavedNamedConfig(newConfigUUID, new NamedConfig(genericConfig.getName(), commandId, configClassId, updatedConfig));
    }

    @Override
    public @NonNull String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .description(getCommandId())
                .nameLocales(I18n.allNoneEnglishMessagesNames("starter.name"))
                .description(I18n.getMessage("starter.description", Locale.ENGLISH))
                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("starter.description"))
                .option(CommandDefinitionOption.builder()
                        .name(COMMAND_CREATE_OPTION)
                        .description(COMMAND_CREATE_OPTION)
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("starter.option.create.name"))
                        .description(I18n.getMessage("starter.option.create.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("starter.option.create.description"))
                        .option(CommandDefinitionOption.builder()
                                .name(COMMAND_MESSAGE_OPTION)
                                .nameLocales(I18n.allNoneEnglishMessagesNames("starter.option.message.name"))
                                .description(I18n.getMessage("starter.option.message.description", Locale.ENGLISH))
                                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("starter.option.message.description"))
                                .type(CommandDefinitionOption.Type.STRING)
                                .required(false)
                                .build())
                        .options(COMMAND_IDs.stream()
                                .map(StarterCommand::getButtonNameOption)
                                .toList())
                        .option(CommandDefinitionOption.builder()
                                .name(COMMAND_OPEN_IN_NEW_MESSAGE_OPTION)
                                .nameLocales(I18n.allNoneEnglishMessagesNames("starter.option.new.message.name"))
                                .description(I18n.getMessage("starter.option.new.message.description", Locale.ENGLISH))
                                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("starter.option.new.message.description"))
                                .type(CommandDefinitionOption.Type.BOOLEAN)
                                .required(false)
                                .build())
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(COMMAND_WELCOME_OPTION)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("starter.option.welcome.name"))
                        .description(I18n.getMessage("starter.option.welcome.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("starter.option.welcome.description"))
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .build()
                )
                .option(CommandDefinitionOption.builder()
                        .name(HELP_OPTION_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("base.option.help"))
                        .description(I18n.getMessage("base.help.description", Locale.ENGLISH, (I18n.getMessage("%s.name".formatted(getCommandId()), Locale.ENGLISH))))
                        .descriptionLocales(I18n.allNoneEnglishDescriptionsWithKeys("base.help.description", "%s.name".formatted(getCommandId())))
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .build())
                .build();
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocale) {

        Optional<CommandInteractionOption> helpOptional = event.getOption(HELP_OPTION_NAME);
        if (helpOptional.isPresent()) {
            return event.sendMessage(getHelpMessage(userLocale)).then();
        }

        final long userId = event.getUserId();
        final Long guildId = event.getGuildId();
        final long channelId = event.getChannelId();
        Optional<CommandInteractionOption> createOptional = event.getOption(COMMAND_CREATE_OPTION);
        final StarterConfig starterConfig;
        if (createOptional.isPresent()) {
            starterConfig = createStartConfigFromCreateOption(createOptional.get(), guildId, channelId, userId, userLocale);
        } else if (event.getOption(COMMAND_WELCOME_OPTION).isPresent()) {
            starterConfig = createWelcomeStarterConfig(guildId, channelId, userId, userLocale);
        } else if (event.getOption(HELP_OPTION_NAME).isPresent()) {
            BotMetrics.incrementSlashHelpMetricCounter(getCommandId());
            return event.replyWithEmbedOrMessageDefinition(getHelpMessage(event.getRequester().getUserLocal()), true);
        } else {
            starterConfig = null;
        }

        if (starterConfig != null) {
            BotMetrics.incrementSlashStartMetricCounter(getCommandId());
            return Mono.defer(() -> {
                persistenceManager.saveMessageConfig(new MessageConfigDTO(starterConfig.getId(),
                        event.getGuildId(),
                        event.getChannelId(),
                        getCommandId(),
                        CONFIG_TYPE_ID,
                        Mapper.serializedObject(starterConfig),
                        starterConfig.getName(),
                        userId));

                log.info("{}: '{}' -> {}",
                        event.getRequester().toLogString(),
                        event.getCommandString().replace("`", ""),
                        starterConfig.toShortString()
                );
                return event.reply(event.getCommandString(), false);
            }).then(event.sendMessage(createButtonMessage(starterConfig)).then());
        }
        log.error("Unknown command: {} from {}", event.getOptions(), event.getRequester().toLogString());
        return event.reply("There was an error, try again", true);
    }

    public Function<DiscordConnector.WelcomeRequest, EmbedOrMessageDefinition> getWelcomeMessage() {
        return request -> {
            StarterConfig starterConfig = createWelcomeStarterConfig(request.guildId(), request.channelId(), null, request.guildLocale());
            persistenceManager.saveMessageConfig(new MessageConfigDTO(starterConfig.getId(),
                    request.guildId(),
                    request.channelId(),
                    getCommandId(),
                    CONFIG_TYPE_ID,
                    Mapper.serializedObject(starterConfig),
                    starterConfig.getName(),
                    null));
            return createButtonMessage(starterConfig);
        };
    }

    private StarterConfig createWelcomeStarterConfig(Long guildId, long channelId, Long userId, Locale userLocale) {
        final List<NamedConfig> selectedCommandNames = WELCOME_COMMANDS.stream()
                .map(p -> createNameConfigFromPresetId(p.getName(userLocale), p, userLocale))
                .toList();

        final UUID newStarterConfigUUID = uuidSupplier.get();

        final String message = I18n.getMessage("welcome.message", userLocale);
        final List<StarterConfig.Command> commands = selectedCommandNames.stream()
                .map(nu -> {
                    final UUID savedConfig = saveConfigToStart(nu.config(), null, guildId, channelId, userId, nu.commandId(), nu.configClassId()).configUUID();
                    return new StarterConfig.Command(nu.name(), savedConfig);
                }).toList();
        return new StarterConfig(newStarterConfigUUID, commands, userLocale, message, null, true);
    }

    private StarterConfig createStartConfigFromCreateOption(CommandInteractionOption createOption, Long guildId, long channelId, long userId, Locale userLocale) {
        final List<NamedConfig> selectedCommandNames = COMMAND_IDs.stream()
                .filter(i -> createOption.getStringSubOptionWithName(i).isPresent())
                .map(i -> createOption.getStringSubOptionWithName(i).orElseThrow())
                .flatMap(n -> getConfigForName(n, userLocale, guildId, userId).stream())
                .toList();

        final UUID newStarterConfigUUID = uuidSupplier.get();

        final String message = createOption.getStringSubOptionWithName(COMMAND_MESSAGE_OPTION).orElse(I18n.getMessage("starter.message.default", userLocale));
        final String name = BaseCommandOptions.getNameFromStartCommandOption(createOption).orElse(null);
        final boolean openInNewMessage = createOption.getBooleanSubOptionWithName(COMMAND_OPEN_IN_NEW_MESSAGE_OPTION).orElse(false);
        final List<StarterConfig.Command> commands = selectedCommandNames.stream()
                .map(nu -> {
                    final UUID starterConfigAfterFinish;
                    if (openInNewMessage) {
                        starterConfigAfterFinish = null;
                    } else {
                        starterConfigAfterFinish = newStarterConfigUUID;
                    }
                    final UUID savedConfig = saveConfigToStart(nu.config(), starterConfigAfterFinish, guildId, channelId, userId, nu.commandId(), nu.configClassId()).configUUID();
                    return new StarterConfig.Command(nu.name(), savedConfig);
                }).toList();
        return new StarterConfig(newStarterConfigUUID, commands, userLocale, message, name, openInNewMessage);
    }


    private Optional<NamedConfig> getConfigForName(String name, Locale locale, @Nullable Long guildId, long userId) {
        Optional<NamedConfig> savedNamedCommandConfig = persistenceManager.getNamedCommandsForChannel(userId, guildId).stream()
                .filter(nc -> Objects.equals(nc.name(), name))
                .flatMap(nc -> getConfigForNamedCommand(nc.id()).map(snc -> new NamedConfig(snc.namedConfig().name(), snc.namedConfig().commandId(), snc.namedConfig().configClassId(), snc.namedConfig().config())).stream())
                .findFirst();
        if (savedNamedCommandConfig.isPresent()) {
            return savedNamedCommandConfig;
        }

        Optional<RpgSystemCommandPreset.PresetId> presetId = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .filter(p -> p.getName(locale).equals(name))
                .findFirst();
        return presetId.map(id -> createNameConfigFromPresetId(name, id, locale));
    }

    private NamedConfig createNameConfigFromPresetId(String name, RpgSystemCommandPreset.PresetId presetId, Locale locale) {
        Config config = RpgSystemCommandPreset.createConfig(presetId, locale);
        String commandId = presetId.getCommandId();
        String configClassId = presetId.getConfigClassType();
        return new NamedConfig(name, commandId, configClassId, config);
    }

    protected @NonNull EmbedOrMessageDefinition getHelpMessage(Locale userLocale) {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent(I18n.getMessage("starter.help.message", userLocale) + "\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.example.field.name", userLocale), I18n.getMessage("starter.help.example.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.documentation.field.name", userLocale), I18n.getMessage("help.documentation.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.discord.server.field.name", userLocale), I18n.getMessage("help.discord.server.field.value", userLocale), false))
                .build();
    }


    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest autoCompleteRequest, @NonNull Locale userLocale, long channelId, Long guildId, long userId) {
        if (!new HashSet<>(COMMAND_IDs).contains(autoCompleteRequest.getFocusedOptionName())) {
            return List.of();
        }

        final List<AutoCompleteAnswer> savedNamedAnswers = persistenceManager.getNamedCommandsForChannel(userId, guildId).stream()
                .filter(nc -> Strings.isNullOrEmpty(autoCompleteRequest.getFocusedOptionValue()) || nc.name().toLowerCase().contains(autoCompleteRequest.getFocusedOptionValue().toLowerCase()))
                .filter(nc -> SUPPORTED_COMMANDS.contains(nc.commandId()))
                .map(n -> new AutoCompleteAnswer(n.name(), n.name()))
                .sorted(Comparator.comparing(AutoCompleteAnswer::getName))
                .limit(MAX_AUTOCOMPLETE_OPTIONS)
                .toList();

        final List<AutoCompleteAnswer> presets;
        if (savedNamedAnswers.size() < MAX_AUTOCOMPLETE_OPTIONS) {
            presets = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                    .filter(p -> RpgSystemCommandPreset.matchRpgPreset(autoCompleteRequest.getFocusedOptionValue(), p, userLocale))
                    .filter(p -> !(RpgSystemCommandPreset.createConfig(p, userLocale) instanceof AliasConfig))
                    .map(p -> new AutoCompleteAnswer(p.getName(userLocale), p.getName(userLocale)))
                    .sorted(Comparator.comparing(AutoCompleteAnswer::getName))
                    .filter(a -> !savedNamedAnswers.contains(a))
                    .limit(MAX_AUTOCOMPLETE_OPTIONS - savedNamedAnswers.size())
                    .toList();
        } else {
            presets = List.of();
        }

        final Set<String> alreadyUsedNames = autoCompleteRequest.getOptionValues().stream()
                .filter(ov -> COMMAND_ID_SET.contains(ov.getOptionName()))
                .map(OptionValue::getOptionValue)
                .filter(n -> !Strings.isNullOrEmpty(n))
                .collect(Collectors.toSet());

        return Stream.concat(savedNamedAnswers.stream(), presets.stream())
                .filter(a -> !alreadyUsedNames.contains(a.getValue()))
                .toList();
    }

    private record NamedConfig(@NonNull String name, @NonNull String commandId, @NonNull String configClassId,
                               @NonNull Config config) {
    }

    private record SavedNamedConfig(@NonNull UUID configUUID, @NonNull NamedConfig namedConfig) {

    }

}
