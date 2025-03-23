package de.janno.discord.bot.persistance;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

/**
 * The persisted information for a single button message. Containing the optional internal state of the command.
 * Each button message need to have a MessageData so we can use it to delete the button message even on concurrent actions.
 */
@Value
@AllArgsConstructor
public class MessageDataDTO {

    @NonNull
    UUID configUUID;
    Long guildId;
    long channelId;
    long messageId;
    @NonNull
    String commandId;
    @NonNull
    String stateDataClassId;
    String stateData;

}
