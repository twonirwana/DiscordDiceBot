package de.janno.discord.connector.jda;

import com.google.common.base.Strings;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.MessageState;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.dv8tion.jda.api.requests.ErrorResponse.*;

@Slf4j
public class ButtonEventAdapterImpl extends DiscordAdapterImpl implements ButtonEventAdaptor {
    @NonNull
    private final ButtonInteractionEvent event;
    @NonNull
    private final String customId;
    private final long messageId;
    private final long channelId;
    private final Long guildId;
    private final boolean isPinned;
    @NonNull
    private final String messageContent;
    @NonNull
    private final List<LabelAndCustomId> allButtonIds;
    @NonNull
    private final Requester requester;
    @NonNull
    private final String invokingGuildMemberName;

    public ButtonEventAdapterImpl(@NonNull ButtonInteractionEvent event,
                                  @NonNull Requester requester) {
        this.event = event;
        this.requester = requester;
        this.messageId = event.getMessageIdLong();
        this.customId = event.getInteraction().getComponentId();
        this.isPinned = event.getMessage().isPinned();
        this.guildId = Optional.ofNullable(event.getGuild()).map(Guild::getIdLong).orElse(null);
        this.channelId = event.getChannel().getIdLong();
        this.messageContent = event.getMessage().getContentRaw();
        this.allButtonIds = event.getMessage().getButtons().stream()
                .flatMap(l -> {
                    if (l.getLabel().isEmpty() || Strings.isNullOrEmpty(l.getId())) {
                        return Stream.empty();
                    }
                    return Stream.of(new LabelAndCustomId(l.getLabel(), l.getId()));
                }).collect(Collectors.toList());
        this.invokingGuildMemberName = Optional.ofNullable(event.getMember()).map(Member::getEffectiveName).orElse(event.getUser().getName());
    }

    @Override
    public Long getGuildId() {
        return guildId;
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
    public Mono<Void> editMessage(String message, List<ComponentRowDefinition> componentRowDefinitions) {
        if (message == null && componentRowDefinitions == null) {
            return Mono.empty();
        }
        if (message != null && componentRowDefinitions == null) {
            return createMonoFrom(() -> event.editMessage(message)).then()
                    .onErrorResume(t -> handleException("Error on edit button event", t, true));
        }

        if (message == null) {
            return createMonoFrom(() -> event.editComponents(MessageComponentConverter.componentRowDefinition2LayoutComponent(componentRowDefinitions))).then()
                    .onErrorResume(t -> handleException("Error on edit button event", t, true));
        }

        return createMonoFrom(() -> event.getHook()
                .editOriginalComponents(MessageComponentConverter.componentRowDefinition2LayoutComponent(componentRowDefinitions))
                .setContent(message))
                .then(createMonoFrom(event::deferEdit))
                .then()
                .onErrorResume(t -> handleException("Error on edit button event", t, true));

    }

    @Override
    public @NonNull Mono<Long> createButtonMessage(@NonNull MessageDefinition messageDefinition) {
        return createButtonMessage(event.getMessageChannel(), messageDefinition)
                .onErrorResume(t -> handleException("Error on creating button message", t, false).ofType(Message.class))
                .map(Message::getIdLong);
    }

    @Override
    public Mono<Void> createResultMessageWithEventReference(EmbedOrMessageDefinition answer, Long targetChannelId) {
        MessageChannel targetChannel = Optional.ofNullable(targetChannelId)
                .flatMap(id -> Optional.ofNullable(event.getGuild())
                        .map(g -> g.getChannelById(MessageChannel.class, targetChannelId)))
                .orElse(event.getInteraction().getMessageChannel());
        return createMessageWithReference(targetChannel,
                answer, invokingGuildMemberName, event.getUser().getAsMention(),
                Optional.ofNullable(event.getMember()).map(Member::getEffectiveAvatarUrl).orElse(event.getUser().getEffectiveAvatarUrl()),
                event.getUser().getId())
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
    public @NonNull Requester getRequester() {
        return requester;
    }

    @Override
    public @NonNull String getInvokingGuildMemberName() {
        return invokingGuildMemberName;
    }

    @Override
    public @NonNull Mono<Void> reply(@NonNull String message, boolean ephemeral) {
        return createMonoFrom(() -> event.reply(message).setEphemeral(ephemeral))
                .onErrorResume(t -> handleException("Error on replay", t, true).ofType(InteractionHook.class))
                .then();
    }

    @Override
    public @NonNull Flux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds) {
        return Flux.fromIterable(messageIds)
                .flatMap(id -> {
                    //small optimization to avoid unnecessary requests
                    if (id.equals(messageId)) {
                        return Mono.just(new MessageState(id, isPinned, true, true, getMessageCreationTime()));
                    }
                    return Mono.fromFuture(event.getMessageChannel().retrieveMessageById(id)
                            .submit()
                            .handle((m, t) -> {
                                if (m != null) {
                                    return new MessageState(m.getIdLong(), m.isPinned(), true, m.getType().canDelete(), m.getTimeCreated());
                                }
                                if (t != null) {
                                    if (t instanceof ErrorResponseException errorResponseException) {
                                        if (Set.of(MISSING_ACCESS, MISSING_PERMISSIONS, INVALID_DM_ACTION).contains(errorResponseException.getErrorResponse())) {
                                            return new MessageState(id, false, true, false, null);
                                        } else if (Set.of(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL).contains(errorResponseException.getErrorResponse())) {
                                            return new MessageState(id, false, false, false, null);
                                        }
                                    } else if (t instanceof InsufficientPermissionException) {
                                        return new MessageState(id, false, true, false, null);
                                    }
                                    throw new RuntimeException(t);
                                }
                                throw new IllegalStateException("Message and throwable are null");
                            })
                    );
                })
                .onErrorResume(t -> handleException("Error on getting message state", t, false).ofType(MessageState.class));
    }

    @Override
    protected @NonNull MessageChannel getMessageChannel() {
        return event.getMessageChannel();
    }

    @Override
    protected @NonNull String getGuildAndChannelName() {
        return requester.toLogString();
    }

    @Override
    public @NonNull OffsetDateTime getMessageCreationTime() {
        return event.getMessage().getTimeCreated();
    }
}
