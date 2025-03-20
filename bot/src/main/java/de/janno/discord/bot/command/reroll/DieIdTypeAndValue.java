package de.janno.discord.bot.command.reroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.DieIdDb;
import de.janno.evaluator.dice.DieIdAndValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;


import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode
@Getter
@ToString
public class DieIdTypeAndValue implements Serializable {
    @NonNull
    private final DieIdDb dieIdDb;
    private final int numberSupplierValue;
    @NonNull
    private final String value;
    
    private final Integer diceSides;
    
    private final List<String> selectedFrom;

    @JsonCreator
    public DieIdTypeAndValue(@JsonProperty("dieIdDb") @NonNull DieIdDb dieIdDb,
                             @JsonProperty("value") @NonNull String value,
                             @JsonProperty("numberSupplierValue") int numberSupplierValue,
                             @JsonProperty("diceSides") Integer diceSides,
                             @JsonProperty("selectedFrom") List<String> selectedFrom) {
        this.dieIdDb = dieIdDb;
        this.value = value;
        this.numberSupplierValue = numberSupplierValue;
        this.diceSides = diceSides;
        this.selectedFrom = selectedFrom;
        if (diceSides == null && selectedFrom == null) {
            throw new IllegalArgumentException("DiceSides and SelectedFrom are both null");
        }
    }

    @JsonIgnore
    public DieIdAndValue toDieIdAndValue() {
        return DieIdAndValue.of(dieIdDb.toDieId(), numberSupplierValue);
    }
}