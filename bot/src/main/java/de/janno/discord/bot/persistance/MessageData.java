package de.janno.discord.bot.persistance;

import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.StateData;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

@Value
public class MessageData {

    long channelId;
    long messageId;

    @NonNull
    String commandId;

    @NonNull
    Config config;

    @Nullable
    StateData state;
}
