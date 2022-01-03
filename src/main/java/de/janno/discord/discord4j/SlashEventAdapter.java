package de.janno.discord.discord4j;

import de.janno.discord.cache.ActiveButtonsCache;
import de.janno.discord.command.ISlashEventAdaptor;
import de.janno.discord.dice.DiceResult;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Slf4j
public class SlashEventAdapter extends DiscordAdapter implements ISlashEventAdaptor {

    private final ChatInputInteractionEvent event;

    public SlashEventAdapter(ChatInputInteractionEvent event) {
        this.event = event;
    }

    @Override
    public Optional<ApplicationCommandInteractionOption> getOption(String optionName) {
        return event.getOption(optionName);
    }

    @Override
    public Mono<Void> reply(String message) {
        return event.reply(message)
                .onErrorResume(t -> {
                    log.error("Error on replay", t);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> replyEphemeral(EmbedCreateSpec embedCreateSpec) {
        return event.reply().withEphemeral(true).withEmbeds(embedCreateSpec)
                .onErrorResume(t -> {
                    log.error("Error on replay to slash help command", t);
                    return Mono.empty();
                });

    }

    @Override
    public Mono<Void> createButtonMessage(ActiveButtonsCache activeButtonsCache, @NonNull String buttonMessage, @NonNull List<LayoutComponent> buttons, @NonNull List<String> config) {
        return event.getInteraction().getChannel().ofType(TextChannel.class)
                .flatMap(channel -> createButtonMessage(activeButtonsCache, channel, buttonMessage, buttons, config))
                .onErrorResume(t -> {
                    log.error("Error on creating button message", t);
                    return Mono.empty();
                })
                .ofType(Void.class);
    }

    @Override
    public Mono<Void> createResultMessageWithEventReference(List<DiceResult> diceResults) {
        return event.getInteraction().getChannel().ofType(TextChannel.class)
                .flatMap(channel -> channel.createMessage(createEmbedMessageWithReference(diceResults, event.getInteraction().getMember().orElseThrow())))
                .onErrorResume(t -> {
                    log.error("Error on creating dice result message", t);
                    return Mono.empty();
                })
                .ofType(Void.class);
    }
}
