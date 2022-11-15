package de.janno.discord.connector.jda;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.DiscordAdapter;
import de.janno.discord.connector.api.MessageState;
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
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.requests.ErrorResponse.*;

@Slf4j
public abstract class DiscordAdapterImpl implements DiscordAdapter {

    //needed to correctly show utf8 characters in discord
    private static String encodeUTF8(@NonNull String in) {
        return new String(in.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }


    //todo include the error handling
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
                builder.setTitle(StringUtils.abbreviate(encodeUTF8(answer.getTitle()), 256))//https://discord.com/developers/docs/resources/channel#embed-limits
                        .setAuthor(rollRequesterName,
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
                return createMonoFrom(() -> messageChannel.sendMessageEmbeds(builder.build()));
            }
            case MESSAGE -> {
                MessageCreateBuilder builder = new MessageCreateBuilder();
                String answerString = rollRequesterMention + ": " + Optional.ofNullable(answer.getDescriptionOrContent()).map(s -> s + " ").orElse("") + answer.getFields().stream().map(EmbedOrMessageDefinition.Field::getName).collect(Collectors.joining(" "));
                answerString = StringUtils.abbreviate(encodeUTF8(answerString), 2000); //seems to be the limit
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
                        StringUtils.abbreviate(encodeUTF8(messageDefinition.getContent()), 2000), //seems to be the limit
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

    protected Optional<String> checkPermission(@NonNull MessageChannel messageChannel, @Nullable Guild guild) {
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
    public @NonNull Flux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds) {
        return Flux.fromIterable(messageIds)
                .flatMap(id -> Mono.fromFuture(getMessageChannel().retrieveMessageById(id)
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
                ))
                .onErrorResume(t -> handleException("Error on getting message state", t, false).ofType(MessageState.class));
    }

    @Override
    public @NonNull Mono<Void> deleteMessageById(long messageId) {
        return Mono.fromFuture(getMessageChannel().deleteMessageById(messageId).submit())
                .onErrorResume(t -> handleException("Error on deleting message", t, true).ofType(Void.class));
    }

    protected abstract @NonNull MessageChannel getMessageChannel();

    protected abstract @NonNull String getGuildAndChannelName();
}
