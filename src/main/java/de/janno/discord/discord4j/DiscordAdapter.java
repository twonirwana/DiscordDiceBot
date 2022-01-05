package de.janno.discord.discord4j;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.janno.discord.command.IDiscordAdapter;
import de.janno.discord.dice.DiceResult;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class DiscordAdapter implements IDiscordAdapter {

    //needed to correctly show utf8 characters in discord
    private static String encodeUTF8(@NonNull String in) {
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
                .build();
    }

    public static String getSlashOptionsToString(ChatInputInteractionEvent event) {
        List<String> options = event.getOptions().stream()
                .map(DiscordAdapter::optionToString)
                .collect(Collectors.toList());
        return options.isEmpty() ? "" : options.toString();
    }

    private static String optionToString(ApplicationCommandInteractionOption option) {
        List<String> subOptions = option.getOptions().stream().map(DiscordAdapter::optionToString).collect(Collectors.toList());
        return String.format("%s=%s%s",
                option.getName(),
                option.getValue().map(ApplicationCommandInteractionOptionValue::getRaw).orElse(""),
                subOptions.isEmpty() ? "" : subOptions.toString());
    }

    protected EmbedCreateSpec createEmbedMessageWithReference(
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

    protected Mono<Message> createButtonMessage(@NonNull TextChannel channel,
                                                @NonNull String buttonMessage,
                                                @NonNull List<LayoutComponent> buttons) {
        return channel
                .createMessage(MessageCreateSpec.builder()
                        .content(buttonMessage)
                        .components(buttons)
                        .build());
    }

}