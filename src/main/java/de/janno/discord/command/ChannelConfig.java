package de.janno.discord.command;

import discord4j.common.util.Snowflake;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ChannelConfig<T> {
    Snowflake channelId;
    List<Snowflake> buttonIds;
    T config;
}
