package de.janno.discord.connector.javacord;

import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.Answer;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.event.interaction.ButtonClickEvent;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ButtonEventAdapter extends DiscordAdapter implements IButtonEventAdaptor {
    @NonNull
    private final ButtonClickEvent event;
    @NonNull
    private final String customId;
    private final long messageId;
    private final long channelId;
    private final boolean isPinned;
    @NonNull
    private final String messageContent;
    @NonNull
    private final List<LabelAndCustomId> allButtonIds;
    @NonNull
    private final Mono<Requester> requesterMono;
    @NonNull
    private final String invokingGuildMemberName;

    public ButtonEventAdapter(@NonNull ButtonClickEvent event,
                              @NonNull Mono<Requester> requesterMono) {
        this.event = event;
        this.requesterMono = requesterMono;
        this.messageId = event.getButtonInteraction().getMessage().getId();
        this.customId = event.getButtonInteraction().getCustomId();
        this.isPinned = event.getButtonInteraction().getMessage().isPinned();
        this.channelId = event.getButtonInteraction().getChannel().map(DiscordEntity::getId).orElseThrow();
        this.messageContent = event.getButtonInteraction().getMessage().getContent();
        this.allButtonIds = event.getButtonInteraction().getMessage()
                .getComponents().stream()
                .flatMap(s -> s.asActionRow().map(ActionRow::getComponents).orElse(ImmutableList.of()).stream()
                        .flatMap(lc -> lc.asButton().stream())
                        .flatMap(l -> {
                            if (l.getLabel().isEmpty() || l.getCustomId().isEmpty()) {
                                return Stream.empty();
                            }
                            return Stream.of(new LabelAndCustomId(l.getLabel().get(), l.getCustomId().get()));
                        })
                ).collect(Collectors.toList());
        this.invokingGuildMemberName = event.getButtonInteraction().getServer().map(s -> event.getButtonInteraction().getUser().getDisplayName(s)).orElse("");
    }

    @Override
    public @NonNull String getCustomId() {
        return customId;
    }

    @Override
    public long getMessageId() {
        return messageId;
    }

    @Override
    public long getChannelId() {
        return channelId;
    }

    @Override
    public boolean isPinned() {
        return isPinned;
    }

    @Override
    public @NonNull List<LabelAndCustomId> getAllButtonIds() {
        return allButtonIds;
    }

    @Override
    public @NonNull String getMessageContent() {
        return messageContent;
    }

    @Override
    public Mono<Void> acknowledge() {
        return Mono.fromFuture(event.getButtonInteraction().acknowledge());
    }

    @Override
    public Mono<Void> editMessage(String message) {
        return Mono.fromFuture(event.getButtonInteraction().getMessage().edit(message))
                .then()
                .onErrorResume(t -> handleException("Error on edit button event", t, false));
    }

    @Override
    protected Mono<Void> answerOnError(String message) {
        return Mono.fromFuture(event.getButtonInteraction().getMessage().edit(message)).then();
    }

    @Override
    public Mono<Long> createButtonMessage(String messageContent, List<ComponentRowDefinition> buttonLayout) {
        return createButtonMessage(event.getButtonInteraction().getChannel().orElseThrow(), messageContent, buttonLayout)
                .onErrorResume(t -> handleException("Error on creating button message", t, false).ofType(Message.class))
                .map(DiscordEntity::getId);
    }

    @Override
    public Mono<Void> deleteMessage(long messageId) {
        return deleteMessage(event.getInteraction().getChannel().orElseThrow(), messageId);
    }

    @Override
    public Mono<Void> createResultMessageWithEventReference(Answer answer) {
        return createEmbedMessageWithReference(event.getInteraction().getChannel().orElseThrow(),
                answer, event.getInteraction().getUser(),
                event.getInteraction().getServer().orElseThrow())
                .onErrorResume(t -> handleException("Error on creating answer message", t, false).ofType(Message.class))
                .ofType(Void.class);
    }

    @Override
    public Mono<Requester> getRequester() {
        return requesterMono;
    }

    @Override
    public @NonNull String getInvokingGuildMemberName() {
        return invokingGuildMemberName;
    }
}
