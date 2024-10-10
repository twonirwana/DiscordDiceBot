package de.janno.discord.bot;

import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.MessageState;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.Getter;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static de.janno.discord.connector.api.BottomCustomIdUtils.CUSTOM_ID_DELIMITER;

public class ButtonEventAdaptorMock implements ButtonEventAdaptor {

    public static final long CHANNEL_ID = 1L;
    public static final Long GUILD_ID = null;
    private final String customId;
    private final long massageId;
    private final AtomicLong messageIdCounter;
    @Getter
    private final List<String> actions = new ArrayList<>();
    private final Set<Long> pinnedMessageIds;
    private final String invokingUser;
    private final EmbedOrMessageDefinition eventMessage;
    @Getter
    private final List<EmbedOrMessageDefinition> sendMessages = new ArrayList<>();

    public ButtonEventAdaptorMock(String commandId, String buttonValue, UUID configUUID, AtomicLong messageIdCounter, Set<Long> pinnedMessageIds) {
        this(commandId, buttonValue, configUUID, messageIdCounter, pinnedMessageIds, "invokingUser");
    }

    public ButtonEventAdaptorMock(String commandId, String buttonValue, EmbedOrMessageDefinition eventMessage) {
        this(commandId, buttonValue, null, new AtomicLong(), null, "invokingUser", eventMessage);

    }

    public ButtonEventAdaptorMock(String commandId, String buttonValue, UUID configUUID, AtomicLong messageIdCounter, Set<Long> pinnedMessageIds, String invokingUser, EmbedOrMessageDefinition eventMessage) {
        this.customId = configUUID != null ? BottomCustomIdUtils.createButtonCustomId(commandId, buttonValue, configUUID) : BottomCustomIdUtils.createButtonCustomIdWithoutConfigId(commandId, buttonValue);
        this.massageId = messageIdCounter.get();
        this.messageIdCounter = messageIdCounter;
        this.pinnedMessageIds = pinnedMessageIds;
        this.invokingUser = invokingUser;
        this.eventMessage = eventMessage;
    }

    public ButtonEventAdaptorMock(String commandId, String buttonValue, UUID configUUID, AtomicLong messageIdCounter, Set<Long> pinnedMessageIds, String invokingUser) {
        this(commandId, buttonValue, configUUID, messageIdCounter, pinnedMessageIds, invokingUser, null);

    }

    public ButtonEventAdaptorMock(String commandId, String buttonValue, AtomicLong messageIdCounter) {
        this.customId = commandId + CUSTOM_ID_DELIMITER + buttonValue;
        this.massageId = messageIdCounter.get();
        this.messageIdCounter = messageIdCounter;
        this.pinnedMessageIds = Set.of();
        this.invokingUser = "invokingUser";
        this.eventMessage = null;
    }

    public ButtonEventAdaptorMock(String customId, long messageId) {
        this.customId = customId;
        this.massageId = messageId;
        this.messageIdCounter = new AtomicLong();
        this.pinnedMessageIds = Set.of();
        this.invokingUser = "invokingUser";
        this.eventMessage = null;
    }

    public ButtonEventAdaptorMock(String legacyId) {
        this.customId = legacyId;
        this.massageId = 0;
        this.messageIdCounter = new AtomicLong(0);
        this.pinnedMessageIds = Set.of();
        this.invokingUser = "invokingUser";
        this.eventMessage = null;
    }

    public List<String> getSortedActions() {
        return actions.stream().sorted(String::compareTo).toList();
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
    public long getUserId() {
        return 0;
    }

    @Override
    public boolean isPinned() {
        return pinnedMessageIds.contains(massageId);
    }

    @Override
    public String getInvokingGuildMemberName() {
        return invokingUser;
    }

    @Override
    public Mono<Void> editMessage(@Nullable String message, @Nullable List<ComponentRowDefinition> componentRowDefinitions) {
        actions.add(String.format("editMessage: message:%s, buttonValues=%s", message, Optional.ofNullable(componentRowDefinitions).stream()
                .flatMap(Collection::stream)
                .flatMap(r -> r.getButtonDefinitions().stream())
                .map(bd -> BottomCustomIdUtils.getButtonValueFromCustomId(bd.getId()) + (bd.isDisabled() ? "!" : ""))
                .collect(Collectors.joining(","))));
        return Mono.just("").then();
    }

    @Override
    public @NonNull Mono<Long> sendMessage(@NonNull EmbedOrMessageDefinition messageDefinition) {
        actions.add(String.format("sendMessage: %s", messageDefinition));
        sendMessages.add(messageDefinition);
        return Mono.just(messageIdCounter.incrementAndGet());
    }

    @Override
    public Requester getRequester() {
        return new Requester("invokingUser", "channelName", "guildName", "[0 / 1]", Locale.ENGLISH, null);
    }

    @Override
    public Optional<String> checkPermissions(Long answerTargetChannelId, @NonNull Locale userLocale) {
        return Optional.empty();
    }

    @Override
    public Long getGuildId() {
        return GUILD_ID;
    }

    @Override
    public @NonNull Mono<Void> reply(@NonNull String message, boolean ephemeral) {
        actions.add(String.format("reply: %s", message));

        return Mono.just("").then();
    }

    @Override
    public @NonNull ParallelFlux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds) {
        actions.add(String.format("getMessagesState: %s", messageIds));
        return Flux.fromIterable(messageIds).parallel().map(id -> new MessageState(id, pinnedMessageIds.contains(id), true, true, OffsetDateTime.now().minusSeconds(id * 5)));
    }

    @Override
    public @NonNull OffsetDateTime getMessageCreationTime() {
        return OffsetDateTime.now().minusSeconds(5);
    }

    @Override
    public @NonNull Mono<Void> deleteMessageById(long messageId) {
        actions.add(String.format("deleteMessageById: %s", messageId));
        return Mono.empty();
    }

    @Override
    public Mono<Void> acknowledgeAndDeleteOriginal() {
        actions.add("acknowledgeAndDeleteOriginal");
        return Mono.empty();
    }

    @Override
    public @NonNull Mono<Void> acknowledge() {
        actions.add("acknowledge");
        return Mono.empty();
    }

    @Override
    public EmbedOrMessageDefinition getMessageDefinitionOfEventMessageWithoutButtons() {
        actions.add("getMessageDefinitionOfEventMessageWithoutButtons");
        return eventMessage;
    }
}
