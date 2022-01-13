package de.janno.discord.command;

import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface ISlashEventAdaptor extends IDiscordAdapter {
    String checkPermissions();

    Optional<ApplicationCommandInteractionOption> getOption(String actionStart);

    Mono<Void> reply(String message);

    Mono<Void> replyEphemeral(EmbedCreateSpec embedCreateSpec);

    Mono<Long> createButtonMessage(@NonNull String buttonMessage, @NonNull List<LayoutComponent> buttons);

    Long getChannelId();

    String getCommandString();

    Mono<Void> deleteMessage(long messageId);
}
