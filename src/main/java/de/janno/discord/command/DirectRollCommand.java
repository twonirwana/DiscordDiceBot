package de.janno.discord.command;

import com.google.common.annotations.VisibleForTesting;
import de.janno.discord.Metrics;
import de.janno.discord.dice.DiceParserHelper;
import de.janno.discord.discord4j.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
public class DirectRollCommand implements ISlashCommand {
    private static final String ACTION_EXPRESSION = "expression";
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
    public ApplicationCommand getApplicationCommand() {
        return ApplicationCommand.builder()
                .name(getName())
                .description("direct roll of dice expression")
                .option(ApplicationCommandOptionData.builder()
                        .name(ACTION_EXPRESSION)
                        .required(true)
                        .description("dice expression, e.g. '2d6'")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
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
        log.info("Application command: {}", commandString);

        Optional<ApplicationCommandInteractionOption> expressionOptional = event.getOption(ACTION_EXPRESSION);
        if (expressionOptional.isPresent()) {
            String commandParameter = expressionOptional
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow();

            String validationMessage = validate(commandParameter);
            if (validationMessage != null) {
                log.info("Validation message: {}", validationMessage);
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
            log.info(String.format("%s:%s -> %s", getName(), diceExpression, answer.toShortString()));

            return event.reply(commandString)
                    .then(event.createResultMessageWithEventReference(answer));

        }

        return Mono.empty();
    }

    @VisibleForTesting
    String validate(@NonNull String startOptionString) {
        String label;
        String diceExpression;

        if (startOptionString.contains(LABEL_DELIMITER)) {
            String[] split = startOptionString.split(LABEL_DELIMITER);
            if (split.length != 2) {
                return String.format("The button definition '%s' should have the diceExpression@Label", startOptionString);
            }
            label = split[1].trim();
            diceExpression = split[0].trim();
        } else {
            label = startOptionString;
            diceExpression = startOptionString;
        }
        if (label.length() > 80) {
            return String.format("Label for '%s' is to long, max number of characters is 80", startOptionString);
        }
        if (label.isBlank()) {
            return String.format("Label for '%s' requires a visible name", startOptionString);
        }
        if (diceExpression.isBlank()) {
            return String.format("Dice expression for '%s' is empty", startOptionString);
        }
        return diceParserHelper.validateDiceExpression(diceExpression, "custom_dice help");

    }
}
