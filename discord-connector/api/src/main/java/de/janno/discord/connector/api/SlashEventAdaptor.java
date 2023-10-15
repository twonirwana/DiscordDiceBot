package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface SlashEventAdaptor extends DiscordAdapter {
    Optional<String> checkPermissions();

    Optional<CommandInteractionOption> getOption(@NonNull String optionName);

    List<CommandInteractionOption> getOptions();

    Mono<Void> replyWithEmbedOrMessageDefinition(@NonNull EmbedOrMessageDefinition messageDefinition, boolean ephemeral);

    @NonNull Mono<Long> createButtonMessage(@NonNull MessageDefinition messageDefinition);

    long getChannelId();

    String getCommandString();

    Requester getRequester();

    Mono<Void> createResultMessageWithEventReference(EmbedOrMessageDefinition answer);

    boolean isValidAnswerChannel(long channelId);

    Mono<Void> acknowledgeAndRemoveSlash();

    long getUserId();
}
