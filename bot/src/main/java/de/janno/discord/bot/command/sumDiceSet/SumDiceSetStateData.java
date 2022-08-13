package de.janno.discord.bot.command.sumDiceSet;

import de.janno.discord.bot.command.EmptyData;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = true)
public class SumDiceSetStateData extends EmptyData {

    @NonNull
    Map<String, Integer> diceSetMap;

    @Override
    public String getShortStringValues() {
        return String.format("%s", diceSetMap);
    }

}
