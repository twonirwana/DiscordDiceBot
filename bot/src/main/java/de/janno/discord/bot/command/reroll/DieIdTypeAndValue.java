package de.janno.discord.bot.command.reroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.DieIdDb;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode
@Getter
@ToString
public class DieIdTypeAndValue implements Serializable {
    @NonNull
    private final DieIdDb dieIdDb;
    @NonNull
    private final String value;
    /**
     * If the die is a custom die we need the value index for the number supplier
     */
    @Nullable
    private final Integer customDieIndex;
    /**
     * If the die is a normal die, this is its rolled numeric value
     */
    @Nullable
    private final Integer numericDiceValue;

    @Nullable
    private final Integer diceSides;

    @Nullable
    private final List<String> selectedFrom;

    @JsonCreator
    public DieIdTypeAndValue(@JsonProperty("dieIdDb") @NonNull DieIdDb dieIdDb,
                             @JsonProperty("value") @NonNull String value,
                             @JsonProperty("customDieIndex") @Nullable Integer customDieIndex,
                             @JsonProperty("diceSides") @Nullable Integer diceSides,
                             @JsonProperty("selectedFrom") @Nullable List<String> selectedFrom) {
        this.dieIdDb = dieIdDb;
        this.value = value;
        this.customDieIndex = customDieIndex;
        if (customDieIndex == null) {
            numericDiceValue = Integer.parseInt(value);
        } else {
            numericDiceValue = null;
        }
        this.diceSides = diceSides;
        this.selectedFrom = selectedFrom;
        if (diceSides == null && selectedFrom == null) {
            throw new IllegalArgumentException("DiceSides and SelectedFrom are both null");
        }
    }

    @JsonIgnore
    public int getDiceNumberOrCustomDieSideIndex() {
        if (customDieIndex != null) {
            return customDieIndex;
        }
        if (numericDiceValue != null) {
            return numericDiceValue;
        }
        throw new IllegalStateException("Invalid state: " + this);
    }
}