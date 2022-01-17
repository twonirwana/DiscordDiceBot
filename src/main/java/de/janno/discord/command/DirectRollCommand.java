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

@Slf4j
public class DirectRollCommand implements ISlashCommand {
    private static final String ACTION_EXPRESSION = "expression";
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

        if (event.getOption(ACTION_EXPRESSION).isPresent()) {
            ApplicationCommandInteractionOption options = event.getOption(ACTION_EXPRESSION).get();
            String diceExpression = options.getValue()
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow();
            String validationMessage = diceParserHelper.validateDiceExpression(diceExpression, "/custom_dice help");
            if (validationMessage != null) {
                log.info("Validation message: {}", validationMessage);
                return event.reply(String.format("%s\n%s", commandString, validationMessage));
            }
            Metrics.incrementSlashStartMetricCounter(getName(), diceExpression);

            Answer answer = diceParserHelper.roll(diceExpression);
            log.info(String.format("%s:%s -> %s", getName(), diceExpression, answer.toShortString()));

            return event.reply(commandString)
                    .then(event.createResultMessageWithEventReference(answer));

        }

        return Mono.empty();
    }
}
