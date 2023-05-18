package de.janno.discord.connector.jda;

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
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private final long userId;
    private final boolean isPinned;
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
        this.userId = event.getUser().getIdLong();
        this.invokingGuildMemberName = Optional.ofNullable(event.getMember()).map(Member::getEffectiveName).orElse(event.getUser().getName());
    }

    @Override
    public long getUserId() {
        return userId;
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
        Optional<String> primaryChannelPermissionCheck = checkPermission(event.getMessageChannel(), event.getGuild(), true);
        if (primaryChannelPermissionCheck.isPresent()) {
            return primaryChannelPermissionCheck;
        }
        if (answerTargetChannelId != null) {
            Optional<MessageChannel> answerChannel = Optional.ofNullable(event.getGuild()).map(g -> g.getChannelById(MessageChannel.class, answerTargetChannelId));
            if (answerChannel.isEmpty()) {
                return Optional.of("Configured answer target channel is not a valid message channel");
            }
            return checkPermission(answerChannel.get(), event.getGuild(), true);
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
        return createMonoFrom(() -> event.reply(StringUtils.abbreviate(encodeUTF8(message), Message.MAX_CONTENT_LENGTH)).setEphemeral(ephemeral))
                .onErrorResume(t -> handleException("Error on replay", t, true).ofType(InteractionHook.class))
                .then();
    }

    @Override
    public @NonNull ParallelFlux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds) {
        return Flux.fromIterable(messageIds)
                .parallel()
                .flatMap(id -> {
                    //small optimization to avoid unnecessary requests
                    if (id.equals(messageId)) {
                        return Mono.just(new MessageState(id, isPinned, true, true, getMessageCreationTime()));
                    }
                    try {
                        return Mono.fromFuture(event.getMessageChannel().retrieveMessageById(id)
                                .submit().handle((m, t) -> {
                                    if (m != null) {
                                        return new MessageState(m.getIdLong(), m.isPinned(), true, m.getType().canDelete(), m.getTimeCreated());
                                    }
                                    if (t != null) {
                                        if (t instanceof ErrorResponseException errorResponseException) {
                                            if (Set.of(MISSING_ACCESS, MISSING_PERMISSIONS, UNKNOWN_MESSAGE, UNKNOWN_CHANNEL).contains(errorResponseException.getErrorResponse())) {
                                                return new MessageState(id, false, false, false, null);
                                            }
                                        }
                                        throw new RuntimeException(t);
                                    }
                                    throw new IllegalStateException("Message and throwable are null");
                                }));
                    } catch (Exception e) {
                        //for some reason it is thrown outside the handle method, and we need to catch it here
                        if (e instanceof InsufficientPermissionException) {
                            return Mono.just(new MessageState(id, false, false, false, null));
                        }
                        return Mono.error(e);
                    }
                });
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
