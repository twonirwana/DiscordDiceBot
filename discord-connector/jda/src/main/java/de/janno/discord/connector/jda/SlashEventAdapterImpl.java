package de.janno.discord.connector.jda;

import de.janno.discord.connector.api.MessageState;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
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
    /**
     * null if the button event is not in a guild
     */
    private final Long guildId;
    @NonNull
    private final String invokingGuildMemberName;


    public SlashEventAdapterImpl(@NonNull SlashCommandInteractionEvent event, @NonNull Requester requester) {
        this.event = event;
        this.requester = requester;
        this.channelId = event.getChannel().getIdLong();
        this.commandString = String.format("`%s`", event.getCommandString());
        this.guildId = Optional.ofNullable(event.getGuild()).map(Guild::getIdLong).orElse(null);
        this.invokingGuildMemberName = Optional.ofNullable(event.getMember()).map(Member::getEffectiveName).orElse(event.getUser().getName());
    }

    @Override
    public Long getGuildId() {
        return guildId;
    }

    @Override
    public Optional<String> checkPermissions(@NonNull Locale userLocale) {
        Guild guild = event.isFromAttachedGuild() ? event.getGuild() : null;
        return checkPermission(event.getMessageChannel(), guild, false, userLocale);
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
        return replyWithEmbedOrMessageDefinition(event, messageDefinition, ephemeral,
                messageDefinition.isUserReference() ? event.getUser().getId() : null)
                .onErrorResume(t -> handleException("Error on replay", t, true).ofType(InteractionHook.class))
                .then();
    }


    @Override
    public @NonNull Mono<Long> sendMessage(@NonNull EmbedOrMessageDefinition messageDefinition) {
        final MessageChannel targetChannel = Optional.ofNullable(messageDefinition.getSendToOtherChannelId())
                .flatMap(id -> Optional.ofNullable(event.getGuild())
                        .map(g -> (MessageChannel) g.getChannelById(GuildMessageChannel.class, id)))
                .orElse(event.getInteraction().getMessageChannel());
        return sendMessageWithOptionalReference(targetChannel,
                messageDefinition,
                messageDefinition.isUserReference() ? invokingGuildMemberName : null,
                messageDefinition.isUserReference() ? event.getUser().getAsMention() : null,
                messageDefinition.isUserReference() ? Optional.ofNullable(event.getMember()).map(Member::getEffectiveAvatarUrl).orElse(event.getUser().getEffectiveAvatarUrl()) : null,
                messageDefinition.isUserReference() ? event.getUser().getId() : null)
                .onErrorResume(t -> handleException("Error on sending message", t, false).ofType(Message.class))
                .map(Message::getIdLong);
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
                .map(g -> (MessageChannel) g.getChannelById(GuildMessageChannel.class, channelId))
                .isPresent();
    }

    @Override
    public long getUserId() {
        return event.getUser().getIdLong();
    }

    @Override
    public boolean isUserInstallInteraction() {
        return event.isFromGuild() && !event.isFromAttachedGuild(); //has guild but it is not attached
    }

    @Override
    protected @NonNull MessageChannel getMessageChannel() {
        return event.getMessageChannel();
    }

    @Override
    protected @NonNull String getErrorRequester() {
        return requester.toLogString();
    }

    @Override
    public @NonNull ParallelFlux<MessageState> getMessagesState(@NonNull Collection<Long> messageIds) {
        return getMessagesState(event.getMessageChannel(), messageIds);
    }
}
