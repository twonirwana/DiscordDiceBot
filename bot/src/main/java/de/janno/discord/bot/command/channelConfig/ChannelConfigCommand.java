package de.janno.discord.bot.command.channelConfig;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.DefaultCommandOptions;
import de.janno.discord.bot.command.directRoll.DirectRollCommand;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static de.janno.discord.bot.command.channelConfig.AliasHelper.*;

@Slf4j
public class ChannelConfigCommand implements SlashCommand {

    public static final String DIRECT_ROLL_CONFIG_TYPE_ID = "DirectRollConfig";
    private static final String COMMAND_ID = "channel_config";
    private static final String SAVE_DIRECT_ROLL_CONFIG_ACTION = "save_direct_roll_config";
    private static final String DELETE_DIRECT_ROLL_CONFIG_ACTION = "delete_direct_roll_config";
    private static final String ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID = "always_sum_result";
    private static final String CHANNEL_ALIAS = "channel_alias";
    private static final String USER_CHANNEL_ALIAS = "user_channel_alias";
    private static final String SAVE_ALIAS_ACTION = "save";
    private static final String ALIAS_NAME_OPTION = "name";
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
    public String getCommandId() {
        return COMMAND_ID;
    }

    @Override
    public CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .description("Configure options in this channel")
                .option(CommandDefinitionOption.builder()
                        .name(SAVE_DIRECT_ROLL_CONFIG_ACTION)
                        .description("add or update the channel config")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .option(DefaultCommandOptions.ANSWER_FORMAT_COMMAND_OPTION)
                        .option(DefaultCommandOptions.RESULT_IMAGE_COMMAND_OPTION)
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
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(USER_CHANNEL_ALIAS)
                        .description("add, list or remove user channel alias")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND_GROUP)
                        .option(SAVE_ALIAS_OPTION)
                        .option(DELETE_ALIAS_OPTION)
                        .option(LIST_ALIAS_OPTION)
                        .build())
                .build();
    }

    private String serializeConfig(DirectRollConfig channelConfig) {
        return Mapper.serializedObject(channelConfig);
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier) {
        if (event.getOption(SAVE_DIRECT_ROLL_CONFIG_ACTION).isPresent()) {
            CommandInteractionOption saveAction = event.getOption(SAVE_DIRECT_ROLL_CONFIG_ACTION).get();
            boolean alwaysSumResults = saveAction.getBooleanSubOptionWithName(ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID).orElse(true);
            AnswerFormatType answerType = DefaultCommandOptions.getAnswerTypeFromStartCommandOption(saveAction).orElse(AnswerFormatType.full);
            ResultImage resultImage = DefaultCommandOptions.getResultImageOptionFromStartCommandOption(saveAction).orElse(ResultImage.polyhedral_3d_red_and_white);
            DirectRollConfig config = new DirectRollConfig(null, alwaysSumResults, answerType, resultImage);
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
                return event.reply("`%s`\nDeleted direct roll channel config".formatted(event.getCommandString()), false);
            });
        }
        if (event.getOption(CHANNEL_ALIAS).isPresent()) {
            return handelChannelEvent(event, null);
        }
        if (event.getOption(USER_CHANNEL_ALIAS).isPresent()) {
            return handelChannelEvent(event, event.getUserId());
        }
        log.error("unknown option for slash event: {} ", event.getOptions());
        return event.reply("Unknown slash event options", false);
    }

    private Mono<Void> handelChannelEvent(@NonNull SlashEventAdaptor event, @Nullable Long userId) {
        String type = userId == null ? "channel_alias" : "user_channel_alias";
        if (event.getOption(SAVE_ALIAS_ACTION).isPresent()) {
            CommandInteractionOption commandInteractionOption = event.getOption(SAVE_ALIAS_ACTION).get();
            String name = commandInteractionOption.getStringSubOptionWithName(ALIAS_NAME_OPTION).orElseThrow();
            String value = commandInteractionOption.getStringSubOptionWithName(ALIAS_VALUE_OPTION).orElseThrow();
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), type + ", save");
            Alias alias = new Alias(name, value);

            final List<Alias> existingAlias = loadAlias(event.getChannelId(), userId)
                    .stream()
                    .filter(a -> !a.getName().equals(name))
                    .toList();

            List<Alias> newAliasList = ImmutableList.<Alias>builder()
                    .addAll(existingAlias)
                    .add(alias)
                    .build();
            deleteAlias(event.getChannelId(), userId);
            saveAlias(event.getChannelId(), event.getGuildId(), userId, newAliasList);
            log.info("{}: save {} alias: {}",
                    event.getRequester().toLogString(),
                    userId == null ? "channel" : "user channel",
                    alias
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
            saveAlias(event.getChannelId(), event.getGuildId(), userId, newAliasList);
            log.info("{}: delete {} alias: {}",
                    event.getRequester().toLogString(),
                    userId == null ? "channel" : "user channel",
                    name
            );
            return event.reply("`%s`\ndeleted alias".formatted(event.getCommandString()), userId != null);
        } else if (event.getOption(LIST_ALIAS_ACTION).isPresent()) {
            final List<Alias> existingAlias = loadAlias(event.getChannelId(), userId);

            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), type + ", list");

            String aliasList = existingAlias.stream().map(Objects::toString).collect(Collectors.joining("\n"));
            return event.reply("`%s`\nexisting alias:\n%s".formatted(event.getCommandString(), aliasList), userId != null);
        }
        log.error("unknown option for slash event: {} ", event.getOptions());
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

    private void saveAlias(long channelId, long guildId, Long userId, List<Alias> aliasList) {
        persistenceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(),
                guildId,
                channelId,
                userId,
                getCommandId(),
                userId == null ? CHANNEL_ALIAS_CONFIG_TYPE_ID : USER_ALIAS_CONFIG_TYPE_ID,
                Mapper.serializedObject(new AliasConfig(aliasList))));
    }
}
