package de.janno.discord.bot.command.namedCommand;

import de.janno.discord.bot.command.Config;
import lombok.NonNull;

public record NamedConfig(@NonNull String name, @NonNull String commandId, @NonNull String configClassId,
                          @NonNull Config config) {
}