package de.janno.discord.bot;

import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import lombok.NonNull;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ButtonEventAdaptorMock implements ButtonEventAdaptor {

    public static final long CHANNEL_ID = 1L;
    public static final long GUILD_ID = 1L;
    private final String customId;
    private final long massageId;
    private final AtomicLong messageIdCounter;
    private final List<String> actions = new ArrayList<>();

    private final Set<Long> pinnedMessageIds;

    public ButtonEventAdaptorMock(String commandId, String buttonValue, AtomicLong messageIdCounter, Set<Long> pinnedMessageIds) {
        this.customId = commandId + BottomCustomIdUtils.CUSTOM_ID_DELIMITER + buttonValue;
        this.massageId = messageIdCounter.get();
        this.messageIdCounter = messageIdCounter;
        this.pinnedMessageIds = pinnedMessageIds;
    }

    public List<String> getActions() {
        return actions;
    }

    @Override
    public String getCustomId() {
        return customId;
    }

    @Override
    public long getMessageId() {
        return massageId;
    }

    @Override
    public long getChannelId() {
        return CHANNEL_ID;
    }

    @Override
    public boolean isPinned() {
        return pinnedMessageIds.contains(massageId);
    }

    @Override
    public String getInvokingGuildMemberName() {
        return "invokingUser";
    }

    @Override
    public Mono<Void> editMessage(@Nullable String message, @Nullable List<ComponentRowDefinition> componentRowDefinitions) {
        actions.add(String.format("editMessage: message:%s, buttonValues=%s", message, Optional.ofNullable(componentRowDefinitions).stream()
                .flatMap(Collection::stream)
                .flatMap(r -> r.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId)
                .map(BottomCustomIdUtils::getButtonValueFromCustomId)
                .collect(Collectors.joining(","))));
        return Mono.just("").then();
    }

    @Override
    public Mono<Long> createButtonMessage(MessageDefinition messageDefinition) {
        actions.add(String.format("createButtonMessage: content=%s, buttonValues=%s", messageDefinition.getContent(), messageDefinition.getComponentRowDefinitions().stream()
                .flatMap(r -> r.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId)
                .map(BottomCustomIdUtils::getButtonValueFromCustomId)
                .collect(Collectors.joining(","))));
        return Mono.just(messageIdCounter.incrementAndGet());
    }

    @Override
    public List<LabelAndCustomId> getAllButtonIds() {
        throw new NotImplementedException();
    }

    @Override
    public String getMessageContent() {
        throw new NotImplementedException();
    }

    @Override
    public Requester getRequester() {
        return new Requester("invokingUser", "channelName", "guildName", "[0 / 1]");
    }

    @Override
    public Optional<String> checkPermissions(Long answerTargetChannelId) {
        return Optional.empty();
    }

    @Override
    public Mono<Void> createResultMessageWithEventReference(EmbedDefinition answer, Long targetChannelId) {
        actions.add(String.format("createAnswer: title=%s, description=%s, fieldValues:%s, answerChannel:%s", answer.getTitle(), answer.getDescription(), answer.getFields().stream()
                .map(EmbedDefinition.Field::getValue)
                .collect(Collectors.joining(",")), targetChannelId));
        return Mono.just("").then();
    }

    @Override
    public Long getGuildId() {
        return GUILD_ID;
    }

    @Override
    public Mono<Void> reply(@NonNull String message, boolean ephemeral) {
        actions.add(String.format("reply: %s", message));

        return Mono.just("").then();
    }

    @Override
    public Mono<Long> deleteMessage(long messageId, boolean deletePinned) {
        if (pinnedMessageIds.contains(messageId) && !deletePinned) {
            return Mono.empty();
        }
        actions.add(String.format("deleteMessage: %s", messageId));
        return Mono.just(messageId);
    }


}
