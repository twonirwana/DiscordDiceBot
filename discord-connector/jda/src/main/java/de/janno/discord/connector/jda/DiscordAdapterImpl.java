package de.janno.discord.connector.jda;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.DiscordAdapter;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public abstract class DiscordAdapterImpl implements DiscordAdapter {

    //needed to correctly show utf8 characters in discord
    protected static String encodeUTF8(String in) {
        if (in == null) {
            return null;
        }
        return new String(in.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    protected static <T> Mono<T> createMonoFrom(Supplier<RestAction<T>> actionSupplier) {
        try {
            return Mono.fromFuture(actionSupplier.get().submit());
        } catch (Throwable t) {
            return Mono.error(t);
        }
    }

    protected Mono<Message> createMessageWithReference(
            @NonNull MessageChannel messageChannel,
            @NonNull EmbedOrMessageDefinition messageDefinition,
            @Nullable String rollRequesterName,
            @Nullable String rollRequesterMention,
            @Nullable String rollRequesterAvatar,
            @Nullable String rollRequesterId) {
        LayoutComponent[] layoutComponents = MessageComponentConverter.componentRowDefinition2LayoutComponent(messageDefinition.getComponentRowDefinitions());
        switch (messageDefinition.getType()) {
            case EMBED -> {
                EmbedBuilder builder = convertToEmbedMessage(messageDefinition, rollRequesterName, rollRequesterAvatar, rollRequesterId);
                final List<FileUpload> files = applyFiles(builder, messageDefinition);

                return createMonoFrom(() -> messageChannel.sendMessageEmbeds(builder.build()).setComponents(layoutComponents).setFiles(files).setSuppressedNotifications(true));
            }
            case MESSAGE -> {
                return createMonoFrom(() -> messageChannel.sendMessage(convertToMessageCreateData(messageDefinition, rollRequesterMention)).setComponents(layoutComponents).setSuppressedNotifications(true));
            }
            default -> throw new IllegalStateException("Unknown type in %s".formatted(messageDefinition));
        }
    }

    private MessageCreateData convertToMessageCreateData(@NonNull EmbedOrMessageDefinition messageDefinition,
                                                         @Nullable String rollRequesterMention) {
        Preconditions.checkArgument(messageDefinition.getType() == EmbedOrMessageDefinition.Type.MESSAGE);
        MessageCreateBuilder builder = new MessageCreateBuilder();
        final String answerString;
        if (rollRequesterMention != null) {
            answerString = "%s: %s%s".formatted(rollRequesterMention,
                    Optional.ofNullable(messageDefinition.getDescriptionOrContent()).map(s -> s + " ").orElse(""),
                    messageDefinition.getFields().stream().map(EmbedOrMessageDefinition.Field::getName).collect(Collectors.joining(" ")));
        } else {
            answerString = "%s%s".formatted(Optional.ofNullable(messageDefinition.getDescriptionOrContent()).map(s -> s + " ").orElse(""),
                    messageDefinition.getFields().stream().map(EmbedOrMessageDefinition.Field::getName).collect(Collectors.joining(" ")));
        }

        builder.setSuppressedNotifications(true);
        builder.setContent(StringUtils.abbreviate(encodeUTF8(answerString), Message.MAX_CONTENT_LENGTH));
        return builder.build();
    }

    private EmbedBuilder convertToEmbedMessage(@NonNull EmbedOrMessageDefinition messageDefinition,
                                               @Nullable String rollRequesterName,
                                               @Nullable String rollRequesterAvatar,
                                               @Nullable String rollRequesterId) {
        Preconditions.checkArgument(messageDefinition.getType() == EmbedOrMessageDefinition.Type.EMBED);
        EmbedBuilder builder = new EmbedBuilder();
        if (!Strings.isNullOrEmpty(messageDefinition.getTitle())) {
            builder.setTitle(StringUtils.abbreviate(encodeUTF8(messageDefinition.getTitle()), 256));//https://discord.com/developers/docs/resources/channel#embed-limits
        }
        builder.setAuthor(rollRequesterName,
                null,
                rollRequesterAvatar);
        if (rollRequesterId != null) {
            builder.setColor(Color.decode(String.valueOf(rollRequesterId.hashCode())));
        }

        if (!Strings.isNullOrEmpty(messageDefinition.getDescriptionOrContent())) {
            builder.setDescription(StringUtils.abbreviate(encodeUTF8(messageDefinition.getDescriptionOrContent()), 4096)); //https://discord.com/developers/docs/resources/channel#embed-limits
        }

        if (messageDefinition.getFields().size() > 25) {
            log.error("Number of dice results was {} and was reduced", messageDefinition.getFields().size());
        }
        List<EmbedOrMessageDefinition.Field> limitedList = messageDefinition.getFields().stream().limit(25).collect(ImmutableList.toImmutableList()); //https://discord.com/developers/docs/resources/channel#embed-limits
        for (EmbedOrMessageDefinition.Field field : limitedList) {
            builder.addField(StringUtils.abbreviate(encodeUTF8(field.getName()), 256), //https://discord.com/developers/docs/resources/channel#embed-limits
                    StringUtils.abbreviate(encodeUTF8(field.getValue()), 1024), //https://discord.com/developers/docs/resources/channel#embed-limits
                    field.isInline());
        }

        return builder;
    }

    //todo move to SlashEventAdaptorImpl or generlize
    protected Mono<InteractionHook> replyWithMessage(
            @NonNull SlashCommandInteractionEvent event,
            @NonNull EmbedOrMessageDefinition messageDefinition,
            boolean ephemeral) {
        LayoutComponent[] layoutComponents = MessageComponentConverter.componentRowDefinition2LayoutComponent(messageDefinition.getComponentRowDefinitions());
        switch (messageDefinition.getType()) {
            case EMBED -> {
                EmbedBuilder builder = convertToEmbedMessage(messageDefinition, null, null, null);
                final List<FileUpload> files = applyFiles(builder, messageDefinition);
                return createMonoFrom(() -> event.replyEmbeds(builder.build()).setComponents(layoutComponents).setEphemeral(ephemeral).setFiles(files).setSuppressedNotifications(true));
            }
            case MESSAGE -> {
                return createMonoFrom(() -> event.reply(convertToMessageCreateData(messageDefinition, null)).setComponents(layoutComponents).setEphemeral(ephemeral).setSuppressedNotifications(true));
            }
            default -> throw new IllegalStateException("Unknown type in %s".formatted(messageDefinition));
        }
    }

    private List<FileUpload> applyFiles(@NonNull EmbedBuilder builder, @NonNull EmbedOrMessageDefinition messageDefinition) {
        if (messageDefinition.getImage() != null) {
            builder.setImage("attachment://image.png");
            return List.of(FileUpload.fromStreamSupplier("image.png", messageDefinition.getImage()));
        }
        return List.of();
    }

    protected Mono<Message> createMessageWithoutReference(@NonNull MessageChannel channel,
                                                          @NonNull EmbedOrMessageDefinition messageDefinition) {
        return createMessageWithReference(channel, messageDefinition, null, null, null, null);
    }

    protected Mono<Void> handleException(@NonNull String errorMessage,
                                         @NonNull Throwable throwable,
                                         boolean ignoreNotFound) {
        if (throwable instanceof InsufficientPermissionException) {
            log.info(String.format("%s: Missing permissions: %s - %s", getGuildAndChannelName(), errorMessage, throwable.getMessage()));
            return Mono.empty();
        } else if (throwable instanceof ErrorResponseException &&
                ((ErrorResponseException) throwable).getErrorResponse().getCode() < 20000
                && ignoreNotFound) {
            log.trace(String.format("%s: Not found: %s", getGuildAndChannelName(), errorMessage));
        } else {
            log.error("%s: %s".formatted(getGuildAndChannelName(), errorMessage), throwable);
        }
        return Mono.empty();
    }

    /**
     * check the permissions
     *
     * @param allowLegacyPermission if this is set to true only the old set of permissions is checked, so old button messages work as created. Should be removed in the future.
     * @return Optional message with the missing permissions
     */
    protected Optional<String> checkPermission(@NonNull MessageChannel messageChannel, @Nullable Guild guild, boolean allowLegacyPermission) {
        List<String> checks = new ArrayList<>();
        if (!messageChannel.canTalk()) {
            checks.add("'SEND_MESSAGES'");
        }
        boolean missingEmbedPermission = Optional.of(messageChannel)
                .filter(m -> m instanceof GuildMessageChannel)
                .map(m -> (GuildMessageChannel) m)
                .flatMap(g -> Optional.ofNullable(guild).map(Guild::getSelfMember).map(m -> !m.hasPermission(g, Permission.MESSAGE_EMBED_LINKS)))
                .orElse(true);
        if (missingEmbedPermission) {
            checks.add("'EMBED_LINKS'");
        }

        if (!allowLegacyPermission) {
            boolean missingMessageHistoryPermission = Optional.of(messageChannel)
                    .filter(m -> m instanceof GuildMessageChannel)
                    .map(m -> (GuildMessageChannel) m)
                    .flatMap(g -> Optional.ofNullable(guild).map(Guild::getSelfMember).map(m -> !m.hasPermission(g, Permission.MESSAGE_HISTORY)))
                    .orElse(true);
            if (missingMessageHistoryPermission) {
                checks.add("'MESSAGE_HISTORY'");
            }
            boolean missingAddFilePermission = Optional.of(messageChannel)
                    .filter(m -> m instanceof GuildMessageChannel)
                    .map(m -> (GuildMessageChannel) m)
                    .flatMap(g -> Optional.ofNullable(guild).map(Guild::getSelfMember).map(m -> !m.hasPermission(g, Permission.MESSAGE_ATTACH_FILES)))
                    .orElse(true);
            if (missingAddFilePermission) {
                checks.add("'ATTACH_FILES'");
            }
        }
        if (checks.isEmpty()) {
            return Optional.empty();
        }
        String result = String.format("'%s'.'%s': The bot is missing the permission: %s. It will not work correctly without it. Please check the guild and channel permissions for the bot",
                Optional.ofNullable(guild).map(Guild::getName).orElse("-"),
                messageChannel.getName(),
                String.join(" and ", checks));
        log.info(result);
        return Optional.of(result);
    }

    @Override
    public @NonNull Mono<Void> deleteMessageById(long messageId) {
        return Mono.fromFuture(getMessageChannel().deleteMessageById(messageId).submit())
                .onErrorResume(t -> handleException("Error on deleting message", t, true).ofType(Void.class));
    }

    protected abstract @NonNull MessageChannel getMessageChannel();

    protected abstract @NonNull String getGuildAndChannelName();
}
