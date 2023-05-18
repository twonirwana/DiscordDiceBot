package de.janno.discord.connector.jda;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.DiscordAdapter;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
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
            @NonNull EmbedOrMessageDefinition answer,
            @NonNull String rollRequesterName,
            @NonNull String rollRequesterMention,
            @Nullable String rollRequesterAvatar,
            @NonNull String rollRequesterId) {
        switch (answer.getType()) {
            case EMBED -> {
                EmbedBuilder builder = new EmbedBuilder();
                if (!Strings.isNullOrEmpty(answer.getTitle())) {
                    builder.setTitle(StringUtils.abbreviate(encodeUTF8(answer.getTitle()), 256));//https://discord.com/developers/docs/resources/channel#embed-limits
                }
                builder.setAuthor(rollRequesterName,
                                null,
                                rollRequesterAvatar)
                        .setColor(Color.decode(String.valueOf(rollRequesterId.hashCode())));
                if (!Strings.isNullOrEmpty(answer.getDescriptionOrContent())) {
                    builder.setDescription(StringUtils.abbreviate(encodeUTF8(answer.getDescriptionOrContent()), 4096)); //https://discord.com/developers/docs/resources/channel#embed-limits
                }

                if (answer.getFields().size() > 25) {
                    log.error("Number of dice results was {} and was reduced", answer.getFields().size());
                }
                List<EmbedOrMessageDefinition.Field> limitedList = answer.getFields().stream().limit(25).collect(ImmutableList.toImmutableList()); //https://discord.com/developers/docs/resources/channel#embed-limits
                for (EmbedOrMessageDefinition.Field field : limitedList) {
                    builder.addField(StringUtils.abbreviate(encodeUTF8(field.getName()), 256), //https://discord.com/developers/docs/resources/channel#embed-limits
                            StringUtils.abbreviate(encodeUTF8(field.getValue()), 1024), //https://discord.com/developers/docs/resources/channel#embed-limits
                            field.isInline());
                }
                final List<FileUpload> files;
                if (answer.getFile() != null) {
                    files = List.of(FileUpload.fromData(answer.getFile(), "image.png"));
                    builder.setImage("attachment://image.png");
                } else {
                    files = List.of();
                }
                return createMonoFrom(() -> messageChannel.sendMessageEmbeds(builder.build()).setFiles(files).setSuppressedNotifications(true));
            }
            case MESSAGE -> {
                MessageCreateBuilder builder = new MessageCreateBuilder();
                String answerString = rollRequesterMention + ": " + Optional.ofNullable(answer.getDescriptionOrContent()).map(s -> s + " ").orElse("") + answer.getFields().stream().map(EmbedOrMessageDefinition.Field::getName).collect(Collectors.joining(" "));
                answerString = StringUtils.abbreviate(encodeUTF8(answerString), Message.MAX_CONTENT_LENGTH);
                builder.setSuppressedNotifications(true);
                builder.setContent(answerString);
                return createMonoFrom(() -> messageChannel.sendMessage(builder.build()));
            }
            default -> throw new IllegalStateException("Unknown type in %s".formatted(answer));
        }
    }

    protected Mono<Message> createButtonMessage(@NonNull MessageChannel channel,
                                                @NonNull MessageDefinition messageDefinition) {
        return createMonoFrom(() -> channel.sendMessage(
                MessageComponentConverter.messageComponent2MessageLayout(
                        StringUtils.abbreviate(encodeUTF8(messageDefinition.getContent()), Message.MAX_CONTENT_LENGTH),
                        messageDefinition.getComponentRowDefinitions())));
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
