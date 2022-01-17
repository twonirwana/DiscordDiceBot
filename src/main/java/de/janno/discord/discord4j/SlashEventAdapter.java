package de.janno.discord.discord4j;

import de.janno.discord.api.Answer;
import de.janno.discord.api.ISlashEventAdaptor;
import de.janno.discord.api.Requester;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class SlashEventAdapter extends DiscordAdapter implements ISlashEventAdaptor {

    private final ChatInputInteractionEvent event;
    private final Mono<Requester> requesterMono;
    private final long channelId;

    public SlashEventAdapter(ChatInputInteractionEvent event, Mono<Requester> requesterMono) {
        this.event = event;
        this.requesterMono = requesterMono;
        this.channelId = event.getInteraction().getChannelId().asLong();
    }

    @Override
    public String checkPermissions() {
        PermissionSet permissions = Mono.zip(event.getInteraction().getChannel().ofType(TextChannel.class)
                                .onErrorResume(t -> {
                                    log.error("Error getting channel", t);
                                    return Mono.empty();
                                })
                        , event.getInteraction().getGuild().flatMap(Guild::getSelfMember)
                                .onErrorResume(t -> {
                                    log.error("Error in getting self member", t);
                                    return Mono.empty();
                                }))
                .onErrorResume(t -> {
                    log.warn("Error in getting data for permission check", t);
                    return Mono.empty();
                })
                .flatMap(channelAndMember -> channelAndMember.getT1().getEffectivePermissions(channelAndMember.getT2()))
                .blockOptional()
                .orElse(PermissionSet.of());

        List<String> checks = new ArrayList<>();
        if (!permissions.contains(Permission.SEND_MESSAGES)) {
            checks.add("'SEND_MESSAGES'");
        }
        if (!permissions.contains(Permission.EMBED_LINKS)) {
            checks.add("'EMBED_LINKS'");
        }
        if (checks.isEmpty()) {
            return null;
        }
        String result = String.format("The bot is missing the permission: %s. It will not work correctly without it. Please check the guild and channel permissions for the bot", String.join(" and ", checks));
        log.info(result);
        return result;
    }

    @Override
    public Optional<ApplicationCommandInteractionOption> getOption(String optionName) {
        return event.getOption(optionName);
    }

    @Override
    public Mono<Void> reply(String message) {
        return event.reply(message)
                .onErrorResume(t -> {
                    log.error("Error on replay", t);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> replyEphemeral(EmbedCreateSpec embedCreateSpec) {
        return event.reply().withEphemeral(true).withEmbeds(embedCreateSpec)
                .onErrorResume(t -> {
                    log.error("Error on replay to slash help command", t);
                    return Mono.empty();
                });

    }

    @Override
    public Mono<Long> createButtonMessage(@NonNull String buttonMessage, @NonNull List<LayoutComponent> buttons) {
        return event.getInteraction().getChannel().ofType(TextChannel.class)
                .flatMap(channel -> createButtonMessage(channel, buttonMessage, buttons))
                .onErrorResume(t -> {
                    log.error("Error on creating button message", t);
                    return Mono.empty();
                })
                .map(m -> m.getId().asLong());
    }

    @Override
    public Mono<Void> createResultMessageWithEventReference(Answer answer) {
        return event.getInteraction().getChannel().ofType(TextChannel.class)
                .flatMap(channel -> channel.createMessage(createEmbedMessageWithReference(answer, event.getInteraction().getMember().orElseThrow())))
                .onErrorResume(t -> {
                    log.error("Error on creating dice result message", t);
                    return Mono.empty();
                })
                .ofType(Void.class);
    }

    @Override
    public Long getChannelId() {
        return channelId;
    }

    @Override
    public String getCommandString() {
        String options = event.getOptions().stream()
                .map(a -> optionToString(a.getName(), a.getOptions(), a.getValue().orElse(null)))
                .collect(Collectors.joining(" "));
        return String.format("`/%s %s`", event.getCommandName(), options);


    }

    private String optionToString(@NonNull String name, @NonNull List<ApplicationCommandInteractionOption> options, @Nullable ApplicationCommandInteractionOptionValue value) {
        if (options.isEmpty() && value == null) {
            return name;
        }
        String out = name;
        if (value != null) {
            out = String.format("%s:%s", name, value.getRaw());
        }
        String optionsString = options.stream()
                .map(a -> optionToString(a.getName(), a.getOptions(), a.getValue().orElse(null)))
                .collect(Collectors.joining(" "));
        if (!optionsString.isEmpty()) {
            out = String.format("%s %s", out, optionsString);
        }

        return out;
    }


    @Override
    public Mono<Requester> getRequester() {
        return requesterMono;
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


}
