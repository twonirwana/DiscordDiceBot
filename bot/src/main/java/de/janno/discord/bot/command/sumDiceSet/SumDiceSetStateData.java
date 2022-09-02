package de.janno.discord.bot.command.sumDiceSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.StateData;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = true)
public class SumDiceSetStateData extends StateData {

    @NonNull
    List<DiceKeyAndValue> diceSet;

    @JsonCreator
    public SumDiceSetStateData(@NonNull @JsonProperty("diceSet") List<DiceKeyAndValue> diceSet) {
        this.diceSet = diceSet;
    }

    @Override
    @JsonIgnore
    public String getShortStringValues() {
        return "[" + diceSet.stream().map(DiceKeyAndValue::getShortStringValues).collect(Collectors.joining(", ")) + "]";
    }

}
