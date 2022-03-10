package de.janno.discord.connector.command;

import de.janno.discord.connector.Metrics;
import de.janno.discord.connector.api.Answer;
import de.janno.discord.connector.api.EmbedDefinition;
import de.janno.discord.connector.api.ISlashCommand;
import de.janno.discord.connector.api.ISlashEventAdaptor;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import com.google.common.annotations.VisibleForTesting;
import de.janno.discord.connector.dice.DiceParserHelper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Optional;

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
        String checkPermissions = event.checkPermissions();
        if (checkPermissions != null) {
            return event.reply(checkPermissions);
        }

        String commandString = event.getCommandString();

        Optional<CommandInteractionOption> expressionOptional = event.getOption(ACTION_EXPRESSION);
        if (expressionOptional.isPresent()) {
            String commandParameter = expressionOptional
                    .map(CommandInteractionOption::getStringValue)
                    .orElseThrow();
            if (commandParameter.equals(HELP)) {
                Metrics.incrementSlashHelpMetricCounter(getName());
                return event.replyEphemeral(EmbedDefinition.builder()
                        .description("Type /r and a dice expression e.g. `/r 1d6` \n" + DiceParserHelper.HELP)
                        .build());
            }

            String validationMessage = diceParserHelper.validateDiceExpressionWitOptionalLabel(commandParameter, LABEL_DELIMITER, "`/r help`");
            if (validationMessage != null) {
                log.info("Validation message: {} for {}", validationMessage, commandString);
                return event.reply(String.format("%s\n%s", commandString, validationMessage));
            }
            String label;
            String diceExpression;

            if (commandParameter.contains(LABEL_DELIMITER)) {
                String[] split = commandParameter.split(LABEL_DELIMITER);
                label = split[1].trim();
                diceExpression = split[0].trim();
            } else {
                label = null;
                diceExpression = commandParameter;
            }
            Metrics.incrementSlashStartMetricCounter(getName(), diceExpression);

            Answer answer = diceParserHelper.roll(diceExpression, label);

            return event.reply(commandString)
                    .then(event.createResultMessageWithEventReference(answer))
                    .then(event.getRequester()
                            .doOnNext(requester -> log.info("'{}'.'{}' from '{}' slash '{}': {} -> {}",
                                    requester.getGuildName(),
                                    requester.getChannelName(),
                                    requester.getUserName(),
                                    event.getCommandString(),
                                    diceExpression,
                                    answer.toShortString()
                            ))
                            .ofType(Void.class));

        }

        return Mono.empty();
    }


}
