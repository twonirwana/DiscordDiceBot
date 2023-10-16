package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
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

    //todo combiend with ButtonEventAdaptor.createResultMessageWithReference?
    Mono<Void> createResultMessageWithEventReference(EmbedOrMessageDefinition answer);

    long getChannelId();

    String getCommandString();

    Requester getRequester();

    boolean isValidAnswerChannel(long channelId);

    Mono<Void> acknowledgeAndRemoveSlash();

    long getUserId();
}
