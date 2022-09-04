package de.janno.discord.bot.command;

import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Value
public class State<T extends StateData> {

    @NonNull
    String buttonValue;
    @Nullable
    T data;

    public String toShortString() {
        String persistedStateValues = Optional.ofNullable(data)
                .map(d -> String.format(",%s", data.getShortStringValues()))
                .orElse("");
        return String.format("[%s]", persistedStateValues);
    }
}
