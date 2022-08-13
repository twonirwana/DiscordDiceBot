package de.janno.discord.bot.command.sumCustomSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.EmptyData;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class SumCustomSetStateData extends EmptyData {

    @NonNull
    String diceExpression;
    String lockedForUserName;

    @JsonCreator
    public SumCustomSetStateData(@JsonProperty("diceExpression") @NonNull String diceExpression,
                                 @JsonProperty("lockedForUserName") String lockedForUserName) {
        this.diceExpression = diceExpression;
        this.lockedForUserName = lockedForUserName;
    }

    @Override
    @JsonIgnore
    public String getShortStringValues() {
        return String.format("%s, %s", diceExpression, lockedForUserName);
    }
}
