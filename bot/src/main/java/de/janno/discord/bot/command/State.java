package de.janno.discord.bot.command;

import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

@Value
public class State<T extends EmptyData> {

    @NonNull
    String buttonValue;
    @Nullable
    T data;

    public String toShortString() {
        String persistedStateValues = data == null ? "" : String.format(",%s", data.getShortStringValues());
        return String.format("[%s%s]", getButtonValue(), persistedStateValues);
    }
}
