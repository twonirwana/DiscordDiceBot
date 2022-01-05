package de.janno.discord.command;

import de.janno.discord.discord4j.ApplicationCommand;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface ISlashCommand {

    String getName();

    ApplicationCommand getApplicationCommand();

    Mono<Void> handleSlashCommandEvent(@NonNull ISlashEventAdaptor event);

}
