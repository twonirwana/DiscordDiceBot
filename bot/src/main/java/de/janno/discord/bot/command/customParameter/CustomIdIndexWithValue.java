package de.janno.discord.bot.command.customParameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;

@Value
public class CustomIdIndexWithValue {
    @NonNull
    CustomIdIndex customIdIndex;
    @NonNull
    String value;

    @JsonCreator
    public CustomIdIndexWithValue(@JsonProperty("customIdIndex") @NonNull CustomIdIndex customIdIndex,
                                  @NonNull @JsonProperty("value") String value) {
        this.customIdIndex = customIdIndex;
        this.value = value;
    }

    public void addToArray(String[] array) {
        array[customIdIndex.index] = value;
    }
}
