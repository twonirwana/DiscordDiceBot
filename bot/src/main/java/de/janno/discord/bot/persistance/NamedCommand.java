package de.janno.discord.bot.persistance;

import java.util.UUID;

public record NamedCommand(UUID id, String commandId, String name) {
}
