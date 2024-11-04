package de.janno.discord.bot.command;

import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nullable;

import java.util.Optional;

@Value
public class State<T extends StateData> {

    @NonNull
    String buttonValue;

    @Nullable
    T data;

    public String toShortString() {
        String persistedStateValues = Optional.ofNullable(data)
                .map(d -> data.getShortStringValues())
                .orElse("");
        return String.format("%s [%s]", buttonValue, persistedStateValues).replace("\n", " ");
    }
}
