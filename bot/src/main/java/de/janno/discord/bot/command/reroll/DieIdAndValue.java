package de.janno.discord.bot.command.reroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.DieIdDb;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.io.Serializable;

@EqualsAndHashCode
@Getter
@ToString
public class DieIdAndValue implements Serializable {
    @NonNull
    private final DieIdDb dieIdDb;
    @NonNull
    private final String value;

    @JsonCreator
    public DieIdAndValue(@JsonProperty("dieIdDb") @NonNull DieIdDb dieIdDb,
                         @JsonProperty("value") @NonNull String value) {
        this.dieIdDb = dieIdDb;
        this.value = value;
    }
}