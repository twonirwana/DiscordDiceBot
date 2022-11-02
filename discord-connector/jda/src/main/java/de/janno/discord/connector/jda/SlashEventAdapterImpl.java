package de.janno.discord.connector.jda;

import com.google.common.collect.ImmutableSet;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class SlashEventAdapterImpl extends DiscordAdapterImpl implements SlashEventAdaptor {

    @NonNull
    private final SlashCommandInteractionEvent event;
    @NonNull
    private final Mono<Requester> requesterMono;
    private final long channelId;
    @NonNull
    private final String commandString;
    private final Long guildId;

    public SlashEventAdapterImpl(@NonNull SlashCommandInteractionEvent event, @NonNull Mono<Requester> requesterMono) {
        this.event = event;
        this.requesterMono = requesterMono;
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
        return checkPermission(event.getMessageChannel(), event.getGuild());
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
    public Mono<Void> reply(@NonNull String message, boolean ephemeral) {
        return createMonoFrom(() -> event.reply(message).setEphemeral(ephemeral))
                .onErrorResume(t -> handleException("Error on replay", t, true).ofType(InteractionHook.class))
                .then();
    }

    @Override
    public Mono<Void> replyEmbed(@NonNull EmbedDefinition embedDefinition, boolean ephemeral) {
        //todo combine with DiscordAdapter.createEmbedMessageWithReference
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setDescription(embedDefinition.getDescription());
        embedDefinition.getFields().forEach(f -> embedBuilder.addField(f.getName(), f.getValue(), f.isInline()));
        return createMonoFrom(() -> event.replyEmbeds(ImmutableSet.of(embedBuilder.build())).setEphemeral(ephemeral))
                .onErrorResume(t -> handleException("Error on replay ephemeral", t, true).ofType(InteractionHook.class))
                .then();
    }

    @Override
    public Mono<Long> createButtonMessage(@NonNull MessageDefinition messageDefinition) {
        return createButtonMessage(event.getMessageChannel(), messageDefinition)
                .onErrorResume(t -> handleException("Error on creating button message", t, false).ofType(Message.class))
                .map(Message::getIdLong);
    }

    @Override
    public Mono<Void> createResultMessageWithEventReference(EmbedDefinition answer) {
        return createEmbedMessageWithReference(event.getMessageChannel(),
                answer,
                Optional.ofNullable(event.getMember()).map(Member::getEffectiveName).orElse(event.getUser().getName()),
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
    public Mono<Requester> getRequester() {
        return requesterMono;
    }

    @Override
    public Mono<Long> deleteMessage(long messageId, boolean deletePinned) {
        return deleteMessage(event.getMessageChannel(), messageId, deletePinned);
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
                .onErrorResume(t -> handleException("Error on reply to slash", t, false).ofType(InteractionHook.class))
                .flatMap(i -> createMonoFrom(i::deleteOriginal)
                        .onErrorResume(t -> handleException("Error on deleting reply", t, true)));


    }
}
