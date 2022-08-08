package de.janno.discord.bot.command;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class StateWithData<T extends StateData> extends State  {

    @NonNull
    T data;

    public StateWithData(@NonNull String buttonValue, @NonNull T data) {
        super(buttonValue);
        this.data = data;
    }

    public String toShortString() {
        String persistedStateValues = String.format(",%s", data.getShortStringValues());
        return String.format("[%s%s]", getButtonValue(), persistedStateValues);
    }
}
