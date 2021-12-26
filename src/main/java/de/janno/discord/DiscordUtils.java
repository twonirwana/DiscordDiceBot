package de.janno.discord;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.janno.discord.command.ActiveButtonsCache;
import de.janno.discord.dice.DiceResult;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
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

    private static EmbedCreateSpec createEmbedMessageWithReference(
            @NonNull DiceResult diceResult,
            @NonNull Member rollRequester) {
        return EmbedCreateSpec.builder()
                .title(StringUtils.abbreviate(encodeUTF8(diceResult.getResultTitle()), 256)) //https://discord.com/developers/docs/resources/channel#embed-limits
                .author(rollRequester.getDisplayName(), null, rollRequester.getAvatarUrl())
                .color(Color.of(rollRequester.getId().hashCode()))
                .description(StringUtils.abbreviate(encodeUTF8(diceResult.getResultDetails()), 4096)) //https://discord.com/developers/docs/resources/channel#embed-limits
                //   .timestamp(Instant.now())
                .build();
    }

    public static EmbedCreateSpec createEmbedMessageWithReference(
            @NonNull List<DiceResult> diceResults,
            @NonNull Member rollRequester) {
        Preconditions.checkArgument(!diceResults.isEmpty(), "Results list empty");
        if (diceResults.size() == 1) {
            return createEmbedMessageWithReference(diceResults.get(0), rollRequester);
        }
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                .title(StringUtils.abbreviate(encodeUTF8("Multiple Results"), 256)) //https://discord.com/developers/docs/resources/channel#embed-limits
                .author(rollRequester.getDisplayName(), null, rollRequester.getAvatarUrl())
                .color(Color.of(rollRequester.getId().hashCode()));
        if (diceResults.size() > 25) {
            log.error("Number of dice results was {} and was reduced", diceResults.size());
        }
        List<DiceResult> limitedList = diceResults.stream().limit(25).collect(ImmutableList.toImmutableList()); //https://discord.com/developers/docs/resources/channel#embed-limits
        for (DiceResult diceResult : limitedList) {
            builder.addField(StringUtils.abbreviate(encodeUTF8(diceResult.getResultTitle()), 256), //https://discord.com/developers/docs/resources/channel#embed-limits
                    StringUtils.abbreviate(encodeUTF8(diceResult.getResultDetails()), 1024), //https://discord.com/developers/docs/resources/channel#embed-limits
                    false);
        }
        return builder.build();
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
