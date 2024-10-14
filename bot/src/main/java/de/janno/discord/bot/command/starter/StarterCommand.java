package de.janno.discord.bot.command.starter;

import com.google.common.annotations.VisibleForTesting;
import de.janno.discord.bot.command.ButtonHelper;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.persistance.ChannelCommandConfigUUID;
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
import org.apache.commons.lang3.stream.IntStreams;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class StarterCommand implements SlashCommand, ComponentCommand {

    public static final String COMMAND_NAME = "starter";
    public static final String COMMAND_CREATE_OPTION = "create";
    public static final String COMMAND_NAME_OPTION = "name";
    public static final String COMMAND_MESSAGE_OPTION = "message";
    public static final String COMMAND_COMMAND_OPTION = "command";
    private static final String CONFIG_TYPE_ID = "StarterConfig";
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<CommandIdAndNameId> COMMAND_IDs = IntStreams.range(5).boxed()
            .map(i -> new CommandIdAndNameId(COMMAND_COMMAND_OPTION + "_" + i, COMMAND_NAME_OPTION + "_" + i))
            .toList();
    private static final Set<String> SUPPORTED_COMMANDS = Set.of(CustomDiceCommand.COMMAND_NAME, CustomParameterCommand.COMMAND_NAME, SumCustomSetCommand.COMMAND_NAME);
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
                .build();
    }

    private static CommandDefinitionOption getButtonCommandOption(String name) {
        return CommandDefinitionOption.builder()
                .type(CommandDefinitionOption.Type.STRING)
                .name(name)
                .description(COMMAND_COMMAND_OPTION)
                //       .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.value.name"))
                //      .description(I18n.getMessage("channel_config.option.value.description", Locale.ENGLISH))
                //      .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.value.description"))
                .required(false)
                .autoComplete(true)
                .build();
    }

    private static String getConfigAutoCompleteName(ChannelCommandConfigUUID channelCommandConfigUUID) {
        return dateFormatter.format(channelCommandConfigUUID.getCreationDate()) + " " + channelCommandConfigUUID.getCommand();
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
        final String buttonValue = BottomCustomIdUtils.getButtonValueFromCustomId(event.getCustomId());
        final UUID commandConfigUUID = UUID.fromString(buttonValue);
        Optional<MessageConfigDTO> messageConfigDTO = persistenceManager.getMessageConfig(commandConfigUUID);
        if (messageConfigDTO.isPresent()) {
            EmbedOrMessageDefinition embedOrMessageDefinition = getMessage(messageConfigDTO.get(), commandConfigUUID, event.getChannelId());
            //todo problematisch dann müsste man die followUp id in der Config wieder entfernen
            if(event.isPinned()){
                return event.acknowledge()
                        .then(event.sendMessage(embedOrMessageDefinition))
                        .then();
            }
            return event.editMessage(embedOrMessageDefinition.getDescriptionOrContent(), embedOrMessageDefinition.getComponentRowDefinitions());
        }
        //todo i18n
        //todo handle pined
        return event.reply("invallid config, recreate command", false);

    }

    private EmbedOrMessageDefinition getMessage(MessageConfigDTO messageConfigDTO, UUID configUUID, long channelId) {
        if (customDiceCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
            CustomDiceConfig config = CustomDiceCommand.deserializeConfig(messageConfigDTO);
            return customDiceCommand.createSlashResponseMessage(configUUID, config, channelId);
        } else if (customParameterCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
            CustomParameterConfig config = CustomParameterCommand.deserializeConfig(messageConfigDTO);
            return customParameterCommand.createSlashResponseMessage(configUUID, config, channelId);
        } else if (sumCustomSetCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
            SumCustomSetConfig config = SumCustomSetCommand.deserializeConfig(messageConfigDTO);
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
                //.nameLocales(I18n.allNoneEnglishMessagesNames("r.name"))
                // .description(I18n.getMessage("r.description", Locale.ENGLISH))
                //  .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("r.description"))
                .option(CommandDefinitionOption.builder()
                        .name(COMMAND_CREATE_OPTION)
                        .description(COMMAND_CREATE_OPTION)
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        //.nameLocales(I18n.allNoneEnglishMessagesNames("r.name"))
                        // .description(I18n.getMessage("r.description", Locale.ENGLISH))
                        //  .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("r.description"))
                        .option(CommandDefinitionOption.builder()
                                .name(COMMAND_MESSAGE_OPTION)
                                .description(COMMAND_MESSAGE_OPTION)
                                .type(CommandDefinitionOption.Type.STRING)
                                .required(false)
                                .build())
                        //todo optiopn always create new message (like pined)
                        .options(
                                COMMAND_IDs.stream()
                                        .flatMap(i -> Stream.of(getButtonNameOption(i.commandName), getButtonCommandOption(i.commandId)))
                                        .toList()
                        )
                        .build())
                //todo help
                .build();
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocale) {
        Optional<CommandInteractionOption> createOptional = event.getOption(COMMAND_CREATE_OPTION);

        if (createOptional.isPresent()) {
            final Map<CommandIdAndNameId, UUID> configUUIDMap = COMMAND_IDs.stream()
                    .filter(i -> createOptional.get().getStringSubOptionWithName(i.commandId()).isPresent())
                    .collect(Collectors.toMap(Function.identity(), i -> {
                        String commandConfigUUIDString = createOptional.get().getStringSubOptionWithName(i.commandId()).orElseThrow();
                        return UUID.fromString(commandConfigUUIDString);
                    }));
            final Map<CommandIdAndNameId, String> nameMap = COMMAND_IDs.stream()
                    .filter(i -> createOptional.get().getStringSubOptionWithName(i.commandName()).isPresent())
                    .collect(Collectors.toMap(Function.identity(), i -> createOptional.get().getStringSubOptionWithName(i.commandName()).orElseThrow()));
            if (configUUIDMap.isEmpty()) {
                //todo i18n
                return event.reply("missing command", true);
            }
            List<String> missingConfigs = configUUIDMap.keySet().stream()
                    .filter(c -> !nameMap.containsKey(c))
                    .map(CommandIdAndNameId::commandId)
                    .sorted()
                    .toList();
            if (!missingConfigs.isEmpty()) {
                //todo i18n
                return event.reply("missing configs for names: " + String.join(", ", missingConfigs), true);
            }

            List<String> missingNames = nameMap.keySet().stream()
                    .filter(c -> !configUUIDMap.containsKey(c))
                    .map(CommandIdAndNameId::commandName)
                    .sorted()
                    .toList();
            if (!missingNames.isEmpty()) {
                //todo i18n
                return event.reply("missing names for configs: " + String.join(", ", missingNames), true);
            }

            final UUID newStarterConfigUUID = uuidSupplier.get();

            final List<StarterConfig.Command> commands = configUUIDMap.entrySet().stream()
                    .map(c -> {
                        String name = nameMap.getOrDefault(c.getKey(), c.getKey().commandId());
                        CommandIdAndConfigUUID updatedCommandConfigUUID = updateConfig(c.getValue(), newStarterConfigUUID);
                        return new StarterConfig.Command(name, updatedCommandConfigUUID.commandId(), updatedCommandConfigUUID.configUUID());
                    }).toList();

            final String message = createOptional.get().getStringSubOptionWithName(COMMAND_MESSAGE_OPTION).orElse("Chose roll"); //todo i18n
            final StarterConfig starterConfig = new StarterConfig(newStarterConfigUUID, commands, userLocale, message);

            //todo metric
            return Mono.defer(() -> {
                persistenceManager.saveMessageConfig(new MessageConfigDTO(starterConfig.getId(), event.getGuildId(), event.getChannelId(), getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(starterConfig)));

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

    private CommandIdAndConfigUUID updateConfig(UUID configUUIDOfExistingCommand, UUID starterConfigUUID) {
        //todo handle not found
        UUID configUUIDForCopiedConfig = uuidSupplier.get();
        MessageConfigDTO messageConfigDTO = persistenceManager.getMessageConfig(configUUIDOfExistingCommand).orElseThrow();
        if (customDiceCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
            CustomDiceConfig config = CustomDiceCommand.deserializeConfig(messageConfigDTO);
            config.setCallStarterConfigAfterFinish(starterConfigUUID);
            persistenceManager.saveMessageConfig(new MessageConfigDTO(configUUIDForCopiedConfig, messageConfigDTO.getGuildId(), messageConfigDTO.getChannelId(), messageConfigDTO.getCommandId(), messageConfigDTO.getConfigClassId(), Mapper.serializedObject(config)));
        } else if (customParameterCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
            CustomParameterConfig config = CustomParameterCommand.deserializeConfig(messageConfigDTO);
            config.setCallStarterConfigAfterFinish(starterConfigUUID);
            persistenceManager.saveMessageConfig(new MessageConfigDTO(configUUIDForCopiedConfig, messageConfigDTO.getGuildId(), messageConfigDTO.getChannelId(), messageConfigDTO.getCommandId(), messageConfigDTO.getConfigClassId(), Mapper.serializedObject(config)));
        } else if (sumCustomSetCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
            SumCustomSetConfig config = SumCustomSetCommand.deserializeConfig(messageConfigDTO);
            config.setCallStarterConfigAfterFinish(starterConfigUUID);
            persistenceManager.saveMessageConfig(new MessageConfigDTO(configUUIDForCopiedConfig, messageConfigDTO.getGuildId(), messageConfigDTO.getChannelId(), messageConfigDTO.getCommandId(), messageConfigDTO.getConfigClassId(), Mapper.serializedObject(config)));
        } else {
            throw new IllegalStateException("command not supported: " + messageConfigDTO.getCommandId());
        }
        return new CommandIdAndConfigUUID(messageConfigDTO.getCommandId(), configUUIDForCopiedConfig);
    }

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest autoCompleteRequest, @NonNull Locale userLocale, long channelId, long userId) {
        if (!COMMAND_IDs.stream().map(CommandIdAndNameId::commandId).collect(Collectors.toSet()).contains(autoCompleteRequest.getFocusedOptionName())) {
            return List.of();
        }
        //todo limit and config filter into db
        //todo filter on not having starter id
        List<ChannelCommandConfigUUID> commandsInChannel = persistenceManager.getChannelCommandConfigs(channelId);

        //todo filter to user input
        //todo filter already selected
        return commandsInChannel.stream()
                .filter(c -> SUPPORTED_COMMANDS.contains(c.getCommand()))
                .limit(10)
                .map(c -> new AutoCompleteAnswer(getConfigAutoCompleteName(c), c.getConfigUUID().toString()))
                .toList();

    }

    private record CommandIdAndNameId(String commandId, String commandName) {
    }

    private record CommandIdAndConfigUUID(String commandId, UUID configUUID) {
    }
}
