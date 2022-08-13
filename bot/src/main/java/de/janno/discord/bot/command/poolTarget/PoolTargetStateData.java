package de.janno.discord.bot.command.poolTarget;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.EmptyData;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class PoolTargetStateData extends EmptyData {

    Integer dicePool;
    Integer targetNumber;
    Boolean doReroll;

    @JsonCreator
    public PoolTargetStateData(
            @JsonProperty("dicePool") Integer dicePool,
            @JsonProperty("targetNumber") Integer targetNumber,
            @JsonProperty("doReroll") Boolean doReroll) {
        this.dicePool = dicePool;
        this.targetNumber = targetNumber;
        this.doReroll = doReroll;
    }

    @Override
    @JsonIgnore
    public String getShortStringValues() {
        return String.format("%s, %s, %s", dicePool, targetNumber, doReroll);
    }
}
