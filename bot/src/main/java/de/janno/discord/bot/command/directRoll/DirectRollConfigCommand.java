package de.janno.discord.bot.command.directRoll;

import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.DefaultCommandOptions;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistanceManager;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
public class DirectRollConfigCommand extends AbstractDirectRollCommand {

    private static final String SAVE_ACTION = "save";
    private static final String DELETE_ACTION = "delete";
    private static final String ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID = "always_sum_result";
    private final PersistanceManager persistanceManager;

    public DirectRollConfigCommand(PersistanceManager persistanceManager) {
        this.persistanceManager = persistanceManager;
    }

    @Override
    public String getCommandId() {
        return "direct_roll_config";
    }

    @Override
    public CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .description("Configure the direct roll command in this channel")
                .option(CommandDefinitionOption.builder()
                        .name(SAVE_ACTION)
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
                        .name(DELETE_ACTION)
                        .description("remove the current channel config")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .build())
                .build();
    }

    String serializeConfig(DirectRollConfig directRollConfig) {
        return Mapper.serializedObject(directRollConfig);
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event) {
        if (event.getOption(SAVE_ACTION).isPresent()) {
            CommandInteractionOption saveAction = event.getOption(SAVE_ACTION).get();
            Long answerTargetChannelId = null;
            boolean alwaysSumResults = saveAction.getBooleanSubOptionWithName(ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID).orElse(true);
            AnswerFormatType answerType = DefaultCommandOptions.getAnswerTypeFromStartCommandOption(saveAction).orElse(AnswerFormatType.full);
            ResultImage resultImage = DefaultCommandOptions.getResultImageOptionFromStartCommandOption(saveAction).orElse(ResultImage.polyhedral_3d_red_and_white);
            DirectRollConfig config = new DirectRollConfig(answerTargetChannelId, alwaysSumResults, answerType, resultImage);
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), config.toShortString());
            return Mono.defer(() -> {
                persistanceManager.deleteChannelConfig(event.getChannelId(), CONFIG_TYPE_ID);
                persistanceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(),
                        event.getGuildId(),
                        event.getChannelId(),
                        ROLL_COMMAND_ID,
                        CONFIG_TYPE_ID,
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
        if (event.getOption(DELETE_ACTION).isPresent()) {
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), "delete");
            return Mono.defer(() -> {
                log.info("{}: '{}'",
                        event.getRequester().toLogString(),
                        event.getCommandString().replace("`", "")
                );
                persistanceManager.deleteChannelConfig(event.getChannelId(), CONFIG_TYPE_ID);
                return event.reply("`%s`\nDeleted direct roll channel config".formatted(event.getCommandString()), false);
            });
        }
        log.error("unknown option for slash event: {} ", event.getOptions());
        return event.reply("Unknown slash event options", false);
    }
}
