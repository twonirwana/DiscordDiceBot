package de.janno.discord.bot.command;

import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
public class ConfigAndState<C extends RollConfig, S extends StateData> {

    @NonNull
    UUID configUUID;

    @NonNull
    C config;

    @NonNull
    State<S> state;
}
