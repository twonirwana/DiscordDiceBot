package de.janno.discord.connector.api;

import de.janno.discord.connector.api.slash.CommandDefinition;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Supplier;

public interface SlashCommand {

    String getCommandId();

    CommandDefinition getCommandDefinition();

    Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier);

}
