package de.janno.discord.connector.jda;

import com.google.common.base.Strings;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.MessageState;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.AttachmentProxy;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@Slf4j
public class ButtonEventAdapterImpl extends DiscordAdapterImpl implements ButtonEventAdaptor {
    @NonNull
    private final GenericComponentInteractionCreateEvent event;
    @NonNull
    private final String customId;
    private final long messageId;
    private final long channelId;
    /**
     * null if the button event is not in a guild
     */
    private final Long guildId;
    private final long userId;
    private final boolean isPinned;
    @NonNull
    private final Requester requester;
    @NonNull
    private final String invokingGuildMemberName;

    public ButtonEventAdapterImpl(@NonNull String customId,
                                  @NonNull GenericComponentInteractionCreateEvent event,
                                  @NonNull Requester requester) {
        this.event = event;
        this.requester = requester;
        this.messageId = event.getMessageIdLong();
        this.customId = customId;
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
    public @NonNull Mono<Void> editMessage(String message, List<ComponentRowDefinition> componentRowDefinitions) {
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
                .setContent(StringUtils.abbreviate(encodeUTF8(message), Message.MAX_CONTENT_LENGTH)))
                .then(createMonoFrom(event::deferEdit))
                .then()
                .onErrorResume(t -> handleException("Error on edit button event", t, true));

    }

    @Override
    public @NonNull Mono<Long> sendMessage(@NonNull EmbedOrMessageDefinition messageDefinition) {
        MessageChannel targetChannel = Optional.ofNullable(messageDefinition.getSendToOtherChannelId())
                .flatMap(id -> Optional.ofNullable(event.getGuild())
                        .map(g -> (MessageChannel) g.getChannelById(GuildMessageChannel.class, id)))
                .orElse(event.getInteraction().getMessageChannel());

        return sendMessageWithOptionalReference(targetChannel,
                messageDefinition,
                messageDefinition.isUserReference() ? invokingGuildMemberName : null,
                messageDefinition.isUserReference() ? event.getUser().getAsMention() : null,
                messageDefinition.isUserReference() ? Optional.ofNullable(event.getMember()).map(Member::getEffectiveAvatarUrl).orElse(event.getUser().getEffectiveAvatarUrl()) : null,
                messageDefinition.isUserReference() ? event.getUser().getId() : null)
                .onErrorResume(t -> handleException("Error on sending message", t, false).ofType(Message.class))
                .map(Message::getIdLong);
    }

    @Override
    public @NonNull Optional<String> checkPermissions(Long answerTargetChannelId, @NonNull Locale userLocale) {
        Optional<String> primaryChannelPermissionCheck = checkPermission(event.getMessageChannel(), event.getGuild(), true, userLocale);
        if (primaryChannelPermissionCheck.isPresent()) {
            return primaryChannelPermissionCheck;
        }
        if (answerTargetChannelId != null) {
            Optional<MessageChannel> answerChannel = Optional.ofNullable(event.getGuild()).map(g -> g.getChannelById(GuildMessageChannel.class, answerTargetChannelId));
            if (answerChannel.isEmpty()) {
                return Optional.of(I18n.getMessage("permission.check.target.invalid", userLocale));
            }
            return checkPermission(answerChannel.get(), event.getGuild(), true, userLocale);
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
    public @NonNull Mono<Void> acknowledge() {
        return Mono.defer(() -> createMonoFrom(event::deferEdit)
                .onErrorResume(t -> handleException("Error on deferEdit", t, true).ofType(InteractionHook.class))
                .then());
    }

    @Override
    public @NonNull ParallelFlux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds) {
        return getMessagesState(event.getMessageChannel(), messageIds);
    }

    @Override
    protected @NonNull MessageChannel getMessageChannel() {
        return event.getMessageChannel();
    }

    @Override
    protected @NonNull String getErrorRequester() {
        return requester.toLogString();
    }

    @Override
    public @NonNull OffsetDateTime getMessageCreationTime() {
        return event.getMessage().getTimeCreated();
    }

    @Override
    public @NonNull Mono<Void> acknowledgeAndDeleteOriginal() {
        return createMonoFrom(event::deferEdit)
                .onErrorResume(t -> handleException("Error on deferEdit", t, true).ofType(InteractionHook.class))
                .then(createMonoFrom(() -> event.getInteraction().getHook().deleteOriginal()));
    }

    @Override
    public @NonNull EmbedOrMessageDefinition getMessageDefinitionOfEventMessageWithoutButtons() {
        Message message = event.getMessage();

        final String descriptionOrContent;
        final EmbedOrMessageDefinition.Type type;
        final Supplier<InputStream> imageSupplier;
        final List<EmbedOrMessageDefinition.Field> fields;
        final String title;
        if (message.getEmbeds().isEmpty()) {
            type = EmbedOrMessageDefinition.Type.MESSAGE;
            descriptionOrContent = message.getContentRaw();
            imageSupplier = null;
            fields = List.of();
            title = null;
        } else {
            type = EmbedOrMessageDefinition.Type.EMBED;
            MessageEmbed embed = message.getEmbeds().getFirst();
            descriptionOrContent = embed.getDescription();
            title = embed.getTitle();
            if (embed.getImage() == null) {
                imageSupplier = null;
            } else {
                imageSupplier = () -> Optional.ofNullable(embed.getImage().getProxy())
                        .map(AttachmentProxy::download)
                        .map(inputStreamCompletableFuture -> {
                            try {
                                return inputStreamCompletableFuture.get();
                            } catch (InterruptedException | ExecutionException e) {
                                log.error(e.getMessage());
                            }
                            return InputStream.nullInputStream();
                        })
                        .orElse(InputStream.nullInputStream());
            }
            fields = embed.getFields().stream()
                    .filter(f -> !Strings.isNullOrEmpty(f.getName()))
                    .filter(f -> !Strings.isNullOrEmpty(f.getValue()))
                    .map(f -> new EmbedOrMessageDefinition.Field(f.getName(), f.getValue(), f.isInline()))
                    .toList();
        }

        return EmbedOrMessageDefinition.builder()
                .type(type)
                .title(title)
                .descriptionOrContent(descriptionOrContent)
                .image(imageSupplier)
                .fields(fields)
                .build();
    }
}
