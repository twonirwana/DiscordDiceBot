package de.janno.discord.bot.command.channelConfig;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.BaseCommandOptions;
import de.janno.discord.bot.command.directRoll.DirectRollCommand;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static de.janno.discord.bot.command.channelConfig.AliasHelper.*;

@Slf4j
public class ChannelConfigCommand implements SlashCommand {
    public static final String DIRECT_ROLL_CONFIG_TYPE_ID = "DirectRollConfig";
    static final String ALWAYS_SUM_RESULTS_OPTION_NAME = "always_sum_result";
    private static final String COMMAND_ID = "channel_config";
    private static final String SAVE_DIRECT_ROLL_CONFIG_OPTION_NAME = "save_direct_roll_config";
    private static final String DELETE_DIRECT_ROLL_CONFIG_OPTION_NAME = "delete_direct_roll_config";
    private static final String ALIAS_OPTION_NAME = "alias";
    private static final String SCOPE_OPTION_NAME = "scope";
    private static final String SCOPE_OPTION_CHOICE_USER_CHANNEL_NAME = "current_user_in_this_channel";
    private static final String SCOPE_OPTION_CHOICE_CHANNEL_NAME = "all_users_in_this_channel";
    private static final String SAVE_ALIAS_OPTION_NAME = "save";
    private static final String SAVE_MULTI_ALIAS_OPTION_NAME = "multi_save";
    private static final String ALIAS_NAME_OPTION_NAME = "name";
    private static final String ALIASES_OPTION_NAME = "aliases";
    private static final String ALIAS_VALUE_OPTION_NAME = "value";
    private static final String LIST_ALIAS_OPTION_NAME = "list";
    private static final String DELETE_ALIAS_OPTION_NAME = "delete";

    private static final CommandDefinitionOption SCOPE_OPTION = CommandDefinitionOption.builder()
            .type(CommandDefinitionOption.Type.STRING)
            .name(SCOPE_OPTION_NAME)
            .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.scope.name"))
            .description(I18n.getMessage("channel_config.option.scope.description", Locale.ENGLISH))
            .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.scope.description"))
            .required(true)
            .choice(CommandDefinitionOptionChoice.builder()
                    //no locale to have copy and pasteable commands
                    .name(SCOPE_OPTION_CHOICE_CHANNEL_NAME)
                    .value(SCOPE_OPTION_CHOICE_CHANNEL_NAME)
                    .build())
            .choice(CommandDefinitionOptionChoice.builder()
                    //no locale to have copy and pasteable commands
                    .name(SCOPE_OPTION_CHOICE_USER_CHANNEL_NAME)
                    .value(SCOPE_OPTION_CHOICE_USER_CHANNEL_NAME)
                    .build())
            .build();

    private static final CommandDefinitionOption SAVE_ALIAS_OPTION = CommandDefinitionOption.builder()
            .type(CommandDefinitionOption.Type.SUB_COMMAND)
            .name(SAVE_ALIAS_OPTION_NAME)
            .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.save.name"))
            .description(I18n.getMessage("channel_config.option.save.description", Locale.ENGLISH))
            .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.save.description"))
            .option(CommandDefinitionOption.builder()
                    .type(CommandDefinitionOption.Type.STRING)
                    .name(ALIAS_NAME_OPTION_NAME)
                    .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.aliasName.name"))
                    .description(I18n.getMessage("channel_config.option.aliasName.description", Locale.ENGLISH))
                    .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.aliasName.description"))
                    .required(true)
                    .build())
            .option(CommandDefinitionOption.builder()
                    .type(CommandDefinitionOption.Type.STRING)
                    .name(ALIAS_VALUE_OPTION_NAME)
                    .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.value.name"))
                    .description(I18n.getMessage("channel_config.option.value.description", Locale.ENGLISH))
                    .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.value.description"))
                    .required(true)
                    .build())
            .option(SCOPE_OPTION)
            .build();
    private static final CommandDefinitionOption MULTI_SAVE_ALIAS_OPTION = CommandDefinitionOption.builder()
            .type(CommandDefinitionOption.Type.SUB_COMMAND)
            .name(SAVE_MULTI_ALIAS_OPTION_NAME)
            .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.multiSave.name"))
            .description(I18n.getMessage("channel_config.option.multiSave.description", Locale.ENGLISH))
            .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.multiSave.description"))
            .option(CommandDefinitionOption.builder()
                    .type(CommandDefinitionOption.Type.STRING)
                    .name(ALIASES_OPTION_NAME)
                    .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.aliases.name"))
                    .description(I18n.getMessage("channel_config.option.aliases.description", Locale.ENGLISH))
                    .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.aliases.description"))
                    .required(true)
                    .build())
            .option(SCOPE_OPTION)
            .build();
    private static final CommandDefinitionOption DELETE_ALIAS_OPTION = CommandDefinitionOption.builder()
            .type(CommandDefinitionOption.Type.SUB_COMMAND)
            .name(DELETE_ALIAS_OPTION_NAME)
            .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.delete.name"))
            .description(I18n.getMessage("channel_config.option.delete.description", Locale.ENGLISH))
            .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.delete.description"))
            .option(SCOPE_OPTION)
            .option(CommandDefinitionOption.builder()
                    .type(CommandDefinitionOption.Type.STRING)
                    .name(ALIAS_NAME_OPTION_NAME)
                    .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.aliasName.name"))
                    .description(I18n.getMessage("channel_config.option.delete.aliasName.description", Locale.ENGLISH))
                    .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.delete.aliasName.description"))
                    .required(true)
                    .autoComplete(true)
                    .build())
            .build();
    private static final CommandDefinitionOption LIST_ALIAS_OPTION = CommandDefinitionOption.builder()
            .type(CommandDefinitionOption.Type.SUB_COMMAND)
            .name(LIST_ALIAS_OPTION_NAME)
            .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.list.name"))
            .description(I18n.getMessage("channel_config.option.list.description", Locale.ENGLISH))
            .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.list.description"))
            .option(SCOPE_OPTION)
            .build();
    private final static String NAME_VALUE_DELIMITER = ":";
    private final PersistenceManager persistenceManager;

    public ChannelConfigCommand(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public @NonNull String getCommandId() {
        return COMMAND_ID;
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.name"))
                .description(I18n.getMessage("channel_config.description", Locale.ENGLISH))
                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.description"))
                .option(CommandDefinitionOption.builder()
                        .name(SAVE_DIRECT_ROLL_CONFIG_OPTION_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.save_direct_roll_config.name"))
                        .description(I18n.getMessage("channel_config.option.save_direct_roll_config.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.save_direct_roll_config.description"))
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .option(BaseCommandOptions.ANSWER_FORMAT_COMMAND_OPTION)
                        .option(BaseCommandOptions.DICE_IMAGE_STYLE_COMMAND_OPTION)
                        .option(BaseCommandOptions.DICE_IMAGE_COLOR_COMMAND_OPTION)
                        .option(CommandDefinitionOption.builder()
                                .name(ALWAYS_SUM_RESULTS_OPTION_NAME)
                                .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.always_sum_result.name"))
                                .description(I18n.getMessage("channel_config.option.always_sum_result.description", Locale.ENGLISH))
                                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.always_sum_result.description"))
                                .type(CommandDefinitionOption.Type.BOOLEAN)
                                .required(false)
                                .build())
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(DELETE_DIRECT_ROLL_CONFIG_OPTION_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.delete_direct_roll_config.name"))
                        .description(I18n.getMessage("channel_config.option.delete_direct_roll_config.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.delete_direct_roll_config.description"))
                        .type(CommandDefinitionOption.Type.SUB_COMMAND_GROUP)
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(ALIAS_OPTION_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("channel_config.option.alias.name"))
                        .description(I18n.getMessage("channel_config.option.alias.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("channel_config.option.alias.description"))
                        .type(CommandDefinitionOption.Type.SUB_COMMAND_GROUP)
                        .option(SAVE_ALIAS_OPTION)
                        .option(DELETE_ALIAS_OPTION)
                        .option(LIST_ALIAS_OPTION)
                        .option(MULTI_SAVE_ALIAS_OPTION)
                        .build())
                .build();
    }

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest autoCompleteRequest, @NonNull Locale userLocale, long channelId, long userId) {
        if (BaseCommandOptions.DICE_IMAGE_COLOR_OPTION_NAME.equals(autoCompleteRequest.getFocusedOptionName())) {
            return BaseCommandOptions.autoCompleteColorOption(autoCompleteRequest, userLocale);
        }
        if (ALIAS_NAME_OPTION_NAME.equals(autoCompleteRequest.getFocusedOptionName())) {
            Optional<String> scopeOptionValue = autoCompleteRequest.getOptionValues().stream()
                    .filter(o -> SCOPE_OPTION_NAME.equals(o.getOptionName()))
                    .map(OptionValue::getOptionValue)
                    .findFirst();

            Set<String> validScopeValues = Set.of(SCOPE_OPTION_CHOICE_USER_CHANNEL_NAME, SCOPE_OPTION_CHOICE_CHANNEL_NAME);
            if (scopeOptionValue.isEmpty() || !validScopeValues.contains(scopeOptionValue.get())) {
                return List.of(new AutoCompleteAnswer(I18n.getMessage("alias.delete.autoComplete.missingScope.name", userLocale), ""));
            }
            Long userIdForAlias = SCOPE_OPTION_CHOICE_USER_CHANNEL_NAME.equals(scopeOptionValue.get()) ? userId : null;
            List<Alias> existingAliases = loadAlias(channelId, userIdForAlias);
            return existingAliases.stream()
                    .filter(a -> a.getName().contains(autoCompleteRequest.getFocusedOptionValue()))
                    .limit(25)
                    .map(a -> new AutoCompleteAnswer(a.getName() + "->" + a.getValue(), a.getName()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private String serializeConfig(DirectRollConfig channelConfig) {
        return Mapper.serializedObject(channelConfig);
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocal) {
        if (event.getOption(SAVE_DIRECT_ROLL_CONFIG_OPTION_NAME).isPresent()) {
            CommandInteractionOption saveAction = event.getOption(SAVE_DIRECT_ROLL_CONFIG_OPTION_NAME).get();
            boolean alwaysSumResults = saveAction.getBooleanSubOptionWithName(ALWAYS_SUM_RESULTS_OPTION_NAME).orElse(true);
            AnswerFormatType answerFormatType = BaseCommandOptions.getAnswerTypeFromStartCommandOption(saveAction).orElse(AnswerFormatType.full);
            AnswerInteractionType answerInteractionType = BaseCommandOptions.getAnswerInteractionFromStartCommandOption(saveAction);
            DiceImageStyle diceImageStyle = BaseCommandOptions.getDiceStyleOptionFromStartCommandOption(saveAction).orElse(DiceImageStyle.polyhedral_3d);
            String defaultDiceColor = BaseCommandOptions.getDiceColorOptionFromStartCommandOption(saveAction).orElse(DiceImageStyle.polyhedral_3d.getDefaultColor());
            final Locale userOrConfigLocale = BaseCommandOptions.getLocaleOptionFromStartCommandOption(saveAction)
                    .orElse(event.getRequester().getUserLocal());
            DirectRollConfig config = new DirectRollConfig(null, alwaysSumResults, answerFormatType, answerInteractionType, null, new DiceStyleAndColor(diceImageStyle, defaultDiceColor), userOrConfigLocale);
            BotMetrics.incrementSlashStartMetricCounter(getCommandId() + "_rollConfigSave");
            return Mono.defer(() -> {
                persistenceManager.deleteChannelConfig(event.getChannelId(), DIRECT_ROLL_CONFIG_TYPE_ID);
                persistenceManager.saveChannelConfig(new ChannelConfigDTO(uuidSupplier.get(),
                        event.getGuildId(),
                        event.getChannelId(),
                        null,
                        DirectRollCommand.ROLL_COMMAND_ID,
                        DIRECT_ROLL_CONFIG_TYPE_ID,
                        serializeConfig(config)
                ));
                log.info("{}: '{}' -> {}",
                        event.getRequester().toLogString(),
                        event.getCommandString().replace("`", ""),
                        config.toShortString()
                );
                return event.reply(I18n.getMessage("channel_config.save.reply", userLocal, event.getCommandString()), false);
            });
        }
        if (event.getOption(DELETE_DIRECT_ROLL_CONFIG_OPTION_NAME).isPresent()) {
            BotMetrics.incrementSlashStartMetricCounter(getCommandId() + "_rollConfigDelete");
            return Mono.defer(() -> {
                log.info("{}: '{}'",
                        event.getRequester().toLogString(),
                        event.getCommandString().replace("`", "")
                );
                persistenceManager.deleteChannelConfig(event.getChannelId(), DIRECT_ROLL_CONFIG_TYPE_ID);
                return event.reply(I18n.getMessage("channel_config.deleted.reply", userLocal, event.getCommandString()), false);
            });
        }
        if (event.getOption(ALIAS_OPTION_NAME).isPresent()) {
            CommandInteractionOption aliasOption = event.getOption(ALIAS_OPTION_NAME).get();
            if (SCOPE_OPTION_CHOICE_USER_CHANNEL_NAME.equals(aliasOption.getStringSubOptionWithName(SCOPE_OPTION_NAME).orElse(null))) {
                return handelChannelEvent(event, event.getUserId(), uuidSupplier, userLocal);
            } else if (SCOPE_OPTION_CHOICE_CHANNEL_NAME.equals(aliasOption.getStringSubOptionWithName(SCOPE_OPTION_NAME).orElse(null))) {
                return handelChannelEvent(event, null, uuidSupplier, userLocal);
            }
            log.error("missing scope in slash event: {}", event.getOptions());
        }
        log.error("unknown option for slash event: {} ", event.getOptions());
        return event.reply(I18n.getMessage("channel_config.unknown.reply", userLocal), false);
    }

    private void saveAlias(@NonNull Alias alias, @NonNull SlashEventAdaptor event, @Nullable Long userId, @NonNull Supplier<UUID> uuidSupplier) {
        final List<Alias> existingAlias = loadAlias(event.getChannelId(), userId)
                .stream()
                .filter(a -> !a.getName().equals(alias.getName()))
                .toList();

        List<Alias> newAliasList = ImmutableList.<Alias>builder()
                .addAll(existingAlias)
                .add(alias)
                .build();
        deleteAlias(event.getChannelId(), userId);
        saveAlias(event.getChannelId(), event.getGuildId(), userId, newAliasList, uuidSupplier);
    }

    private Mono<Void> handelChannelEvent(@NonNull SlashEventAdaptor event, @Nullable Long userId, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocale) {
        if (event.getOption(SAVE_ALIAS_OPTION_NAME).isPresent()) {
            String type = userId == null ? "channelAliasSave" : "user_channelAliasSave";
            BotMetrics.incrementSlashStartMetricCounter(getCommandId() + "_" + type);
            CommandInteractionOption commandInteractionOption = event.getOption(SAVE_ALIAS_OPTION_NAME).get();
            String name = commandInteractionOption.getStringSubOptionWithName(ALIAS_NAME_OPTION_NAME).orElseThrow();
            String value = commandInteractionOption.getStringSubOptionWithName(ALIAS_VALUE_OPTION_NAME).orElseThrow();

            Alias alias = new Alias(name, value);
            saveAlias(alias, event, userId, uuidSupplier);
            log.info("{}: save {} alias: {}",
                    event.getRequester().toLogString(),
                    userId == null ? "channel" : "user channel",
                    alias
            );

            return event.reply(I18n.getMessage("channel_config.savedAlias.reply", userLocale, event.getCommandString()), userId != null);
        } else if (event.getOption(SAVE_MULTI_ALIAS_OPTION_NAME).isPresent()) {
            BotMetrics.incrementSlashStartMetricCounter(getCommandId() + "_aliasMultiSave");
            CommandInteractionOption commandInteractionOption = event.getOption(SAVE_MULTI_ALIAS_OPTION_NAME).get();
            String aliasesString = commandInteractionOption.getStringSubOptionWithName(ALIASES_OPTION_NAME).orElseThrow();
            List<String> nameValuePair = Arrays.stream(aliasesString.split(";"))
                    .filter(s -> !Strings.isNullOrEmpty(s))
                    .toList();
            if (nameValuePair.isEmpty()) {
                return event.reply(I18n.getMessage("channel_config.noNameValue.reply", userLocale, event.getCommandString()), true);
            }
            List<String> missingNameValue = nameValuePair.stream()
                    .filter(s -> s.split(NAME_VALUE_DELIMITER).length != 2)
                    .toList();
            if (!missingNameValue.isEmpty()) {
                return event.reply(I18n.getMessage("channel_config.missingSeparator.reply", userLocale, event.getCommandString(), String.join(";", missingNameValue)), true);
            }
            List<Alias> aliases = nameValuePair.stream()
                    .map(s -> {
                        String[] split = s.split(NAME_VALUE_DELIMITER);
                        return new Alias(split[0], split[1]);
                    })
                    .toList();
            aliases.forEach(a -> saveAlias(a, event, userId, uuidSupplier));

            log.info("{}: save {} aliases: {}",
                    event.getRequester().toLogString(),
                    userId == null ? "channel" : "user channel",
                    aliases
            );
            return event.reply(I18n.getMessage("channel_config.savedAlias.reply", userLocale, event.getCommandString()), userId != null);
        } else if (event.getOption(DELETE_ALIAS_OPTION_NAME).isPresent()) {
            CommandInteractionOption commandInteractionOption = event.getOption(DELETE_ALIAS_OPTION_NAME).get();
            String name = commandInteractionOption.getStringSubOptionWithName(ALIAS_NAME_OPTION_NAME).orElseThrow();

            BotMetrics.incrementSlashStartMetricCounter(getCommandId() + "_aliasDelete");
            final List<Alias> existingAlias = loadAlias(event.getChannelId(), userId);
            if(existingAlias.stream().noneMatch(alias -> alias.getName().equals(name))){
                return event.reply(I18n.getMessage("channel_config.deletedAlias.notFound", userLocale, name), true);
            }
            List<Alias> newAliasList = existingAlias.stream()
                    .filter(alias -> !alias.getName().equals(name))
                    .toList();
            deleteAlias(event.getChannelId(), userId);
            saveAlias(event.getChannelId(), event.getGuildId(), userId, newAliasList, uuidSupplier);
            log.info("{}: delete {} alias: {}",
                    event.getRequester().toLogString(),
                    userId == null ? "channel" : "user channel",
                    name
            );
            return event.reply(I18n.getMessage("channel_config.deletedAlias.reply", userLocale, event.getCommandString()), userId != null);
        } else if (event.getOption(LIST_ALIAS_OPTION_NAME).isPresent()) {
            final List<Alias> existingAlias = loadAlias(event.getChannelId(), userId);

            BotMetrics.incrementSlashStartMetricCounter(getCommandId() + "_aliasList");

            String aliasList = existingAlias.stream().map(Objects::toString).collect(Collectors.joining("\n"));
            return event.reply(I18n.getMessage("channel_config.listAlias.reply", userLocale, event.getCommandString(), aliasList), userId != null);
        }
        log.error("unknown option for slash event: {} ", event.getOptions());
        return event.reply(I18n.getMessage("channel_config.unknown.reply", userLocale), false);
    }

    private List<Alias> loadAlias(long channelId, Long userId) {
        if (userId == null) {
            return getChannelAlias(channelId, persistenceManager);
        } else {
            return getUserChannelAlias(channelId, userId, persistenceManager);
        }
    }

    private void deleteAlias(long channelId, Long userId) {
        if (userId == null) {
            persistenceManager.deleteChannelConfig(channelId, CHANNEL_ALIAS_CONFIG_TYPE_ID);
        } else {
            persistenceManager.deleteUserChannelConfig(channelId, userId, USER_ALIAS_CONFIG_TYPE_ID);
        }
    }

    private void saveAlias(long channelId, @Nullable Long guildId, Long userId, @NonNull List<Alias> aliasList, @NonNull Supplier<UUID> uuidSupplier) {
        persistenceManager.saveChannelConfig(new ChannelConfigDTO(uuidSupplier.get(),
                guildId,
                channelId,
                userId,
                getCommandId(),
                userId == null ? CHANNEL_ALIAS_CONFIG_TYPE_ID : USER_ALIAS_CONFIG_TYPE_ID,
                Mapper.serializedObject(new AliasConfig(aliasList))));
    }
}
