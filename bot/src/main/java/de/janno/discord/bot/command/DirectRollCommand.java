package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.dice.DiceParserHelper;
import de.janno.discord.connector.api.ISlashCommand;
import de.janno.discord.connector.api.ISlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DirectRollCommand implements ISlashCommand {
    private static final String ACTION_EXPRESSION = "expression";
    private static final String HELP = "help";
    private static final String LABEL_DELIMITER = "@";
    private final DiceParserHelper diceParserHelper;

    public DirectRollCommand() {
        this(new DiceParserHelper());
    }

    @VisibleForTesting
    public DirectRollCommand(DiceParserHelper diceParserHelper) {
        this.diceParserHelper = diceParserHelper;
    }

    @Override
    public String getName() {
        return "r";
    }

    @Override
    public CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getName())
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
    public Mono<Void> handleSlashCommandEvent(@NonNull ISlashEventAdaptor event) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Optional<String> checkPermissions = event.checkPermissions();
        if (checkPermissions.isPresent()) {
            return event.reply(checkPermissions.get());
        }

        String commandString = event.getCommandString();

        Optional<CommandInteractionOption> expressionOptional = event.getOption(ACTION_EXPRESSION);
        if (expressionOptional.isPresent()) {
            String commandParameter = expressionOptional
                    .map(CommandInteractionOption::getStringValue)
                    .orElseThrow();
            if (commandParameter.equals(HELP)) {
                BotMetrics.incrementSlashHelpMetricCounter(getName());
                return event.replyEmbed(EmbedDefinition.builder()
                        .description("Type /r and a dice expression e.g. `/r 1d6` \n" + DiceParserHelper.HELP)
                        .build(), true);
            }

            Optional<String> validationMessage = diceParserHelper.validateDiceExpressionWitOptionalLabel(commandParameter, LABEL_DELIMITER, "`/r help`", Integer.MAX_VALUE);
            if (validationMessage.isPresent()) {
                log.info("Validation message: {} for {}", validationMessage.get(), commandString);
                return event.reply(String.format("%s\n%s", commandString, validationMessage.get()));
            }

            String diceExpression = DiceParserHelper.getExpressionFromExpressionWithOptionalLabel(commandParameter, LABEL_DELIMITER);
            String label = DiceParserHelper.getLabelFromExpressionWithOptionalLabel(commandParameter, LABEL_DELIMITER).orElse(null);
            BotMetrics.incrementSlashStartMetricCounter(getName(), diceExpression);

            EmbedDefinition answer = diceParserHelper.roll(diceExpression, label);

            return Flux.merge(event.reply(commandString),
                            event.createResultMessageWithEventReference(answer))

                    .then(event.getRequester()
                            .doOnNext(requester -> log.info("'{}'.'{}' from '{}' slash '{}': {} -> {} in {}ms",
                                    requester.getGuildName(),
                                    requester.getChannelName(),
                                    requester.getUserName(),
                                    event.getCommandString(),
                                    diceExpression,
                                    answer.toShortString(),
                                    stopwatch.elapsed(TimeUnit.MILLISECONDS)
                            ))
                            .ofType(Void.class));

        }

        return Mono.empty();
    }


}
