package de.janno.discord.connector.javacord;

import de.janno.discord.connector.api.ISlashEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class SlashEventAdapter extends DiscordAdapter implements ISlashEventAdaptor {

    @NonNull
    private final SlashCommandCreateEvent event;
    @NonNull
    private final Mono<Requester> requesterMono;
    private final long channelId;
    @NonNull
    private final String commandString;

    public SlashEventAdapter(SlashCommandCreateEvent event, @NonNull Mono<Requester> requesterMono) {
        this.event = event;
        this.requesterMono = requesterMono;
        this.channelId = event.getSlashCommandInteraction().getChannel().map(DiscordEntity::getId).orElseThrow();
        this.commandString = String.format("`/%s %s`", event.getSlashCommandInteraction().getCommandName(), event.getSlashCommandInteraction().getOptions().stream()
                .map(a -> optionToString(a.getName(), a.getOptions(), a.getStringValue().orElse(null)))
                .collect(Collectors.joining(" ")));
    }

    @Override
    public String checkPermissions() {
        List<String> checks = new ArrayList<>();
        if (!event.getSlashCommandInteraction().getChannel().map(TextChannel::canYouWrite).orElse(false)) {
            checks.add("'SEND_MESSAGES'");
        }
        if (!event.getSlashCommandInteraction().getChannel().map(TextChannel::canYouEmbedLinks).orElse(false)) {
            checks.add("'EMBED_LINKS'");
        }
        if (checks.isEmpty()) {
            return null;
        }
        String result = String.format("The bot is missing the permission: %s. It will not work correctly without it. Please check the guild and channel permissions for the bot", String.join(" and ", checks));
        log.info(result);
        return result;
    }

    @Override
    public Optional<CommandInteractionOption> getOption(@NonNull String optionName) {
        return event.getSlashCommandInteraction().getOptions().stream()
                .filter(o -> optionName.equals(o.getName()))
                .findFirst()
                .map(ApplicationCommandConverter::slashCommandInteractionOption2CommandInteractionOption);
    }

    @Override
    public Mono<Void> reply(@NonNull String message) {
        return Mono.fromFuture(event.getSlashCommandInteraction().createImmediateResponder()
                        .setContent(message)
                        .respond())
                .onErrorResume(t -> handleException("Error on replay", t, true).ofType(InteractionOriginalResponseUpdater.class))
                .then();
    }

    @Override
    public Mono<Void> replyEmbed(@NonNull EmbedDefinition embedDefinition, boolean replyEmbed) {
        //todo combine with DiscordAdapter.createEmbedMessageWithReference
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setDescription(embedDefinition.getDescription());
        embedDefinition.getFields().forEach(f -> embedBuilder.addField(f.getName(), f.getValue(), f.isInline()));
        InteractionImmediateResponseBuilder builder = event.getSlashCommandInteraction().createImmediateResponder()
                .addEmbed(embedBuilder);
        if (replyEmbed) {
            builder = builder.setFlags(InteractionCallbackDataFlag.EPHEMERAL);
        }
        return Mono.fromFuture(builder.respond())
                .onErrorResume(t -> handleException("Error on replay ephemeral", t, true).ofType(InteractionOriginalResponseUpdater.class))
                .then();

    }

    @Override
    public Mono<Long> createButtonMessage(@NonNull MessageDefinition messageDefinition) {
        return createButtonMessage(event.getInteraction().getChannel().orElseThrow(), messageDefinition)
                .onErrorResume(t -> handleException("Error on creating button message", t, false).ofType(Message.class))
                .map(DiscordEntity::getId);
    }

    @Override
    public Mono<Void> createResultMessageWithEventReference(EmbedDefinition answer) {
        return createEmbedMessageWithReference(event.getInteraction().getChannel().orElseThrow(),
                answer,
                event.getInteraction().getUser(),
                event.getSlashCommandInteraction().getServer().orElse(null))
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

    private String optionToString(@NonNull String name, @NonNull List<SlashCommandInteractionOption> options, @Nullable String value) {
        if (options.isEmpty() && value == null) {
            return name;
        }
        String out = name;
        if (value != null) {
            out = String.format("%s:%s", name, value);
        }
        String optionsString = options.stream()
                .map(a -> optionToString(a.getName(), a.getOptions(), a.getStringValue().orElse(null)))
                .collect(Collectors.joining(" "));
        if (!optionsString.isEmpty()) {
            out = String.format("%s %s", out, optionsString);
        }

        return out;
    }


    @Override
    public Mono<Requester> getRequester() {
        return requesterMono;
    }

    @Override
    public Mono<Void> deleteMessage(long messageId) {
        return deleteMessage(event.getInteraction().getChannel().orElseThrow(), messageId);
    }

    @Override
    protected Mono<Void> answerOnError(String message) {
        return reply(message);
    }
}
