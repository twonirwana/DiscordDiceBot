package de.janno.discord.connector.jda;

import com.google.common.base.Strings;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ButtonEventAdapter extends DiscordAdapter implements IButtonEventAdaptor {
    @NonNull
    private final ButtonInteractionEvent event;
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

    public ButtonEventAdapter(@NonNull ButtonInteractionEvent event,
                              @NonNull Mono<Requester> requesterMono) {
        this.event = event;
        this.requesterMono = requesterMono;
        this.messageId = event.getMessageIdLong();
        this.customId = event.getInteraction().getComponentId();
        this.isPinned = event.getMessage().isPinned();
        this.channelId = event.getChannel().getIdLong();
        this.messageContent = event.getMessage().getContentRaw();
        this.allButtonIds = event.getMessage().getButtons().stream()
                .flatMap(l -> {
                    if (l.getLabel().isEmpty() || Strings.isNullOrEmpty(l.getId())) {
                        return Stream.empty();
                    }
                    return Stream.of(new LabelAndCustomId(l.getLabel(), l.getId()));
                }).collect(Collectors.toList());
        this.invokingGuildMemberName = Optional.ofNullable(event.getInteraction().getGuild())
                .map(g -> g.getMemberById(event.getInteraction().getUser().getId())).map(Member::getEffectiveName)
                .orElse(event.getInteraction().getUser().getName());
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
        //not needed with JDA
        return Mono.empty();
    }


    @Override
    public Mono<Void> editMessage(String message) {
        return createMonoFrom(() -> event.editMessage(message)).then()
                .onErrorResume(t -> handleException("Error on edit button event", t, true));

    }

    @Override
    public Mono<Long> createButtonMessage(MessageDefinition messageDefinition) {
        return createButtonMessage(event.getMessageChannel(), messageDefinition)
                .onErrorResume(t -> handleException("Error on creating button message", t, false).ofType(Message.class))
                .map(Message::getIdLong);
    }

    @Override
    public Mono<Void> deleteMessage(long messageId) {
        return deleteMessage(event.getInteraction().getMessageChannel(), messageId);
    }

    @Override
    public Mono<Void> createResultMessageWithEventReference(EmbedDefinition answer, Long targetChannelId) {

        MessageChannel targetChannel = Optional.ofNullable(targetChannelId)
                .flatMap(id -> Optional.ofNullable(event.getGuild())
                        .map(g -> g.getChannelById(MessageChannel.class, targetChannelId)))
                .orElse(event.getInteraction().getMessageChannel());
        return createEmbedMessageWithReference(targetChannel,
                answer, event.getInteraction().getUser(),
                event.getInteraction().getGuild())
                .onErrorResume(t -> handleException("Error on creating answer message", t, false).ofType(Message.class))
                .ofType(Void.class);
    }

    @Override
    public Optional<String> checkPermissions(Long answerTargetChannelId) {
        Optional<String> primaryChannelPermissionCheck = checkPermission(event.getMessageChannel(), event.getGuild());
        if (primaryChannelPermissionCheck.isPresent()) {
            return primaryChannelPermissionCheck;
        }
        if (answerTargetChannelId != null) {
            Optional<MessageChannel> answerChannel = Optional.ofNullable(event.getGuild()).map(g -> g.getChannelById(MessageChannel.class, answerTargetChannelId));
            if (answerChannel.isEmpty()) {
                return Optional.of("Configured answer target channel is not a valid message channel");
            }
            return checkPermission(answerChannel.get(), event.getGuild());
        }
        return Optional.empty();
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
