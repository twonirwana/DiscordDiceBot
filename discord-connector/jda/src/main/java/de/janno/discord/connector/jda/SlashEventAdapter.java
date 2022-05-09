package de.janno.discord.connector.jda;

import com.google.common.collect.ImmutableSet;
import de.janno.discord.connector.api.ISlashEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class SlashEventAdapter extends DiscordAdapter implements ISlashEventAdaptor {

    @NonNull
    private final SlashCommandInteractionEvent event;
    @NonNull
    private final Mono<Requester> requesterMono;
    private final long channelId;
    @NonNull
    private final String commandString;

    public SlashEventAdapter(@NonNull SlashCommandInteractionEvent event, @NonNull Mono<Requester> requesterMono) {
        this.event = event;
        this.requesterMono = requesterMono;
        this.channelId = event.getChannel().getIdLong();
        this.commandString = String.format("`%s`", event.getCommandString());
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
    public Mono<Void> reply(@NonNull String message) {
        return createMonoFrom(() -> event.reply(message))
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
                event.getUser(),
                event.getGuild())
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
    public Mono<Void> deleteMessage(long messageId) {
        return deleteMessage(event.getMessageChannel(), messageId);
    }

}
