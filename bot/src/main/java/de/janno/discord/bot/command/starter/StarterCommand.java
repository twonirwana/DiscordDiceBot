package de.janno.discord.bot.command.starter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.help.RpgSystemCommandPreset;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.NamedCommand;
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
        final Optional<MessageConfigDTO> optionalStartedMessageConfigDTO = persistenceManager.getMessageConfig(startedCommandConfigUUID);


        if (optionalStartedMessageConfigDTO.isPresent()) {
            final MessageConfigDTO startedMessageConfigDTO = optionalStartedMessageConfigDTO.get();
            //this is empty if the message is not in start phase
            final Optional<StarterConfig> starterConfig = BottomCustomIdUtils.getConfigUUIDFromCustomId(event.getCustomId())
                    .flatMap(persistenceManager::getMessageConfig)
                    .flatMap(this::deserializeStarterMessage);
            //todo test if remove pined works
            boolean createNewMessage = event.isPinned() || starterConfig.map(StarterConfig::isStartInNewMessage).orElse(false);

            EmbedOrMessageDefinition embedOrMessageDefinition = getMessage(startedMessageConfigDTO, startedCommandConfigUUID, event.getChannelId(), event.getUserId(), createNewMessage);
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

    private EmbedOrMessageDefinition getMessage(MessageConfigDTO messageConfigDTO, UUID configUUID, long channelId, long userId, boolean createNewMessage) {
        if (customDiceCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
            CustomDiceConfig config = CustomDiceCommand.deserializeConfig(messageConfigDTO);
            if (createNewMessage && config.getCallStarterConfigAfterFinish() != null) {
                config = config.toBuilder()
                        .callStarterConfigAfterFinish(null)
                        .build();
                configUUID = uuidSupplier.get();
                persistenceManager.saveMessageConfig(new MessageConfigDTO(configUUID,
                        messageConfigDTO.getGuildId(),
                        messageConfigDTO.getChannelId(),
                        messageConfigDTO.getCommandId(),
                        messageConfigDTO.getConfigClassId(),
                        Mapper.serializedObject(config),
                        config.getName(),
                        userId));
            }
            return customDiceCommand.createSlashResponseMessage(configUUID, config, channelId);
        } else if (customParameterCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
            CustomParameterConfig config = CustomParameterCommand.deserializeConfig(messageConfigDTO);
            if (createNewMessage && config.getCallStarterConfigAfterFinish() != null) {
                config = config.toBuilder()
                        .callStarterConfigAfterFinish(null)
                        .build();
                configUUID = uuidSupplier.get();
                persistenceManager.saveMessageConfig(new MessageConfigDTO(configUUID,
                        messageConfigDTO.getGuildId(),
                        messageConfigDTO.getChannelId(),
                        messageConfigDTO.getCommandId(),
                        messageConfigDTO.getConfigClassId(),
                        Mapper.serializedObject(config),
                        config.getName(),
                        userId));
            }
            return customParameterCommand.createSlashResponseMessage(configUUID, config, channelId);
        } else if (sumCustomSetCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
            SumCustomSetConfig config = SumCustomSetCommand.deserializeConfig(messageConfigDTO);
            if (createNewMessage && config.getCallStarterConfigAfterFinish() != null) {
                config = config.toBuilder()
                        .callStarterConfigAfterFinish(null)
                        .build();
                configUUID = uuidSupplier.get();
                persistenceManager.saveMessageConfig(new MessageConfigDTO(configUUID,
                        messageConfigDTO.getGuildId(),
                        messageConfigDTO.getChannelId(),
                        messageConfigDTO.getCommandId(),
                        messageConfigDTO.getConfigClassId(),
                        Mapper.serializedObject(config),
                        config.getName(),
                        userId));
            }
            return sumCustomSetCommand.createSlashResponseMessage(configUUID, config, channelId);
        }
        throw new IllegalArgumentException("Unknown command id: " + messageConfigDTO.getCommandId());
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
        if (createOptional.isPresent()) {
            final List<NameAndConfigUUID> selectedCommandNamesWithUUID = COMMAND_IDs.stream()
                    .filter(i -> createOptional.get().getStringSubOptionWithName(i).isPresent())
                    .map(i -> createOptional.get().getStringSubOptionWithName(i).orElseThrow())
                    .map(n -> new NameAndConfigUUID(n, getUuidForName(n, userLocale, uuidSupplier, event.getGuildId(), event.getChannelId(), userId, userLocale).orElse(null)))
                    .filter(nu -> nu.configUUID != null)
                    .toList();

            //todo move to own methode?
            final UUID newStarterConfigUUID = uuidSupplier.get();

            final List<StarterConfig.Command> commands = selectedCommandNamesWithUUID.stream()
                    .map(nu -> {
                        CommandIdAndConfigUUID updatedCommandConfigUUID = updateConfig(nu.configUUID(), newStarterConfigUUID, userId);
                        return new StarterConfig.Command(nu.name(), updatedCommandConfigUUID.commandId(), updatedCommandConfigUUID.configUUID());
                    }).toList();

            final String message = createOptional.get().getStringSubOptionWithName(COMMAND_MESSAGE_OPTION).orElse("Chose roll"); //todo i18n
            final String name = BaseCommandOptions.getNameFromStartCommandOption(createOptional.get()).orElse(null);
            final boolean openInNewMessage = createOptional.get().getBooleanSubOptionWithName(COMMAND_OPEN_IN_NEW_MESSAGE_OPTION).orElse(false);

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

    private Optional<UUID> getUuidForName(String name, Locale locale, Supplier<UUID> uuidSupplier, @Nullable Long guildId, long channelId, long userId, Locale userLocale) {
        Optional<UUID> savedNamedCommandUUID = persistenceManager.getNamedCommandsForChannel(userId, guildId).stream()
                .filter(nc -> Objects.equals(nc.name(), name))
                .map(NamedCommand::id)
                .findFirst();
        if (savedNamedCommandUUID.isPresent()) {
            return savedNamedCommandUUID;
        }

        Optional<RpgSystemCommandPreset.PresetId> presetId = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .filter(p -> p.getName(locale).equals(name))
                .findFirst();
        if (presetId.isPresent()) {
            //todo don't save only to create new one later
            UUID baseConfigUUID = uuidSupplier.get();
            savePresetConfig(presetId.get(), baseConfigUUID, guildId, channelId, userId, userLocale);
            return Optional.of(baseConfigUUID);
        }
        return Optional.empty();
    }

    public void savePresetConfig(RpgSystemCommandPreset.PresetId presetId, UUID newConfigUUID, @Nullable Long guildId, long channelId, long userId, Locale userLocale) {
        Config config = RpgSystemCommandPreset.createConfig(presetId, userLocale);
        switch (config) {
            case CustomDiceConfig customDiceConfig ->
                    createPresetConfig(customDiceConfig, customDiceCommand, newConfigUUID, guildId, channelId, userId);
            case SumCustomSetConfig sumCustomSetConfig ->
                    createPresetConfig(sumCustomSetConfig, sumCustomSetCommand, newConfigUUID, guildId, channelId, userId);
            case CustomParameterConfig customParameterConfig ->
                    createPresetConfig(customParameterConfig, customParameterCommand, newConfigUUID, guildId, channelId, userId);
            default -> throw new IllegalStateException("Could not create valid config for: " + presetId);
        }
    }

    private <C extends RollConfig> void createPresetConfig(C config, AbstractCommand<C, ?> command, UUID newConfigUUID, @Nullable Long guildId, long channelId, long userId) {
        command.createMessageConfig(newConfigUUID, guildId, channelId, userId, config).ifPresent(persistenceManager::saveMessageConfig);
    }

    //todo combine with getMessage
    private CommandIdAndConfigUUID updateConfig(UUID configUUIDOfExistingCommand, UUID starterConfigUUID, long userId) {
        UUID configUUIDForCopiedConfig = uuidSupplier.get();
        Optional<MessageConfigDTO> optionalMessageConfigDTO = persistenceManager.getMessageConfig(configUUIDOfExistingCommand);
        if (optionalMessageConfigDTO.isPresent()) {
            final MessageConfigDTO messageConfigDTO = optionalMessageConfigDTO.get();
            if (customDiceCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
                CustomDiceConfig config = CustomDiceCommand.deserializeConfig(messageConfigDTO);
                config = config.toBuilder()
                        .callStarterConfigAfterFinish(starterConfigUUID)
                        .build();
                persistenceManager.saveMessageConfig(new MessageConfigDTO(configUUIDForCopiedConfig,
                        messageConfigDTO.getGuildId(),
                        messageConfigDTO.getChannelId(),
                        messageConfigDTO.getCommandId(),
                        messageConfigDTO.getConfigClassId(),
                        Mapper.serializedObject(config),
                        config.getName(),
                        userId));
            } else if (customParameterCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
                CustomParameterConfig config = CustomParameterCommand.deserializeConfig(messageConfigDTO);
                config = config.toBuilder()
                        .callStarterConfigAfterFinish(starterConfigUUID)
                        .build();
                persistenceManager.saveMessageConfig(new MessageConfigDTO(configUUIDForCopiedConfig,
                        messageConfigDTO.getGuildId(),
                        messageConfigDTO.getChannelId(),
                        messageConfigDTO.getCommandId(),
                        messageConfigDTO.getConfigClassId(),
                        Mapper.serializedObject(config),
                        config.getName(),
                        userId));
            } else if (sumCustomSetCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
                SumCustomSetConfig config = SumCustomSetCommand.deserializeConfig(messageConfigDTO);
                config = config.toBuilder()
                        .callStarterConfigAfterFinish(starterConfigUUID)
                        .build();
                persistenceManager.saveMessageConfig(new MessageConfigDTO(configUUIDForCopiedConfig,
                        messageConfigDTO.getGuildId(),
                        messageConfigDTO.getChannelId(),
                        messageConfigDTO.getCommandId(),
                        messageConfigDTO.getConfigClassId(),
                        Mapper.serializedObject(config),
                        config.getName(),
                        userId));
            } else {
                throw new IllegalStateException("command not supported: " + messageConfigDTO.getCommandId());
            }
            return new CommandIdAndConfigUUID(messageConfigDTO.getCommandId(), configUUIDForCopiedConfig);
        }

        throw new IllegalStateException("UUID  not found: " + configUUIDOfExistingCommand);
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

    private record NameAndConfigUUID(String name, UUID configUUID) {

    }

    private record CommandIdAndConfigUUID(String commandId, UUID configUUID) {
    }
}
