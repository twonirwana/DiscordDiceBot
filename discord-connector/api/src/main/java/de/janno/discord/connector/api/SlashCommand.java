package de.janno.discord.connector.api;

import de.janno.discord.connector.api.slash.CommandDefinition;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface SlashCommand {

    String getCommandId();

    CommandDefinition getCommandDefinition();

    Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event);

}
