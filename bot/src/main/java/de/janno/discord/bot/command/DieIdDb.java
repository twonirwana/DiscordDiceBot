package de.janno.discord.bot.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.evaluator.dice.DieId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.io.Serializable;

@EqualsAndHashCode
@ToString
@Getter
public class DieIdDb implements Serializable {
    private final int expressionPositionStartInc;
    @NonNull
    private final String value;
    private final int reEvaluateCounter;
    private final int dieIndex;
    private final int reroll;

    @JsonCreator
    public DieIdDb(@JsonProperty("expressionPositionStartInc") int expressionPositionStartInc,
                   @JsonProperty("value") @NonNull String value,
                   @JsonProperty("reEvaluateCounter") int reEvaluateCounter,
                   @JsonProperty("dieIndex") int dieIndex,
                   @JsonProperty("reroll") int reroll
    ) {
        this.expressionPositionStartInc = expressionPositionStartInc;
        this.value = value;
        this.reEvaluateCounter = reEvaluateCounter;
        this.dieIndex = dieIndex;
        this.reroll = reroll;
    }

    @JsonIgnore
    public static DieIdDb fromDieId(DieId dieId) {
        return new DieIdDb(
                dieId.getRollId().getExpressionPosition().getStartInc(),
                dieId.getRollId().getExpressionPosition().getValue(),
                dieId.getRollId().getReevaluate(),
                dieId.getDieIndex(),
                dieId.getReroll()
        );
    }

    @JsonIgnore
    public DieId toDieId() {
        return DieId.of(expressionPositionStartInc, value, reEvaluateCounter, dieIndex, reroll);
    }
}
