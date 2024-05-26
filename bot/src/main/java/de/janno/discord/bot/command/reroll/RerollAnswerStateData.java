package de.janno.discord.bot.command.reroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.DieIdDb;
import de.janno.discord.bot.command.StateData;
import de.janno.evaluator.dice.DieId;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class RerollAnswerStateData extends StateData {

    @NonNull
    List<DieIdDb> rerollDice;
    String lockedForUserName;

    @JsonCreator
    public RerollAnswerStateData(@JsonProperty("rerollDice") @NonNull List<DieIdDb> rerollDice,
                                 @JsonProperty("lockedForUserName") String lockedForUserName) {
        this.rerollDice = rerollDice;
        this.lockedForUserName = lockedForUserName;
    }

    @Override
    @JsonIgnore
    public String getShortStringValues() {
        //todo
        return "";
    }

}
