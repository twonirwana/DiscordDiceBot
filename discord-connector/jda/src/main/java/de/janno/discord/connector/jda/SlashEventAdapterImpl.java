package de.janno.discord.connector.jda;

import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class SlashEventAdapterImpl extends DiscordAdapterImpl implements SlashEventAdaptor {

    @NonNull
    private final SlashCommandInteractionEvent event;
    @NonNull
    private final Requester requester;
    private final long channelId;
    @NonNull
    private final String commandString;
    private final Long guildId;

    public SlashEventAdapterImpl(@NonNull SlashCommandInteractionEvent event, @NonNull Requester requester) {
        this.event = event;
        this.requester = requester;
        this.channelId = event.getChannel().getIdLong();
        this.commandString = String.format("`%s`", event.getCommandString());
        this.guildId = Optional.ofNullable(event.getGuild()).map(Guild::getIdLong).orElse(null);

    }

    @Override
    public Long getGuildId() {
        return guildId;
    }

    @Override
    public Optional<String> checkPermissions() {
        return checkPermission(event.getMessageChannel(), event.getGuild(), false);
    }

    @Override
    public Optional<CommandInteractionOption> getOption(@NonNull String optionName) {
        if (optionName.equals(event.getSubcommandGroup()) || optionName.equals(event.getSubcommandName())) {
            return Optional.of(CommandInteractionOption.builder()
                    .name(optionName)
                    .options(event.getOptions().stream()
                            .map(ApplicationCommandConverter::optionMapping2CommandInteractionOption)
                            .collect(Collectors.toList()))
                    .build());
        }
        return Optional.ofNullable(event.getOption(optionName))
                .map(ApplicationCommandConverter::optionMapping2CommandInteractionOption);
    }

    @Override
    public List<CommandInteractionOption> getOptions() {
        return event.getOptions().stream()
                .map(ApplicationCommandConverter::optionMapping2CommandInteractionOption)
                .toList();
    }

    @Override
    public @NonNull Mono<Void> reply(@NonNull String message, boolean ephemeral) {
        return createMonoFrom(() -> event.reply(StringUtils.abbreviate(encodeUTF8(message), Message.MAX_CONTENT_LENGTH)).setEphemeral(ephemeral))
                .onErrorResume(t -> handleException("Error on replay", t, true).ofType(InteractionHook.class))
                .then();
    }

    @Override
    public Mono<Void> replyWithEmbedOrMessageDefinition(@NonNull EmbedOrMessageDefinition messageDefinition, boolean ephemeral) {
        return replyWithMessage(event, messageDefinition, ephemeral)
                .onErrorResume(t -> handleException("Error on replay", t, true).ofType(InteractionHook.class))
                .then();
    }


    @Override
    public @NonNull Mono<Long> createMessageWithoutReference(@NonNull EmbedOrMessageDefinition messageDefinition) {
        return createMessageWithoutReference(event.getMessageChannel(), messageDefinition)
                .onErrorResume(t -> handleException("Error on creating button message", t, false).ofType(Message.class))
                .map(Message::getIdLong);
    }

    @Override
    public Mono<Void> createResultMessageWithEventReference(EmbedOrMessageDefinition answer) {
        return createMessageWithReference(event.getMessageChannel(),
                answer,
                Optional.ofNullable(event.getMember()).map(Member::getEffectiveName).orElse(event.getUser().getName()),
                event.getMember().getAsMention(),
                Optional.ofNullable(event.getMember()).map(Member::getEffectiveAvatarUrl).orElse(event.getUser().getEffectiveAvatarUrl()),
                event.getUser().getId())
                .onErrorResume(t -> handleException("Error on creating answer message", t, false).ofType(Message.class))
                .ofType(Void.class);
    }

    @Override
    public long getChannelId() {
        return channelId;
    }

    @Override
    public @NonNull String getCommandString() {
        return commandString;
    }


    @Override
    public @NonNull Requester getRequester() {
        return requester;
    }

    @Override
    public boolean isValidAnswerChannel(long channelId) {
        return Optional.ofNullable(event.getGuild())
                .map(g -> g.getChannelById(MessageChannel.class, channelId))
                .isPresent();
    }

    @Override
    public Mono<Void> acknowledgeAndRemoveSlash() {
        return createMonoFrom(() -> event.reply("..."))
                .onErrorResume(t -> handleException("Error on reply to slash", t, true).ofType(InteractionHook.class))
                .flatMap(i -> createMonoFrom(i::deleteOriginal)
                        .onErrorResume(t -> handleException("Error on deleting reply", t, true)));
    }


    @Override
    public long getUserId() {
        return event.getUser().getIdLong();
    }

    @Override
    protected @NonNull MessageChannel getMessageChannel() {
        return event.getMessageChannel();
    }

    @Override
    protected @NonNull String getGuildAndChannelName() {
        return requester.toLogString();
    }
}
