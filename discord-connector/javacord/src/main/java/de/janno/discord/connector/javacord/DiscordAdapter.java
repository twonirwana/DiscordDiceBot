package de.janno.discord.connector.javacord;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.Answer;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.IDiscordAdapter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.exception.MissingPermissionsException;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public abstract class DiscordAdapter implements IDiscordAdapter {

    protected static final String PERMISSION_ERROR_MESSAGE = "Missing permission, see https://github.com/twonirwana/DiscordDiceBot for help";

    //needed to correctly show utf8 characters in discord
    private static String encodeUTF8(@NonNull String in) {
        return new String(in.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    protected Mono<Message> createEmbedMessageWithReference(
            @NonNull TextChannel textChannel,
            @NonNull Answer answer,
            @NonNull User rollRequester,
            @NonNull Server server) {

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(StringUtils.abbreviate(encodeUTF8(answer.getTitle()), 256))//https://discord.com/developers/docs/resources/channel#embed-limits
                .setAuthor(rollRequester.getDisplayName(server), null, rollRequester.getAvatar())
                .setColor(Color.decode(String.valueOf((int) rollRequester.getId())));
        if (!Strings.isNullOrEmpty(answer.getContent())) {
            builder.setDescription(StringUtils.abbreviate(encodeUTF8(answer.getContent()), 4096)); //https://discord.com/developers/docs/resources/channel#embed-limits
        }

        if (answer.getFields().size() > 25) {
            log.error("Number of dice results was {} and was reduced", answer.getFields().size());
        }
        List<Answer.Field> limitedList = answer.getFields().stream().limit(25).collect(ImmutableList.toImmutableList()); //https://discord.com/developers/docs/resources/channel#embed-limits
        for (Answer.Field field : limitedList) {
            builder.addField(StringUtils.abbreviate(encodeUTF8(field.getName()), 256), //https://discord.com/developers/docs/resources/channel#embed-limits
                    StringUtils.abbreviate(encodeUTF8(field.getValue()), 1024), //https://discord.com/developers/docs/resources/channel#embed-limits
                    field.isInline());
        }
        return Mono.fromFuture(textChannel.sendMessage(builder));
    }

    protected Mono<Message> createButtonMessage(@NonNull TextChannel channel,
                                                @NonNull String buttonMessage,
                                                @NonNull List<ComponentRowDefinition> buttons) {
        return Mono.fromFuture(channel.sendMessage(buttonMessage, MessageComponentConverter.messageComponent2MessageLayout(buttons)));
    }

    protected Mono<Void> handleException(@NonNull String errorMessage,
                                         @NonNull Throwable throwable) {
        if (throwable instanceof MissingPermissionsException) {
            //todo need to stop the execution of the other actions
            log.info(String.format("Missing permissions: %s", errorMessage));
            return answerOnError(PERMISSION_ERROR_MESSAGE);
        } else {
            log.error(errorMessage, throwable);
        }
        return Mono.empty();
    }

    protected abstract Mono<Void> answerOnError(String message);


    protected Mono<Void> deleteMessage(TextChannel textChannel, long messageId) {
        return Mono.fromFuture(textChannel.getMessageById(messageId))
                .filter(m -> !m.isPinned())
                .flatMap(m -> Mono.fromFuture(m.delete()))
                .onErrorResume(t -> handleException("Error on deleting message", t));
    }

}
