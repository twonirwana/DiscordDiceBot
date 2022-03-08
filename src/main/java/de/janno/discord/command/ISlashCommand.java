package de.janno.discord.command;

import de.janno.discord.api.ISlashEventAdaptor;
import de.janno.discord.command.slash.CommandDefinition;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface ISlashCommand {

    String getName();

    CommandDefinition getCommandDefinition();

    Mono<Void> handleSlashCommandEvent(@NonNull ISlashEventAdaptor event);

}
