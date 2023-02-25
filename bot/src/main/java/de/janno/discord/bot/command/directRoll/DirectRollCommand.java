package de.janno.discord.bot.command.directRoll;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.command.RollAnswerConverter;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.dice.DiceSystemAdapter;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class DirectRollCommand extends AbstractDirectRollCommand {

    private static final String ACTION_EXPRESSION = "expression";
    private static final String HELP = "help";
    private final DiceEvaluatorAdapter diceEvaluatorAdapter;
    private final PersistenceManager persistenceManager;

    public DirectRollCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        super(persistenceManager);
        this.diceEvaluatorAdapter = new DiceEvaluatorAdapter(cachingDiceEvaluator);
        this.persistenceManager = persistenceManager;
    }

    @Override
    public String getCommandId() {
        return ROLL_COMMAND_ID;
    }

    @Override
    public CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .description("direct roll of dice expression, configuration with /direct_roll_config")
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_EXPRESSION)
                        .required(true)
                        .description("dice expression, e.g. '2d6'")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build();
    }

    private DirectRollConfig getDirectRollConfig(long channelId) {
        return persistenceManager.getChannelConfig(channelId, CONFIG_TYPE_ID)
                .map(this::deserializeConfig)
                .orElse(new DirectRollConfig(null, true, AnswerFormatType.full, ResultImage.polyhedral_3d_red_and_white));
    }

    @VisibleForTesting
    DirectRollConfig deserializeConfig(ChannelConfigDTO channelConfigDTO) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(channelConfigDTO.getConfigClassId()), "Unknown configClassId: %s", channelConfigDTO.getConfigClassId());
        return Mapper.deserializeObject(channelConfigDTO.getConfig(), DirectRollConfig.class);
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Optional<String> checkPermissions = event.checkPermissions();
        if (checkPermissions.isPresent()) {
            return event.reply(checkPermissions.get(), false);
        }

        final String commandString = event.getCommandString();

        Optional<CommandInteractionOption> expressionOptional = event.getOption(ACTION_EXPRESSION);
        if (expressionOptional.isPresent()) {
            final String commandParameter = expressionOptional
                    .map(CommandInteractionOption::getStringValue)
                    .orElseThrow();
            if (commandParameter.equals(HELP)) {
                BotMetrics.incrementSlashHelpMetricCounter(getCommandId());
                return event.replyEmbed(EmbedOrMessageDefinition.builder()
                        .descriptionOrContent("Type /r and a dice expression, configuration with /direct_roll_config\n" + DiceEvaluatorAdapter.getHelp())
                        .field(new EmbedOrMessageDefinition.Field("Example", "`/r expression:1d6`", false))
                        .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                        .field(new EmbedOrMessageDefinition.Field("Discord Server for Help and News", "https://discord.gg/e43BsqKpFr", false))
                        .build(), true);
            }

            final List<Alias> channelAlias = getChannelAlias(event.getChannelId());
            final List<Alias> userChannelAlias = getUserChannelAlias(event.getChannelId(), event.getUserId());

            final String expressionWithOptionalLabelsAndAppliedAliases = applyAliaseToExpression(channelAlias, userChannelAlias, commandParameter);
            Optional<String> labelValidationMessage = DiceSystemAdapter.validateLabel(expressionWithOptionalLabelsAndAppliedAliases);
            if (labelValidationMessage.isPresent()) {
                return replyValidationMessage(event, labelValidationMessage.get(), commandString);
            }
            String diceExpression = DiceSystemAdapter.getExpressionFromExpressionWithOptionalLabel(expressionWithOptionalLabelsAndAppliedAliases);
            Optional<String> expressionValidationMessage = diceEvaluatorAdapter.validateDiceExpression(diceExpression, "`/r expression:help`");
            if (expressionValidationMessage.isPresent()) {
                return replyValidationMessage(event, expressionValidationMessage.get(), commandString);
            }

            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), diceExpression);
            DirectRollConfig config = getDirectRollConfig(event.getChannelId());
            BotMetrics.incrementAnswerFormatCounter(config.getAnswerFormatType(), getCommandId());

            RollAnswer answer = diceEvaluatorAdapter.answerRollWithOptionalLabelInExpression(expressionWithOptionalLabelsAndAppliedAliases, DiceSystemAdapter.LABEL_DELIMITER, config.isAlwaysSumResult(), config.getAnswerFormatType(), config.getResultImage());

            return Flux.merge(Mono.defer(event::acknowledgeAndRemoveSlash),
                            Mono.defer(() -> event.createResultMessageWithEventReference(RollAnswerConverter.toEmbedOrMessageDefinition(answer))
                                    .doOnSuccess(v ->
                                            log.info("{}: '{}'={} -> {} in {}ms",
                                                    event.getRequester().toLogString(),
                                                    commandString.replace("`", ""),
                                                    diceExpression,
                                                    answer.toShortString(),
                                                    stopwatch.elapsed(TimeUnit.MILLISECONDS)
                                            )))
                    )
                    .parallel().then();

        }

        return Mono.empty();
    }

    private Mono<Void> replyValidationMessage(@NonNull SlashEventAdaptor event, @NonNull String validationMessage, @NonNull String commandString) {
        log.info("{} Validation message: {} for {}", event.getRequester().toLogString(),
                validationMessage,
                commandString);
        return event.reply(String.format("%s\n%s", commandString, validationMessage), true);
    }

    @VisibleForTesting
    String applyAliaseToExpression(@NonNull List<Alias> channelAlias, @NonNull List<Alias> userAlias, final String expressionWithOptionalLabel) {
        if (channelAlias.isEmpty() && userAlias.isEmpty()) {
            return expressionWithOptionalLabel;
        }
        String expressionWithOptionalLabelsAndAppliedAliases = expressionWithOptionalLabel;

        //specific before general
        for (Alias alias : userAlias) {
            expressionWithOptionalLabelsAndAppliedAliases = expressionWithOptionalLabelsAndAppliedAliases.replace(alias.getName(), alias.getValue());
        }

        for (Alias alias : channelAlias) {
            expressionWithOptionalLabelsAndAppliedAliases = expressionWithOptionalLabelsAndAppliedAliases.replace(alias.getName(), alias.getValue());
        }


        return expressionWithOptionalLabelsAndAppliedAliases;
    }

}
