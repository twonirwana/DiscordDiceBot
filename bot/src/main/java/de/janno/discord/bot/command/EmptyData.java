package de.janno.discord.bot.command;

import lombok.EqualsAndHashCode;

import java.io.Serializable;


@EqualsAndHashCode
public class EmptyData implements Serializable {

    public String getShortStringValues() {
        return "";
    }

    public String toShortString() {
        return "[]";
    }
}
