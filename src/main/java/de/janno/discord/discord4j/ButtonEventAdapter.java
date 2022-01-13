package de.janno.discord.discord4j;

import com.google.common.collect.ImmutableList;
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
import java.util.stream.Collectors;

@Slf4j
public class ButtonEventAdapter extends DiscordAdapter implements IButtonEventAdaptor {
    private final ComponentInteractionEvent event;
    private final String customId;
    private final Long messageId;
    private final Long channelId;
    private final boolean isPinned;
    private final String messageContent;
    private final List<LabelAndCustomId> allButtonIds;

    public ButtonEventAdapter(ComponentInteractionEvent event) {
        this.event = event;
        messageId = event.getMessageId().asLong();
        customId = event.getCustomId();
        isPinned = event.getMessage().map(Message::isPinned).orElse(false);
        channelId = event.getInteraction().getChannelId().asLong();
        messageContent = event.getMessage().map(Message::getContent).orElse("");
        allButtonIds = event.getInteraction().getMessage()
                .map(s -> s.getComponents().stream()
                        .flatMap(lc -> lc.getChildren().stream())
                        .map(l -> {
                            if (l.getData().label().isAbsent() || l.getData().customId().isAbsent()) {
                                return null;
                            }
                            return new LabelAndCustomId(l.getData().label().get(), l.getData().customId().get());
                        }).collect(Collectors.toList())
                )
                .orElse(ImmutableList.of());
    }

    @Override
    public String getCustomId() {
        return customId;
    }

    @Override
    public Long getMessageId() {
        return messageId;
    }

    @Override
    public Long getChannelId() {
        return channelId;
    }

    @Override
    public boolean isPinned() {
        return isPinned;
    }

    @Override
    public List<LabelAndCustomId> getAllButtonIds() {
        return allButtonIds;
    }

    @Override
    public String getMessageContent() {
        return messageContent;
    }

    @Override
    public Mono<Void> editMessage(String message) {
        return event.edit(message)
                .onErrorResume(t -> {
                    log.warn("Error on edit button event");
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Long> createButtonMessage(String messageContent, List<LayoutComponent> buttonLayout) {
        return event.getInteraction().getChannel()
                .ofType(TextChannel.class)
                .flatMap(channel -> createButtonMessage(channel, messageContent, buttonLayout)
                        .onErrorResume(t -> {
                            log.warn("Error on creating button message");
                            return Mono.empty();
                        }))
                .map(m -> m.getId().asLong());
    }

    @Override
    public Mono<Void> deleteMessage(long messageId) {
        return event.getInteraction().getChannel()
                .flatMap(c -> c.getMessageById(Snowflake.of(messageId)))
                .filter(m -> !m.isPinned())
                .flatMap(Message::delete)
                .onErrorResume(t -> {
                    log.warn("Error on deleting message");
                    return Mono.empty();
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


}
