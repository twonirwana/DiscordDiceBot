package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.dice.*;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.NumberSupplier;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DirectRollCommand implements SlashCommand {
    private static final String ACTION_EXPRESSION = "expression";
    private static final String HELP = "help";
    private final DiceSystemAdapter diceSystemAdapter;

    public DirectRollCommand() {
        this(new RandomNumberSupplier(), new DiceParser());
    }

    @VisibleForTesting
    public DirectRollCommand(NumberSupplier numberSupplier, Dice dice) {
        this.diceSystemAdapter = new DiceSystemAdapter(numberSupplier, 1000, dice);
    }

    @Override
    public String getCommandId() {
        return "r";
    }

    @Override
    public CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .description("direct roll of dice expression")
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_EXPRESSION)
                        .required(true)
                        .description("dice expression, e.g. '2d6'")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build();
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Optional<String> checkPermissions = event.checkPermissions();
        if (checkPermissions.isPresent()) {
            return event.reply(checkPermissions.get(), false);
        }

        String commandString = event.getCommandString();

        Optional<CommandInteractionOption> expressionOptional = event.getOption(ACTION_EXPRESSION);
        if (expressionOptional.isPresent()) {
            String commandParameter = expressionOptional
                    .map(CommandInteractionOption::getStringValue)
                    .orElseThrow();
            if (commandParameter.equals(HELP)) {
                BotMetrics.incrementSlashHelpMetricCounter(getCommandId());
                return event.replyEmbed(EmbedOrMessageDefinition.builder()
                        .descriptionOrContent("Type /r and a dice expression.\n" + DiceEvaluatorAdapter.getHelp())
                        .field(new EmbedOrMessageDefinition.Field("Example", "`/r expression:1d6`", false))
                        .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                        .field(new EmbedOrMessageDefinition.Field("Discord Server", "https://discord.gg/e43BsqKpFr", false))
                        .build(), true);
            }

            Optional<String> validationMessage = diceSystemAdapter.validateDiceExpressionWitOptionalLabel(commandParameter, "`/r expression:help`", DiceParserSystem.DICE_EVALUATOR);
            if (validationMessage.isPresent()) {
                log.info("{} Validation message: {} for {}", event.getRequester().toLogString(),
                        validationMessage.get(),
                        commandString);
                return event.reply(String.format("%s\n%s", commandString, validationMessage.get()), true);
            }

            String diceExpression = DiceSystemAdapter.getExpressionFromExpressionWithOptionalLabel(commandParameter);
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), diceExpression);
            BotMetrics.incrementAnswerFormatCounter(AnswerFormatType.full, getCommandId());

            RollAnswer answer = diceSystemAdapter.answerRollWithOptionalLabelInExpression(commandParameter, true, DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none);

            return Flux.merge(Mono.defer(event::acknowledgeAndRemoveSlash),
                            Mono.defer(() -> event.createResultMessageWithEventReference(RollAnswerConverter.toEmbedOrMessageDefinition(answer))
                                    .doOnSuccess(v -> log.info("{}: '{}'={} -> {} in {}ms",
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


}
