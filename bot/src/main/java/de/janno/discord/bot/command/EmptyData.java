package de.janno.discord.bot.command;

import java.io.Serializable;


public class EmptyData implements Serializable {

    public String getShortStringValues() {
        return "";
    }

    public String toShortString() {
        return "[]";
    }
}
