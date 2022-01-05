package de.janno.discord.command;

import de.janno.discord.cache.ButtonMessageCache;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface ISlashEventAdaptor extends IDiscordAdapter {
    Optional<ApplicationCommandInteractionOption> getOption(String actionStart);

    Mono<Void> reply(String message);

    Mono<Void> replyEphemeral(EmbedCreateSpec embedCreateSpec);

    Mono<Void> createButtonMessage(ButtonMessageCache buttonMessageCache,
                                   @NonNull String buttonMessage,
                                   @NonNull List<LayoutComponent> buttons,
                                   int configHash);

}
