package de.janno.discord.bot.command.directRoll;

import de.janno.discord.connector.api.SlashCommand;

public abstract class AbstractDirectRollCommand implements SlashCommand {
    protected static final String CONFIG_TYPE_ID = "DirectRollConfig";
    protected static final String ROLL_COMMAND_ID = "r";
}
