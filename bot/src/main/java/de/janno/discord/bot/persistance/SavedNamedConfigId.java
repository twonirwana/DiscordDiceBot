package de.janno.discord.bot.persistance;

import java.util.UUID;

public record SavedNamedConfigId(UUID id, String commandId, String name) {
}
