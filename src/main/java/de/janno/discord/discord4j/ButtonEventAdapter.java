package de.janno.discord.discord4j;

import com.google.common.collect.ImmutableList;
import de.janno.discord.cache.ActiveButtonsCache;
import de.janno.discord.command.IButtonEventAdaptor;
import de.janno.discord.dice.DiceResult;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ButtonEventAdapter extends DiscordAdapter implements IButtonEventAdaptor {
    private final ComponentInteractionEvent event;

    public ButtonEventAdapter(ComponentInteractionEvent event) {
        this.event = event;
    }

    @Override
    public String getCustomId() {
        return event.getCustomId();
    }

    @Override
    public Snowflake getMessageId() {
        return event.getMessageId();
    }

    @Override
    public Snowflake getChannelId() {
        return event.getInteraction().getChannelId();
    }

    @Override
    public boolean isPinned() {
        return event.getMessage().map(Message::isPinned).orElse(false);
    }

    @Override
    public Mono<Void> editMessage(String message) {
        return event.edit(message)
                .onErrorResume(t -> {
                    log.warn("Error on acknowledge button event");
                    return Mono.empty();
                });
    }

    //TODO split
    @Override
    public Mono<Void> moveButtonMessage(boolean triggeringMessageIsPinned,
                                        String editMessageText,
                                        String buttonMessageText,
                                        ActiveButtonsCache activeButtonsCache,
                                        List<LayoutComponent> buttonLayout,
                                        int configHash) {
        return event.getInteraction().getChannel()
                .ofType(TextChannel.class)
                .flatMap(channel ->
                {
                    //if the triggering message is pinned and its content is not changed, then the new message should have a modified message content
                    String messageContent = triggeringMessageIsPinned ? editMessageText : buttonMessageText;
                    return createButtonMessage(activeButtonsCache, channel, messageContent, buttonLayout, configHash)
                            .onErrorResume(t -> {
                                log.warn("Error on creating button message");
                                return Mono.empty();
                            });
                })
                .flatMap(m -> {
                    if (triggeringMessageIsPinned) {
                        //removing from cache on pin event would be better but currently not possible with discord4j
                        //if the message was not removed, we don't want that it is removed later
                        activeButtonsCache.removeButtonFromChannel(event.getInteraction().getChannelId(), event.getMessageId(), configHash);
                    }
                    return deleteMessage(m.getChannel(), m.getChannelId(), activeButtonsCache, m.getId(), configHash);
                });
    }

    @Override
    public Mono<Void> createResultMessageWithEventReference(List<DiceResult> diceResults) {
        return event.getInteraction().getChannel().ofType(TextChannel.class)
                .flatMap(channel -> channel.createMessage(createEmbedMessageWithReference(diceResults, event.getInteraction().getMember().orElseThrow())))
                .onErrorResume(t -> {
                    log.error("Error on creating dice result message", t);
                    return Mono.empty();
                })
                .ofType(Void.class);
    }

    @Override
    public List<String> getAllButtonIds() {
        return event.getInteraction().getMessage()
                .map(s -> s.getComponents().stream()
                        .flatMap(lc -> lc.getChildren().stream())
                        .map(l -> l.getData().customId())
                        .map(c -> c.toOptional().orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                )
                .orElse(ImmutableList.of());
    }

    @Override
    public String getMessageContent() {
        return event.getMessage().map(Message::getContent).orElse("");
    }
}
