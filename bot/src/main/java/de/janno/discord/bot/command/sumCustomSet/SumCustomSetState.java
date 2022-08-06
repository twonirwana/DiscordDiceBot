package de.janno.discord.bot.command.sumCustomSet;

import de.janno.discord.bot.command.State;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode(callSuper = true)
@Getter
public class SumCustomSetState extends State {
    @NonNull
    final String diceExpression;
    final String lockedForUserName;

    public SumCustomSetState(@NonNull String buttonValue, @NonNull String diceExpression, String lockedForUserName) {
        super(buttonValue);
        this.diceExpression = diceExpression;
        this.lockedForUserName = lockedForUserName;
    }

    @Override
    public String toShortString() {
        return String.format("[%s, %s, %s]", getButtonValue(), diceExpression, lockedForUserName);
    }
}
