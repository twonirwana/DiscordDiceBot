package de.janno.discord.bot.command;

import java.io.Serializable;

public interface Config extends Serializable {
    String toCommandOptionsString();
}
