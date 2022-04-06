package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface ISlashEventAdaptor extends IDiscordAdapter {
    String checkPermissions();

    Optional<CommandInteractionOption> getOption(@NonNull String actionStart);

    Mono<Void> reply(@NonNull String message);

    Mono<Void> replyEmbed(@NonNull EmbedDefinition embedDefinition, boolean ephemeral);

    Mono<Long> createButtonMessage(@NonNull MessageDefinition messageDefinition);

    long getChannelId();

    String getCommandString();

    Mono<Void> deleteMessage(long messageId);

    Mono<Requester> getRequester();
}
