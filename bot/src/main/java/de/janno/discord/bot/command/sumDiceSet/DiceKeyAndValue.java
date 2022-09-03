package de.janno.discord.bot.command.sumDiceSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

@Value
public class DiceKeyAndValue {
    @NonNull
    String diceKey;

    @NonNull
    Integer value;

    @JsonCreator
    public DiceKeyAndValue(@NonNull @JsonProperty("diceKey") String diceKey, @NonNull @JsonProperty("value") Integer value) {
        this.diceKey = diceKey;
        this.value = value;
    }

    @JsonIgnore
    public String getShortStringValues() {
        return String.format("%s:%s", diceKey, value);
    }
}
