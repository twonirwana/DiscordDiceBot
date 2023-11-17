package de.janno.discord.bot.command.channelConfig;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.BaseCommandOptions;
import de.janno.discord.bot.command.directRoll.DirectRollCommand;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static de.janno.discord.bot.command.channelConfig.AliasHelper.*;

@Slf4j
public class ChannelConfigCommand implements SlashCommand {
    //todo i18n
    public static final String DIRECT_ROLL_CONFIG_TYPE_ID = "DirectRollConfig";
    static final String ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID = "always_sum_result";
    private static final String COMMAND_ID = "channel_config";
    private static final String SAVE_DIRECT_ROLL_CONFIG_ACTION = "save_direct_roll_config";
    private static final String DELETE_DIRECT_ROLL_CONFIG_ACTION = "delete_direct_roll_config";
    private static final String CHANNEL_ALIAS = "channel_alias";
    private static final String USER_CHANNEL_ALIAS = "user_channel_alias";
    private static final String SAVE_ALIAS_ACTION = "save";
    private static final String SAVE_MULTI_ALIAS_ACTION = "multi_save";
    private static final String ALIAS_NAME_OPTION = "name";
    private static final String ALIASES_OPTION = "aliases";
    private static final String ALIAS_VALUE_OPTION = "value";
    private static final String LIST_ALIAS_ACTION = "list";
    private static final String DELETE_ALIAS_ACTION = "delete";
    private static final CommandDefinitionOption SAVE_ALIAS_OPTION = CommandDefinitionOption.builder()
            .type(CommandDefinitionOption.Type.SUB_COMMAND)
            .name(SAVE_ALIAS_ACTION)
            .description("Add a new alias")
            .option(CommandDefinitionOption.builder()
                    .type(CommandDefinitionOption.Type.STRING)
                    .name(ALIAS_NAME_OPTION)
                    .description("The name of the alias (the name in the expression will be replaced with the value)")
                    .required(true)
                    .build())
            .option(CommandDefinitionOption.builder()
                    .type(CommandDefinitionOption.Type.STRING)
                    .name(ALIAS_VALUE_OPTION)
                    .description("The value of the alias (the name in the expression will be replaced with the value)")
                    .required(true)
                    .build())
            .build();
    private static final CommandDefinitionOption MULTI_SAVE_ALIAS_OPTION = CommandDefinitionOption.builder()
            .type(CommandDefinitionOption.Type.SUB_COMMAND)
            .name(SAVE_MULTI_ALIAS_ACTION)
            .description("Save multiple alias")
            .option(CommandDefinitionOption.builder()
                    .type(CommandDefinitionOption.Type.STRING)
                    .name(ALIASES_OPTION)
                    .description("Separate alias name and value by `:` and aliases by `;` e.g.: att:2d20;dmg:2d6+3=")
                    .required(true)
                    .build())
            .build();
    private static final CommandDefinitionOption DELETE_ALIAS_OPTION = CommandDefinitionOption.builder()
            .type(CommandDefinitionOption.Type.SUB_COMMAND)
            .name(DELETE_ALIAS_ACTION)
            .description("Delete alias")
            .option(CommandDefinitionOption.builder()
                    .type(CommandDefinitionOption.Type.STRING)
                    .name(ALIAS_NAME_OPTION)
                    .description("The name of the alias to delete")
                    .required(true)
                    .build())
            .build();
    private static final CommandDefinitionOption LIST_ALIAS_OPTION = CommandDefinitionOption.builder()
            .type(CommandDefinitionOption.Type.SUB_COMMAND)
            .name(LIST_ALIAS_ACTION)
            .description("List all alias")
            .build();
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
                //todo i18n
                .description("Configure options in this channel")
                .option(CommandDefinitionOption.builder()
                        .name(SAVE_DIRECT_ROLL_CONFIG_ACTION)
                        .description("add or update the channel config")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .option(BaseCommandOptions.ANSWER_FORMAT_COMMAND_OPTION)
                        .option(BaseCommandOptions.DICE_IMAGE_STYLE_COMMAND_OPTION)
                        .option(BaseCommandOptions.DICE_IMAGE_COLOR_COMMAND_OPTION)
                        .option(CommandDefinitionOption.builder()
                                .name(ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID)
                                .description("Always sum the results of the dice expressions")
                                .type(CommandDefinitionOption.Type.BOOLEAN)
                                .required(false)
                                .build())
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(DELETE_DIRECT_ROLL_CONFIG_ACTION)
                        .description("remove the current channel config")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND_GROUP)
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(CHANNEL_ALIAS)
                        .description("add, list or remove channel alias")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND_GROUP)
                        .option(SAVE_ALIAS_OPTION)
                        .option(DELETE_ALIAS_OPTION)
                        .option(LIST_ALIAS_OPTION)
                        .option(MULTI_SAVE_ALIAS_OPTION)
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(USER_CHANNEL_ALIAS)
                        .description("add, list or remove user channel alias")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND_GROUP)
                        .option(SAVE_ALIAS_OPTION)
                        .option(DELETE_ALIAS_OPTION)
                        .option(LIST_ALIAS_OPTION)
                        .option(MULTI_SAVE_ALIAS_OPTION)
                        .build())
                .build();
    }

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest autoCompleteRequest, @NonNull Locale userLocale) {
        return BaseCommandOptions.autoCompleteColorOption(autoCompleteRequest, userLocale);
    }

    private String serializeConfig(DirectRollConfig channelConfig) {
        return Mapper.serializedObject(channelConfig);
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocal) {
        if (event.getOption(SAVE_DIRECT_ROLL_CONFIG_ACTION).isPresent()) {
            CommandInteractionOption saveAction = event.getOption(SAVE_DIRECT_ROLL_CONFIG_ACTION).get();
            boolean alwaysSumResults = saveAction.getBooleanSubOptionWithName(ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID).orElse(true);
            AnswerFormatType answerType = BaseCommandOptions.getAnswerTypeFromStartCommandOption(saveAction).orElse(AnswerFormatType.full);
            DiceImageStyle diceImageStyle = BaseCommandOptions.getDiceStyleOptionFromStartCommandOption(saveAction).orElse(DiceImageStyle.polyhedral_3d);
            String defaultDiceColor = BaseCommandOptions.getDiceColorOptionFromStartCommandOption(saveAction).orElse(DiceImageStyle.polyhedral_3d.getDefaultColor());
            final Locale userOrConfigLocale = BaseCommandOptions.getLocaleOptionFromStartCommandOption(saveAction)
                    .orElse(event.getRequester().getUserLocal());
            DirectRollConfig config = new DirectRollConfig(null, alwaysSumResults, answerType, null, new DiceStyleAndColor(diceImageStyle, defaultDiceColor), userOrConfigLocale);
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), config.toShortString());
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
                //todo i18n
                return event.reply("`%s`\nSaved direct roll channel config".formatted(event.getCommandString()), false);
            });
        }
        if (event.getOption(DELETE_DIRECT_ROLL_CONFIG_ACTION).isPresent()) {
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), "delete");
            return Mono.defer(() -> {
                log.info("{}: '{}'",
                        event.getRequester().toLogString(),
                        event.getCommandString().replace("`", "")
                );
                persistenceManager.deleteChannelConfig(event.getChannelId(), DIRECT_ROLL_CONFIG_TYPE_ID);
                //todo i18n
                return event.reply("`%s`\nDeleted direct roll channel config".formatted(event.getCommandString()), false);
            });
        }
        if (event.getOption(CHANNEL_ALIAS).isPresent()) {
            return handelChannelEvent(event, null, uuidSupplier);
        }
        if (event.getOption(USER_CHANNEL_ALIAS).isPresent()) {
            return handelChannelEvent(event, event.getUserId(), uuidSupplier);
        }
        log.error("unknown option for slash event: {} ", event.getOptions());
        return event.reply("Unknown slash event options", false);
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

    private Mono<Void> handelChannelEvent(@NonNull SlashEventAdaptor event, @Nullable Long userId, @NonNull Supplier<UUID> uuidSupplier) {
        String type = userId == null ? "channel_alias" : "user_channel_alias";
        if (event.getOption(SAVE_ALIAS_ACTION).isPresent()) {
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), type + ", save");
            CommandInteractionOption commandInteractionOption = event.getOption(SAVE_ALIAS_ACTION).get();
            String name = commandInteractionOption.getStringSubOptionWithName(ALIAS_NAME_OPTION).orElseThrow();
            String value = commandInteractionOption.getStringSubOptionWithName(ALIAS_VALUE_OPTION).orElseThrow();

            Alias alias = new Alias(name, value);
            saveAlias(alias, event, userId, uuidSupplier);
            log.info("{}: save {} alias: {}",
                    event.getRequester().toLogString(),
                    userId == null ? "channel" : "user channel",
                    alias
            );
            //todo i18n
            return event.reply("`%s`\nSaved new alias".formatted(event.getCommandString()), userId != null);
        } else if (event.getOption(SAVE_MULTI_ALIAS_ACTION).isPresent()) {
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), type + ", multi save");
            CommandInteractionOption commandInteractionOption = event.getOption(SAVE_MULTI_ALIAS_ACTION).get();
            String aliasesString = commandInteractionOption.getStringSubOptionWithName(ALIASES_OPTION).orElseThrow();
            List<String> nameValuePair = Arrays.stream(aliasesString.split(";"))
                    .filter(s -> !Strings.isNullOrEmpty(s))
                    .toList();
            if (nameValuePair.isEmpty()) {
                //todo i18n
                return event.reply("`%s`\nNo name/value pair".formatted(event.getCommandString()), true);
            }
            List<String> missingNameValue = nameValuePair.stream()
                    .filter(s -> StringUtils.countMatches(s, ":") != 1)
                    .toList();
            if (!missingNameValue.isEmpty()) {
                //todo i18n
                return event.reply("`%s`\nMissing name value separator `:` in: %s".formatted(event.getCommandString(), missingNameValue), true);
            }
            List<Alias> aliases = nameValuePair.stream()
                    .map(s -> {
                        String[] split = s.split(":");
                        return new Alias(split[0], split[1]);
                    })
                    .toList();
            aliases.forEach(a -> saveAlias(a, event, userId, uuidSupplier));

            log.info("{}: save {} aliases: {}",
                    event.getRequester().toLogString(),
                    userId == null ? "channel" : "user channel",
                    aliases
            );
            return event.reply("`%s`\nSaved new alias".formatted(event.getCommandString()), userId != null);
        } else if (event.getOption(DELETE_ALIAS_ACTION).isPresent()) {
            CommandInteractionOption commandInteractionOption = event.getOption(DELETE_ALIAS_ACTION).get();
            String name = commandInteractionOption.getStringSubOptionWithName(ALIAS_NAME_OPTION).orElseThrow();

            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), type + ", delete");
            final List<Alias> existingAlias = loadAlias(event.getChannelId(), userId);

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
            //todo i18n
            return event.reply("`%s`\ndeleted alias".formatted(event.getCommandString()), userId != null);
        } else if (event.getOption(LIST_ALIAS_ACTION).isPresent()) {
            final List<Alias> existingAlias = loadAlias(event.getChannelId(), userId);

            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), type + ", list");

            String aliasList = existingAlias.stream().map(Objects::toString).collect(Collectors.joining("\n"));
            //todo i18n
            return event.reply("`%s`\nexisting alias:\n%s".formatted(event.getCommandString(), aliasList), userId != null);
        }
        log.error("unknown option for slash event: {} ", event.getOptions());
        //todo i18n
        return event.reply("Unknown slash event options", false);
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

    private void saveAlias(long channelId, long guildId, Long userId, @NonNull List<Alias> aliasList, @NonNull Supplier<UUID> uuidSupplier) {
        persistenceManager.saveChannelConfig(new ChannelConfigDTO(uuidSupplier.get(),
                guildId,
                channelId,
                userId,
                getCommandId(),
                userId == null ? CHANNEL_ALIAS_CONFIG_TYPE_ID : USER_ALIAS_CONFIG_TYPE_ID,
                Mapper.serializedObject(new AliasConfig(aliasList))));
    }
}
