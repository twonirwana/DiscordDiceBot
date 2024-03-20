package de.janno.discord.bot;

import de.janno.discord.connector.api.MessageState;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.Getter;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

public class SlashEventAdaptorMock implements SlashEventAdaptor {
    public static final long CHANNEL_ID = 1L;
    public static final Long GUILD_ID = null;
    public final List<CommandInteractionOption> commandInteractionOptions;
    @Getter
    private final List<String> actions = new ArrayList<>();
    @Getter
    private final List<EmbedOrMessageDefinition> allReplays = new ArrayList<>();
    private final long userId;
    private final Locale userLocale;

    @Getter
    private Optional<ButtonEventAdaptorMock> firstButtonEventMockOfLastButtonMessage = Optional.empty();

    public SlashEventAdaptorMock(List<CommandInteractionOption> commandInteractionOptions) {
        this(commandInteractionOptions, 0L);
    }

    public SlashEventAdaptorMock(List<CommandInteractionOption> commandInteractionOptions, Locale userLocale) {
        this(commandInteractionOptions, 0L, userLocale);
    }

    public SlashEventAdaptorMock(List<CommandInteractionOption> commandInteractionOptions, long userId) {
        this(commandInteractionOptions, userId, Locale.ENGLISH);
    }

    public SlashEventAdaptorMock(List<CommandInteractionOption> commandInteractionOptions, long userId, Locale userLocale) {
        this.commandInteractionOptions = commandInteractionOptions;
        this.userId = userId;
        this.userLocale = userLocale;
    }

    public List<String> getSortedActions() {
        return actions.stream().sorted().toList();
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
    public @NonNull Mono<Void> deleteMessageById(long messageId) {
        actions.add(String.format("deleteMessageById: %d", messageId));
        return Mono.just("").then();
    }

    @Override
    public Optional<String> checkPermissions(@NonNull Locale userLocal) {
        return Optional.empty();
    }

    @Override
    public Optional<CommandInteractionOption> getOption(@NonNull String optionName) {
        return commandInteractionOptions.stream()
                .flatMap(s -> Stream.concat(Stream.of(s), s.getOptions().stream()))
                .flatMap(s -> Stream.concat(Stream.of(s), s.getOptions().stream()))
                .filter(s -> Objects.equals(s.getName(), optionName))
                .findFirst();
    }

    @Override
    public List<CommandInteractionOption> getOptions() {
        return commandInteractionOptions;
    }

    @Override
    public Mono<Void> replyWithEmbedOrMessageDefinition(@NonNull EmbedOrMessageDefinition messageDefinition, boolean ephemeral) {
        allReplays.add(messageDefinition);
        actions.add(String.format("replyWithEmbedOrMessageDefinition: %s", messageDefinition));
        return Mono.just("").then();
    }

    @Override
    public @NonNull Mono<Long> createMessageWithoutReference(@NonNull EmbedOrMessageDefinition messageDefinition) {
        actions.add(String.format("createMessageWithoutReference: %s", messageDefinition));
        long messageId = 1L;
        messageDefinition.getComponentRowDefinitions().stream()
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId)
                .findFirst().ifPresent(id ->
                        firstButtonEventMockOfLastButtonMessage = Optional.of(new ButtonEventAdaptorMock(id, messageId))
                );
        return Mono.just(messageId);
    }

    @Override
    public long getChannelId() {
        return CHANNEL_ID;
    }

    @Override
    public String getCommandString() {
        return "commandString";
    }

    @Override
    public Requester getRequester() {
        return new Requester("invokingUser", "channelName", "guildName", "[0 / 1]", userLocale);
    }

    @Override
    public Mono<Long> createResultMessageWithReference(EmbedOrMessageDefinition answer) {
        actions.add(String.format("createResultMessageWithReference: %s", answer));
        return Mono.just(1L);
    }

    @Override
    public boolean isValidAnswerChannel(long channelId) {
        return true;
    }

    @Override
    public Mono<Void> acknowledgeAndRemoveSlash() {
        actions.add("acknowledgeAndRemoveSlash");
        return Mono.just("").then();
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    public @NonNull ParallelFlux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds) {
        actions.add(String.format("getMessagesState: %s", messageIds));
        return Flux.fromIterable(messageIds).parallel().map(id -> new MessageState(id, false, true, true, OffsetDateTime.now().minusSeconds(id * 5)));
    }
}
