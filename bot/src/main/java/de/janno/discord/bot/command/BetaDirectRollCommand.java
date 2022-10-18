package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.dice.DiceEvaluatorHelper;
import de.janno.discord.bot.dice.DiceParserHelper;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.NumberSupplier;
import de.janno.evaluator.dice.RandomNumberSupplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BetaDirectRollCommand implements SlashCommand {
    private static final String ACTION_EXPRESSION = "expression";
    private static final String HELP = "help";
    private static final String LABEL_DELIMITER = "@";
    private final DiceEvaluatorHelper diceEvaluatorHelper;

    public BetaDirectRollCommand() {
        this(new RandomNumberSupplier());
    }

    @VisibleForTesting
    public BetaDirectRollCommand(NumberSupplier numberSupplier) {
        this.diceEvaluatorHelper = new DiceEvaluatorHelper(numberSupplier, 1000);
    }

    @Override
    public String getCommandId() {
        return "beta_roll";
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
            return event.reply(checkPermissions.get());
        }

        Optional<CommandInteractionOption> expressionOptional = event.getOption(ACTION_EXPRESSION);
        if (expressionOptional.isPresent()) {
            String commandParameter = expressionOptional
                    .map(CommandInteractionOption::getStringValue)
                    .orElseThrow();
            if (commandParameter.equals(HELP)) {
                BotMetrics.incrementSlashHelpMetricCounter(getCommandId());
                return event.replyEmbed(EmbedDefinition.builder()
                        .description("Type /r and a dice expression e.g. `/r 1d6` \n" + DiceParserHelper.HELP)
                        .build(), true);
            }

            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), commandParameter);

            EmbedDefinition answer = diceEvaluatorHelper.answerRoll(commandParameter, LABEL_DELIMITER);
            String answerShortString = answer.toShortString();
            return Flux.merge(event.acknowledgeAndRemoveSlash(),
                            event.createResultMessageWithEventReference(answer))
                    .then(event.getRequester()
                            .doOnNext(requester -> log.info("'{}'.'{}': '{}'={} -> {} in {}ms",
                                    requester.getGuildName(),
                                    requester.getChannelName(),
                                    event.getCommandString(),
                                    commandParameter,
                                    answerShortString,
                                    stopwatch.elapsed(TimeUnit.MILLISECONDS)
                            ))
                            .ofType(Void.class));

        }

        return Mono.empty();
    }

}
