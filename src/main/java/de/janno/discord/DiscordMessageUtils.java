package de.janno.discord;

import com.google.common.collect.ImmutableSet;
import de.janno.discord.command.ActiveButtonsCache;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class DiscordMessageUtils {
    //needed to correctly show utf8 characters in discord
    public static String encodeUTF8(@NonNull String in) {
        return new String(in.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    public static Mono<Message> createEmbedMessageWithReference(
            @NonNull TextChannel channel,
            @NonNull String title,
            @NonNull String description,
            @NonNull Member rollRequester) {
        return channel.createMessage(MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title(encodeUTF8(title))
                        .author(rollRequester.getDisplayName(), null, rollRequester.getAvatarUrl())
                        .color(Color.of(rollRequester.getId().hashCode()))
                        .description(encodeUTF8(description))
                        .timestamp(Instant.now())
                        .build())
                .build());
    }

    public static Flux<Void> deleteAllButtonMessagesOfTheBot(@NonNull Mono<TextChannel> channel,
                                                             @NonNull Snowflake deleteBeforeMessageId,
                                                             @NonNull Snowflake botUserId,
                                                             @NonNull Function<String, Boolean> isFromSystem) {
        return channel.flux()
                .flatMap(c -> c.getMessagesBefore(deleteBeforeMessageId))
                .take(500) //only look at the last 500 messages
                .filter(m -> botUserId.equals(m.getAuthor().map(User::getId).orElse(null)))
                .filter(m -> m.getComponents().stream()
                        .flatMap(l -> buttonIds(l).stream())
                        .anyMatch(isFromSystem::apply))
                .flatMap(Message::delete);
    }

    public static Mono<Void> deleteMessage(
            @NonNull Mono<MessageChannel> channel,
            @NonNull Snowflake channelId,
            @NonNull ActiveButtonsCache activeButtonsCache,
            @NonNull Snowflake toKeep) {
        return channel
                .flux()
                .flatMap(c -> {
                    List<Snowflake> allButtonsWithoutTheLast = activeButtonsCache.getAllWithoutOneAndRemoveThem(channelId, toKeep);
                    return Flux.fromIterable(allButtonsWithoutTheLast).flatMap(c::getMessageById);
                })
                .onErrorResume(e -> {
                    log.info("Button was not found");
                    return Mono.empty();
                })
                .flatMap(Message::delete).then();
    }

    public static Function<TextChannel, Mono<Message>> createButtonMessage(@NonNull ActiveButtonsCache activeButtonsCache,
                                                                           @NonNull String buttonMessage,
                                                                           @NonNull List<LayoutComponent> buttons) {
        return channel -> channel.createMessage(msg -> {
            msg.setContent(buttonMessage);
            msg.setComponents(buttons);
        }).map(m -> {
            activeButtonsCache.addChannelWithButton(m.getChannelId(), m.getId());
            return m;
        });
    }

    private static Set<String> buttonIds(MessageComponent messageComponent) {
        if (messageComponent instanceof LayoutComponent) {
            LayoutComponent layoutComponent = (LayoutComponent) messageComponent;
            if (!layoutComponent.getChildren().isEmpty()) {
                return layoutComponent.getChildren().stream().flatMap(mc -> buttonIds(mc).stream()).collect(Collectors.toSet());
            }
        }
        return messageComponent.getData().customId().toOptional().map(ImmutableSet::of).orElse(ImmutableSet.of());
    }


}
