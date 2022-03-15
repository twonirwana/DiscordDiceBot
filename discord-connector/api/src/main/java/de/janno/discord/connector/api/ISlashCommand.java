package de.janno.discord.connector.api;

import de.janno.discord.connector.api.slash.CommandDefinition;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface ISlashCommand {

    String getName();

    CommandDefinition getCommandDefinition();

    Mono<Void> handleSlashCommandEvent(@NonNull ISlashEventAdaptor event);

}
