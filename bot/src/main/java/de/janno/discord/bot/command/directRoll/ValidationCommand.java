package de.janno.discord.bot.command.directRoll;

import com.google.common.base.Strings;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class ValidationCommand extends DirectRollCommand {

    public static final String ROLL_COMMAND_ID = "validation";
    private final DiceEvaluatorAdapter diceEvaluatorAdapter;

    public ValidationCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        super(persistenceManager, cachingDiceEvaluator, false);
        this.diceEvaluatorAdapter = new DiceEvaluatorAdapter(cachingDiceEvaluator);
    }

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(AutoCompleteRequest option) {
        if (!ACTION_EXPRESSION.equals(option.getFocusedOptionName())) {
            return List.of();
        }
        if (Strings.isNullOrEmpty(option.getFocusedOptionValue())) {
            return List.of(new AutoCompleteAnswer("2d6=", "2d6="));
        }
        Optional<String> validation = diceEvaluatorAdapter.shortValidateDiceExpressionWitOptionalLabel(option.getFocusedOptionValue());
        BotMetrics.incrementValidationCounter(validation.isEmpty());
        return validation
                .map(s -> List.of(new AutoCompleteAnswer(s, option.getFocusedOptionValue())))
                .orElse(List.of(new AutoCompleteAnswer(option.getFocusedOptionValue(), option.getFocusedOptionValue())));
    }

    @Override
    public @NonNull String getCommandId() {
        return ROLL_COMMAND_ID;
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .description("provide a expression and the autocomplete will show a error message if invalid")
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_EXPRESSION)
                        .required(true)
                        .autoComplete(true)
                        .description("provide a expression (e.g. 2d6) and the autocomplete will show if it is invalid")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build();
    }
}