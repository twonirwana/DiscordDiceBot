package de.janno.discord.bot.command.help;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import de.janno.discord.bot.BaseCommandUtils;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.channelConfig.AliasConfig;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.namedCommand.NamedCommandHelper;
import de.janno.discord.bot.command.namedCommand.NamedConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class QuickstartCommand implements SlashCommand {

    public static final String ROLL_COMMAND_ID = "quickstart";
    protected static final String SYSTEM_OPTION_NAME = "system";
    private static final Set<String> SUPPORTED_COMMANDS = Set.of(CustomDiceCommand.COMMAND_NAME, CustomParameterCommand.COMMAND_NAME, SumCustomSetCommand.COMMAND_NAME, ChannelConfigCommand.COMMAND_NAME);
    private final static int MAX_AUTOCOMPLETE_OPTIONS = 5;
    private final PersistenceManager persistenceManager;
    private final CustomParameterCommand customParameterCommand;
    private final CustomDiceCommand customDiceCommand;
    private final SumCustomSetCommand sumCustomSetCommand;
    private final ChannelConfigCommand channelConfigCommand;

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest autoCompleteRequest, @NonNull Locale userLocale, long channelId, Long guildId, long userId) {
        if (!SYSTEM_OPTION_NAME.equals(autoCompleteRequest.getFocusedOptionName())) {
            return List.of();
        }

        final List<AutoCompleteAnswer> savedNamedAnswers = persistenceManager.getLastUsedNamedCommandsOfUserAndGuild(userId, guildId).stream()
                .filter(nc -> Strings.isNullOrEmpty(autoCompleteRequest.getFocusedOptionValue()) || nc.name().toLowerCase().contains(autoCompleteRequest.getFocusedOptionValue().toLowerCase()))
                .filter(nc -> SUPPORTED_COMMANDS.contains(nc.commandId()))
                .map(n -> new AutoCompleteAnswer(n.name(), n.name()))
                .distinct()
                .sorted(Comparator.comparing(AutoCompleteAnswer::getName))
                .limit(MAX_AUTOCOMPLETE_OPTIONS)
                .toList();

        final Set<String> alreadyUsedNamesAndSavedNamed = savedNamedAnswers.stream().map(AutoCompleteAnswer::getName).collect(Collectors.toSet());

        final List<AutoCompleteAnswer> presets;
        if (savedNamedAnswers.size() < MAX_AUTOCOMPLETE_OPTIONS) {
            presets = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                    .filter(p -> RpgSystemCommandPreset.matchRpgPreset(autoCompleteRequest.getFocusedOptionValue(), p, userLocale))
                    .map(p -> new AutoCompleteAnswer(p.getName(userLocale), p.getName(userLocale)))
                    .distinct()
                    .filter(a -> !alreadyUsedNamesAndSavedNamed.contains(a.getName()))
                    .sorted(Comparator.comparing(AutoCompleteAnswer::getName))
                    .limit(MAX_AUTOCOMPLETE_OPTIONS - savedNamedAnswers.size())
                    .toList();
        } else {
            presets = List.of();
        }

        return Stream.concat(savedNamedAnswers.stream(), presets.stream())
                .toList();
    }

    @Override
    public @NonNull String getCommandId() {
        return ROLL_COMMAND_ID;
    }


    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .nameLocales(I18n.allNoneEnglishMessagesNames("quickstart.name"))
                .description(I18n.getMessage("quickstart.description", Locale.ENGLISH))
                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("quickstart.description"))
                .option(CommandDefinitionOption.builder()
                        .name(SYSTEM_OPTION_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("quickstart.option.system"))
                        .description(I18n.getMessage("quickstart.option.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("quickstart.option.description"))
                        .required(true)
                        .autoComplete(true)
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build();
    }

    private Optional<EmbedOrMessageDefinition> getMessage(Config genericConfig, UUID configUUID, long channelId, Long guildId) {
        return switch (genericConfig) {
            case CustomDiceConfig config ->
                    Optional.of(customDiceCommand.createSlashResponseMessage(configUUID, config, channelId));
            case CustomParameterConfig config ->
                    Optional.of(customParameterCommand.createSlashResponseMessage(configUUID, config, channelId));
            case SumCustomSetConfig config ->
                    Optional.of(sumCustomSetCommand.createSlashResponseMessage(configUUID, config, channelId));
            case AliasConfig aliasConfig -> {
                channelConfigCommand.saveAliasesConfig(aliasConfig.getAliasList(), channelId, guildId, null, () -> configUUID, aliasConfig.getName());
                yield Optional.empty();
            }
            default -> throw new IllegalStateException("Could not create valid config for: " + genericConfig);

        };
    }


    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocal) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Optional<String> checkPermissions = event.checkPermissions(userLocal);
        if (checkPermissions.isPresent()) {
            return event.reply(checkPermissions.get(), false);
        }

        final UUID newConfigUUID = uuidSupplier.get();
        final Long guildId = event.getGuildId();
        final long channelId = event.getChannelId();
        final long userId = event.getUserId();
        Optional<CommandInteractionOption> commandIdOptional = event.getOption(SYSTEM_OPTION_NAME);
        if (commandIdOptional.isPresent()) {
            final String systemId = commandIdOptional
                    .map(CommandInteractionOption::getStringValue)
                    .orElseThrow();
            final Optional<NamedConfig> namedConfigOptional = NamedCommandHelper.getConfigForName(persistenceManager, systemId, userLocal, guildId, userId);

            if (namedConfigOptional.isPresent()) {
                BotMetrics.incrementSlashStartMetricCounter(getCommandId());
                final NamedConfig namedConfig = namedConfigOptional.get();
                final Config configWithoutStarter = NamedCommandHelper.updateCallStarterConfigAfterFinish(namedConfig.config(), null);
                persistenceManager.saveMessageConfig(new MessageConfigDTO(newConfigUUID,
                        guildId,
                        channelId,
                        namedConfig.commandId(),
                        namedConfig.configClassId(),
                        Mapper.serializedObject(configWithoutStarter),
                        namedConfig.name(),
                        userId));

                final Optional<EmbedOrMessageDefinition> commandAndMessageDefinition = getMessage(configWithoutStarter, newConfigUUID, channelId, guildId);
                final String commandString;
                if (configWithoutStarter instanceof AliasConfig) {
                    commandString = "**%s:** `/channel_config alias multi_save aliases:%s scope:all_users_in_this_channel`".formatted(namedConfig.name(), configWithoutStarter.toCommandOptionsString());
                } else {
                    commandString = "**%s:** `/%s start %s`".formatted(namedConfig.name(), namedConfig.commandId(), configWithoutStarter.toCommandOptionsString());
                }
                if (commandAndMessageDefinition.isPresent()) {
                    return Mono.defer(() -> event.sendMessage(commandAndMessageDefinition.get()))
                            .doOnNext(messageId -> BaseCommandUtils.createCleanupAndSaveEmptyMessageData(newConfigUUID, guildId, channelId, messageId, getCommandId(), persistenceManager))
                            .doOnSuccess(v -> BotMetrics.timerNewButtonMessageMetricCounter(getCommandId(), stopwatch.elapsed()))
                            .then(event.reply(commandString, false))
                            .doOnSuccess(v ->
                                    log.info("{}: '{}' {}ms",
                                            event.getRequester().toLogString(),
                                            namedConfig.name(),
                                            stopwatch.elapsed(TimeUnit.MILLISECONDS)
                                    ));
                } else {
                    return Mono.defer(() -> event.reply(commandString, false))
                            .doOnSuccess(v -> BotMetrics.timerNewButtonMessageMetricCounter(getCommandId(), stopwatch.elapsed()))
                            .doOnSuccess(v ->
                                    log.info("{}: '{}' {}ms",
                                            event.getRequester().toLogString(),
                                            namedConfig.name(),
                                            stopwatch.elapsed(TimeUnit.MILLISECONDS)
                                    ));
                }

            } else {
                log.info("Can't match RPG system id: '{}'", systemId);
                return event.reply(I18n.getMessage("quickstart.unknown.reply", userLocal, systemId), true);
            }

        }
        return Mono.empty();
    }
}