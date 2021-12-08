package de.janno.discord;

import de.janno.discord.command.ActiveButtonsCache;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class DiscordUtils {

    //needed to correctly show utf8 characters in discord
    public static String encodeUTF8(@NonNull String in) {
        return new String(in.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    public static EmbedCreateSpec createEmbedMessageWithReference(
            @NonNull String title,
            @NonNull String description,
            @NonNull Member rollRequester) {
        return EmbedCreateSpec.builder()
                .title(StringUtils.abbreviate(encodeUTF8(title), 256)) //https://discord.com/developers/docs/resources/channel#embed-limits
                .author(rollRequester.getDisplayName(), null, rollRequester.getAvatarUrl())
                .color(Color.of(rollRequester.getId().hashCode()))
                .description(StringUtils.abbreviate(encodeUTF8(description), 4096)) //https://discord.com/developers/docs/resources/channel#embed-limits
                //   .timestamp(Instant.now())
                .build();
    }

    public static Mono<Void> deleteMessage(
            @NonNull Mono<MessageChannel> channel,
            @NonNull Snowflake channelId,
            @NonNull ActiveButtonsCache activeButtonsCache,
            @NonNull Snowflake toKeep,
            @NonNull List<String> config) {
        return channel
                .flux()
                .flatMap(c -> {
                    List<Snowflake> allButtonsWithoutTheLast = activeButtonsCache.getAllWithoutOneAndRemoveThem(channelId, toKeep, config);
                    return Flux.fromIterable(allButtonsWithoutTheLast).flatMap(c::getMessageById);
                })
                .onErrorResume(e -> {
                    log.warn("Tried to delete button but it was not found");
                    return Mono.empty();
                })
                .flatMap(Message::delete).next().ofType(Void.class);
    }

    public static Function<TextChannel, Mono<Message>> createButtonMessage(@NonNull ActiveButtonsCache activeButtonsCache,
                                                                           @NonNull String buttonMessage,
                                                                           @NonNull List<LayoutComponent> buttons,
                                                                           @NonNull List<String> config) {
        return channel -> channel
                .createMessage(MessageCreateSpec.builder()
                        .content(buttonMessage)
                        .components(buttons)
                        .build())
                .map(m -> {
                    activeButtonsCache.addChannelWithButton(m.getChannelId(), m.getId(), config);
                    return m;
                });
    }

    public static Mono<Message> createEphemeralButtonReplay(
            @NonNull DeferrableInteractionEvent event,
            @NonNull ActiveButtonsCache activeButtonsCache,
            @NonNull String buttonMessage,
            @NonNull List<LayoutComponent> buttons,
            @NonNull List<String> config) {
        return event
                .createFollowup(InteractionFollowupCreateSpec.builder()
                        .content(buttonMessage)
                        .components(buttons)
                        .ephemeral(true)
                        .build());
    }

    public static String getSlashOptionsToString(ChatInputInteractionEvent event) {
        List<String> options = event.getOptions().stream()
                .map(DiscordUtils::optionToString)
                .collect(Collectors.toList());
        return options.isEmpty() ? "" : options.toString();
    }

    private static String optionToString(ApplicationCommandInteractionOption option) {
        List<String> subOptions = option.getOptions().stream().map(DiscordUtils::optionToString).collect(Collectors.toList());
        return String.format("%s=%s%s",
                option.getName(),
                option.getValue().map(ApplicationCommandInteractionOptionValue::getRaw).orElse(""),
                subOptions.isEmpty() ? "" : subOptions.toString());
    }

}
