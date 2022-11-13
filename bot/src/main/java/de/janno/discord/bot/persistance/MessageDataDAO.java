package de.janno.discord.bot.persistance;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MessageDataDAO {

    @NonNull Optional<MessageDataDTO> getDataForMessage(long channelId, long messageId);

    @NonNull List<IdAndCreationDate> getAllMessageIdsForConfig(@NonNull UUID configUUID);

    void deleteDataForMessage(long channelId, long messageId);

    @NonNull Set<Long> deleteDataForChannel(long channelId);

    void saveMessageData(@NonNull MessageDataDTO messageData);

    void updateCommandConfigOfMessage(long channelId, long messageId, @NonNull String stateDataClassId, @Nullable String stateData);
}
