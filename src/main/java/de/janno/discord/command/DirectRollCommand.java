package de.janno.discord.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import de.janno.discord.Metrics;
import de.janno.discord.dice.DiceParserHelper;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.discord4j.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

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
        if (event.getOption(ACTION_EXPRESSION).isPresent()) {
            ApplicationCommandInteractionOption options = event.getOption(ACTION_EXPRESSION).get();
            String diceExpression = options.getValue()
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow();
            String validationMessage = diceParserHelper.validateDiceExpressions(ImmutableList.of(diceExpression), "/custom_dice help");
            if (validationMessage != null) {
                log.info("Validation message: {}", validationMessage);
                return event.reply(validationMessage);
            }
            Metrics.incrementSlashStartMetricCounter(getName(), ImmutableList.of(diceExpression));

            List<DiceResult> results = diceParserHelper.roll(diceExpression);
            results.forEach(d -> log.info(String.format("%s:%s -> %s: %s", getName(), diceExpression, d.getResultTitle(), d.getResultDetails())));

            return event.reply("...")
                    .then(event.createResultMessageWithEventReference(results));

        }

        return Mono.empty();
    }
}
