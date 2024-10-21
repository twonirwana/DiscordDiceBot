package de.janno.discord.bot.command.starter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.command.channelConfig.AliasConfig;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.help.RpgSystemCommandPreset;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
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
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Slf4j
public class StarterCommand implements SlashCommand, ComponentCommand {

    public static final String COMMAND_NAME = "starter";
    public static final String COMMAND_CREATE_OPTION = "create";
    public static final String COMMAND_NAME_OPTION = "command_name";
    public static final String COMMAND_MESSAGE_OPTION = "message";
    public static final String COMMAND_OPEN_IN_NEW_MESSAGE_OPTION = "open_in_new_message";
    private static final String CONFIG_TYPE_ID = "StarterConfig";
    private static final List<String> COMMAND_IDs = IntStream.range(1, 11).boxed()
            .map(i -> COMMAND_NAME_OPTION + "_" + i)
            .toList();
    private static final Set<String> SUPPORTED_COMMANDS = Set.of(CustomDiceCommand.COMMAND_NAME, CustomParameterCommand.COMMAND_NAME, SumCustomSetCommand.COMMAND_NAME);
    private final static int MAX_AUTOCOMPLETE_OPTIONS = 5;
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
                .name(name)
                .description(COMMAND_NAME_OPTION)
                //.nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.aliasName.name"))
                //.description(I18n.getMessage("channel_config.option.aliasName.description", Locale.ENGLISH))
                //.descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.aliasName.description"))
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
                //todo emoji
                .map(c -> new ButtonIdLabelAndDiceExpression(c.getConfigUUID().toString(), c.getName(), "", false, false, null))
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
        Optional<UUID> starterUUID = BottomCustomIdUtils.getConfigUUIDFromCustomId(event.getCustomId());
        final Optional<StarterConfig> starterConfig = starterUUID
                .flatMap(persistenceManager::getMessageConfig)
                .flatMap(this::deserializeStarterMessage);
        final boolean createNewMessage = event.isPinned() || starterConfig.map(StarterConfig::isStartInNewMessage).orElse(false);

        //this is empty if the message is not in start phase
        if (optionalConfigToStart.isPresent()) {
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
                return event.acknowledge()
                        .then(event.sendMessage(embedOrMessageDefinition))
                        .then();
            }
            return event.editMessage(embedOrMessageDefinition.getDescriptionOrContent(), embedOrMessageDefinition.getComponentRowDefinitions());
        }
        //todo i18n
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

    private SavedNamedConfig saveConfigToStart(Config genericConfig, UUID starterConfigAfterFinish, Long guildId, long channelId, long userId, String commandId, String configClassId) {
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
                //todo i18n
                //.nameLocales(I18n.allNoneEnglishMessagesNames("r.name"))
                // .description(I18n.getMessage("r.description", Locale.ENGLISH))
                //  .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("r.description"))
                .option(CommandDefinitionOption.builder()
                        .name(COMMAND_CREATE_OPTION)
                        .description(COMMAND_CREATE_OPTION)
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        //todo i18n
                        //.nameLocales(I18n.allNoneEnglishMessagesNames("r.name"))
                        // .description(I18n.getMessage("r.description", Locale.ENGLISH))
                        //  .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("r.description"))
                        .option(CommandDefinitionOption.builder()
                                .name(COMMAND_MESSAGE_OPTION)
                                //todo i18n
                                .description(COMMAND_MESSAGE_OPTION)
                                .type(CommandDefinitionOption.Type.STRING)
                                .required(false)
                                .build())
                        .options(COMMAND_IDs.stream()
                                .map(StarterCommand::getButtonNameOption)
                                .toList())
                        .option(CommandDefinitionOption.builder()
                                .name(COMMAND_OPEN_IN_NEW_MESSAGE_OPTION)
                                //todo i18n
                                .description(COMMAND_OPEN_IN_NEW_MESSAGE_OPTION)
                                .type(CommandDefinitionOption.Type.BOOLEAN)
                                .required(false)
                                .build())
                        .build())
                //todo help
                .build();
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocale) {
        Optional<CommandInteractionOption> createOptional = event.getOption(COMMAND_CREATE_OPTION);
        final long userId = event.getUserId();
        final Long guildId = event.getGuildId();
        final long channelId = event.getChannelId();
        if (createOptional.isPresent()) {
            final List<NamedConfig> selectedCommandNamesWithUUID = COMMAND_IDs.stream()
                    .filter(i -> createOptional.get().getStringSubOptionWithName(i).isPresent())
                    .map(i -> createOptional.get().getStringSubOptionWithName(i).orElseThrow())
                    .flatMap(n -> getConfigForName(n, userLocale, guildId, userId).stream())
                    .toList();

            final UUID newStarterConfigUUID = uuidSupplier.get();

            final String message = createOptional.get().getStringSubOptionWithName(COMMAND_MESSAGE_OPTION).orElse("Chose roll"); //todo i18n
            final String name = BaseCommandOptions.getNameFromStartCommandOption(createOptional.get()).orElse(null);
            final boolean openInNewMessage = createOptional.get().getBooleanSubOptionWithName(COMMAND_OPEN_IN_NEW_MESSAGE_OPTION).orElse(false);
            final List<StarterConfig.Command> commands = selectedCommandNamesWithUUID.stream()
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
            final StarterConfig starterConfig = new StarterConfig(newStarterConfigUUID, commands, userLocale, message, name, openInNewMessage);

            //todo metric
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
                //todo better string
                return event.reply(event.getCommandString(), false);
            }).then(event.sendMessage(createButtonMessage(starterConfig)).then());
        }
        return Mono.empty(); //todo
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
        if (presetId.isPresent()) {
            Config config = RpgSystemCommandPreset.createConfig(presetId.get(), locale);
            String commandId = RpgSystemCommandPreset.getCommandIdForConfig(config);
            //todo better?
            String configClassId = RpgSystemCommandPreset.getConfigClassIdForConfig(config);
            return Optional.of(new NamedConfig(name, commandId, configClassId, config));
        }
        return Optional.empty();
    }

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest autoCompleteRequest, @NonNull Locale userLocale, long channelId, Long guildId, long userId) {
        if (!new HashSet<>(COMMAND_IDs).contains(autoCompleteRequest.getFocusedOptionName())) {
            return List.of();
        }

        final List<AutoCompleteAnswer> savedNamedAnswers = persistenceManager.getNamedCommandsForChannel(userId, guildId).stream()
                .filter(nc -> nc.name().toLowerCase().contains(autoCompleteRequest.getFocusedOptionValue().toLowerCase()))
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

        return ImmutableList.<AutoCompleteAnswer>builder()
                .addAll(savedNamedAnswers)
                .addAll(presets)
                .build();
    }

    private record NamedConfig(@NonNull String name, @NonNull String commandId, @NonNull String configClassId,
                               @NonNull Config config) {
    }

    private record SavedNamedConfig(@NonNull UUID configUUID, @NonNull NamedConfig namedConfig) {

    }

}
