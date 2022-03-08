package de.janno.discord.api;

import de.janno.discord.command.slash.CommandInteractionOption;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface ISlashEventAdaptor extends IDiscordAdapter {
    String checkPermissions();

    Optional<CommandInteractionOption> getOption(String actionStart);

    Mono<Void> reply(String message);

    Mono<Void> replyEphemeral(EmbedDefinition embedDefinition);

    Mono<Long> createButtonMessage(@NonNull String buttonMessage, @NonNull List<ComponentRowDefinition> buttons);

    Long getChannelId();

    String getCommandString();

    Mono<Void> deleteMessage(long messageId);

    Mono<Requester> getRequester();
}
