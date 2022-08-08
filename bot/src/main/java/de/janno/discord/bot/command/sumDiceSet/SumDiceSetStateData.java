package de.janno.discord.bot.command.sumDiceSet;

import de.janno.discord.bot.command.StateData;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;

@Value
public class SumDiceSetStateData implements StateData {

    @NonNull
    Map<String, Integer> diceSetMap;

    @Override
    public String getShortStringValues() {
        return String.format("%s", diceSetMap);
    }

}
