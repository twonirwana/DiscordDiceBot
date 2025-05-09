package de.janno.discord.bot;

import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.MessageState;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.Data;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static de.janno.discord.connector.api.BottomCustomIdUtils.CUSTOM_ID_DELIMITER;

@Data
public class ButtonEventAdaptorMock implements ButtonEventAdaptor {

    public static final long CHANNEL_ID = 1L;
    public static final Long GUILD_ID = null;
    public static final Long USER_ID = 0L;
    private final String customId;
    private final long massageId;
    private final AtomicLong messageIdCounter;
    private final List<String> actions = new ArrayList<>();
    private final Set<Long> pinnedMessageIds;
    private final String invokingUser;
    private final EmbedOrMessageDefinition eventMessage;
    private final List<EmbedOrMessageDefinition> sendMessages = new ArrayList<>();
    private List<ComponentRowDefinition> editedComponentRowDefinition;
    private String permissionCheck = null;

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

    public static ButtonEventAdaptorMock ofCustomId(String customId, long messageId) {
        return new ButtonEventAdaptorMock(BottomCustomIdUtils.getCommandNameFromCustomId(customId),
                BottomCustomIdUtils.getButtonValueFromCustomId(customId),
                BottomCustomIdUtils.getConfigUUIDFromCustomId(customId).orElseThrow(),
                new AtomicLong(messageId), Set.of(), "invokingUser");
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
        return USER_ID;
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
    public @NonNull Mono<Void> editMessage(String message, List<ComponentRowDefinition> componentRowDefinitions) {
        actions.add(String.format("editMessage: message:%s, buttons=%s", message, componentRowDefinitions));
        this.editedComponentRowDefinition = componentRowDefinitions;
        return Mono.just("").then();
    }

    @Override
    public @NonNull Mono<Long> sendMessage(@NonNull EmbedOrMessageDefinition messageDefinition) {
        actions.add(String.format("sendMessage: %s", messageDefinition));
        sendMessages.add(messageDefinition);
        return Mono.just(messageIdCounter.incrementAndGet());
    }

    @Override
    public @NonNull Requester getRequester() {
        return new Requester("invokingUser", "channelName", "guildName", "[0 / 1]", Locale.ENGLISH, null);
    }

    @Override
    public @NonNull Optional<String> checkPermissions(Long answerTargetChannelId, @NonNull Locale userLocale) {
        return Optional.ofNullable(permissionCheck);
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
    public @NonNull Mono<Void> acknowledgeAndDeleteOriginal() {
        actions.add("acknowledgeAndDeleteOriginal");
        return Mono.empty();
    }

    @Override
    public @NonNull Mono<Void> acknowledge() {
        actions.add("acknowledge");
        return Mono.empty();
    }

    @Override
    public @NonNull EmbedOrMessageDefinition getMessageDefinitionOfEventMessageWithoutButtons() {
        actions.add("getMessageDefinitionOfEventMessageWithoutButtons");
        return eventMessage;
    }
}
