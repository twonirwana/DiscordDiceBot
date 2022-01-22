package de.janno.discord.discord4j;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.api.Answer;
import de.janno.discord.api.IDiscordAdapter;
import de.janno.discord.api.MissingPermissionException;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Color;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public abstract class DiscordAdapter implements IDiscordAdapter {

    protected static final String PERMISSION_ERROR_MESSAGE = "Missing permission, see https://github.com/twonirwana/DiscordDiceBot for help";

    //needed to correctly show utf8 characters in discord
    private static String encodeUTF8(@NonNull String in) {
        return new String(in.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    protected EmbedCreateSpec createEmbedMessageWithReference(
            @NonNull Answer answer,
            @NonNull Member rollRequester) {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                .title(StringUtils.abbreviate(encodeUTF8(answer.getTitle()), 256)) //https://discord.com/developers/docs/resources/channel#embed-limits
                .author(rollRequester.getDisplayName(), null, rollRequester.getAvatarUrl())
                .color(Color.of(rollRequester.getId().hashCode()));

        if (!Strings.isNullOrEmpty(answer.getContent())) {
            builder.description(StringUtils.abbreviate(encodeUTF8(answer.getContent()), 4096)); //https://discord.com/developers/docs/resources/channel#embed-limits

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

    //todo retry on server error class
    protected Mono<Void> handleException(@NonNull String errorMessage, @NonNull Throwable throwable, boolean ignoreMissing, @Nullable Message triggeringMessage) {
        if (throwable instanceof ClientException) {
            ClientException clientException = (ClientException) throwable;
            if (clientException.getStatus().code() == 404 && ignoreMissing) {
                log.trace(errorMessage, clientException);
            } else if (clientException.getStatus().code() == 403 && triggeringMessage != null) {
                log.trace(errorMessage, clientException);
                //todo find better solution than sending the Mono.error to immediately terminate the mono
                return triggeringMessage.edit().withContentOrNull(PERMISSION_ERROR_MESSAGE).then(Mono.error(new MissingPermissionException()));
            } else {
                log.error("{}: {}{}", errorMessage,
                        clientException.getResponse().status(),
                        getClientExceptionShortString(clientException));
            }
        } else {
            log.error(errorMessage, throwable);
        }
        return Mono.empty();
    }

    private String getClientExceptionShortString(ClientException clientException) {
        return String.format("%s%s", clientException.getResponse().status(),
                clientException.getErrorResponse().map(er -> " with response " + er.getFields()).orElse(""));
    }

}
