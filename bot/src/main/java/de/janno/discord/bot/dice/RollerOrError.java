package de.janno.discord.bot.dice;

import de.janno.evaluator.dice.Roller;
import lombok.NonNull;
import lombok.Value;

@Value
public class RollerOrError {
    @NonNull
    String expression;
    Roller roller;
    boolean valid;
    String errorMessage;
}
