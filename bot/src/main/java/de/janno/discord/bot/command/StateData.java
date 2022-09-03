package de.janno.discord.bot.command;

import lombok.EqualsAndHashCode;

import java.io.Serializable;


@EqualsAndHashCode
public class StateData implements Serializable {

    private static final StateData EMPTY = StateData.empty();

    protected StateData() {
    }

    public static StateData empty() {
        return EMPTY;
    }

    public String getShortStringValues() {
        return "";
    }

    public String toShortString() {
        return "[]";
    }
}
