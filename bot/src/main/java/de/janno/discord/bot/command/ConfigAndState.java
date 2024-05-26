package de.janno.discord.bot.command;

import de.janno.discord.bot.command.reroll.Config;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
public class ConfigAndState<C extends Config, S extends StateData> {

    @NonNull
    UUID configUUID;

    @NonNull
    C config;

    @NonNull
    State<S> state;
}
