package de.janno.discord.bot.command.namedCommand;


import lombok.NonNull;

import java.util.UUID;

public record SavedNamedConfig(@NonNull UUID configUUID, @NonNull NamedConfig namedConfig) {

}