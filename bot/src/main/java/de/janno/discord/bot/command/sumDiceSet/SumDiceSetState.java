package de.janno.discord.bot.command.sumDiceSet;

import de.janno.discord.bot.command.State;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Getter
public class SumDiceSetState extends State {

    @NonNull
    private final Map<String, Integer> diceSetMap;

    public SumDiceSetState(@NonNull String buttonValue, @NonNull Map<String, Integer> diceSetMap) {
        super(buttonValue);
        this.diceSetMap = diceSetMap;
    }

    @Override
    public String toShortString() {
        return String.format("[%s, %s]", getButtonValue(), diceSetMap);
    }
}
