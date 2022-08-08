package de.janno.discord.bot.command;

import java.io.Serializable;

public interface StateData extends Serializable {

    String getShortStringValues();

    default String toShortString() {
        return "[]";
    }
}
