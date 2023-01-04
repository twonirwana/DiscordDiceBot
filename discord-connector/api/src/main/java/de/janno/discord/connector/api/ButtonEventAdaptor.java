package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ButtonEventAdaptor extends DiscordAdapter {

    String getCustomId();

    long getMessageId();

    long getChannelId();

    boolean isPinned();

    String getInvokingGuildMemberName();

    Mono<Void> editMessage(@Nullable String message, @Nullable List<ComponentRowDefinition> componentRowDefinitions);

    Requester getRequester();

    Optional<String> checkPermissions(Long answerTargetChannelId);

    Mono<Void> createResultMessageWithEventReference(EmbedOrMessageDefinition answer, Long targetChannelId);

    @NonNull ParallelFlux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds);

    @NonNull OffsetDateTime getMessageCreationTime();

}
