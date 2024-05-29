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
import java.util.Optional;

@EqualsAndHashCode
@Getter
@ToString
public class DieIdAndValue implements Serializable {
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

    @JsonCreator
    public DieIdAndValue(@JsonProperty("dieIdDb") @NonNull DieIdDb dieIdDb,
                         @JsonProperty("value") @NonNull String value,
                         @JsonProperty("customDieIndex") @Nullable Integer customDieIndex) {
        this.dieIdDb = dieIdDb;
        this.value = value;
        this.customDieIndex = customDieIndex;
        if (customDieIndex == null) {
            numericDiceValue = Integer.parseInt(value);
        } else {
            numericDiceValue = null;
        }

    }

    @JsonIgnore
    public int getDiceNumberOrCustomDieSideIndex() {
        return Optional.ofNullable(customDieIndex)
                .orElse(numericDiceValue);
    }
}