package de.janno.discord.connector.api;

import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;

import java.util.Locale;
import java.util.UUID;

public interface WelcomeMessageCreator {
    MessageAndConfigId getWelcomeMessage(WelcomeRequest welcomeRequest);

    void processMessageId(WelcomeRequest welcomeRequest, UUID configUUID, long messageId);

    record WelcomeRequest(Long guildId, long channelId, Locale guildLocale) {
    }

    record MessageAndConfigId(@NonNull EmbedOrMessageDefinition embedOrMessageDefinition, @NonNull UUID configUUID) {

    }
}
