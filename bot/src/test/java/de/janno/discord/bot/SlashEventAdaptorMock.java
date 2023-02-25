package de.janno.discord.bot;

import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class SlashEventAdaptorMock implements SlashEventAdaptor {
    public static final long CHANNEL_ID = 1L;
    public static final long GUILD_ID = 1L;
    public final List<CommandInteractionOption> commandInteractionOptions;
    private final List<String> actions = new ArrayList<>();

    public SlashEventAdaptorMock(List<CommandInteractionOption> commandInteractionOptions) {
        this.commandInteractionOptions = commandInteractionOptions;
    }

    public List<String> getActions() {
        return actions;
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
        actions.add(String.format("delete: %d", messageId));
        return Mono.just("").then();
    }

    @Override
    public Optional<String> checkPermissions() {
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
    public Mono<Void> replyEmbed(@NonNull EmbedOrMessageDefinition embedOrMessageDefinition, boolean ephemeral) {
        actions.add(String.format("replyEmbed: %s", embedOrMessageDefinition));
        return Mono.just("").then();
    }

    @Override
    public @NonNull Mono<Long> createButtonMessage(@NonNull MessageDefinition messageDefinition) {
        actions.add(String.format("createButtonMessage: %s", messageDefinition));
        return Mono.just(1L);
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
        return new Requester("invokingUser", "channelName", "guildName", "[0 / 1]");
    }

    @Override
    public Mono<Void> createResultMessageWithEventReference(EmbedOrMessageDefinition answer) {
        actions.add(String.format("createResultMessageWithEventReference: %s", answer));
        return Mono.just("").then();
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
        return 0;
    }
}
