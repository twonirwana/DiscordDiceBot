package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface SlashEventAdaptor extends DiscordAdapter {
    Optional<String> checkPermissions();

    Optional<CommandInteractionOption> getOption(@NonNull String actionStart);

    Mono<Void> reply(@NonNull String message);

    Mono<Void> replyEmbed(@NonNull EmbedDefinition embedDefinition, boolean ephemeral);

    Mono<Long> createButtonMessage(@NonNull MessageDefinition messageDefinition);

    long getChannelId();

    String getCommandString();

    Mono<Requester> getRequester();

    Mono<Void> createResultMessageWithEventReference(EmbedDefinition answer);

    boolean isValidAnswerChannel(long channelId);

}
