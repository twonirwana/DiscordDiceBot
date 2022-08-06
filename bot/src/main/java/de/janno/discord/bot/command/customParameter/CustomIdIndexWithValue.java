package de.janno.discord.bot.command.customParameter;

import lombok.NonNull;
import lombok.Value;

@Value
public class CustomIdIndexWithValue {
    @NonNull
    CustomIdIndex customIdIndex;
    @NonNull
    String value;

    public void addToArray(String[] array) {
        array[customIdIndex.index] = value;
    }
}
