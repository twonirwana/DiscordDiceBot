package de.janno.discord.bot.command.sumCustomSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.EmptyData;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class SumCustomSetStateData extends EmptyData {

    @NonNull
    List<String> diceExpressions;
    String lockedForUserName;

    @JsonCreator
    public SumCustomSetStateData(@JsonProperty("diceExpressions") @NonNull List<String> diceExpressions,
                                 @JsonProperty("lockedForUserName") String lockedForUserName) {
        this.diceExpressions = diceExpressions;
        this.lockedForUserName = lockedForUserName;
    }

    @Override
    @JsonIgnore
    public String getShortStringValues() {
        return String.format("%s, %s", diceExpressions, lockedForUserName);
    }
}
