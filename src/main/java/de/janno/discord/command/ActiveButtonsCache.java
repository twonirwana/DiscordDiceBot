package de.janno.discord.command;

import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class ActiveButtonsCache {

    private final Map<Snowflake, Set<Snowflake>> channel2ButtonMessageIds = new ConcurrentHashMap<>();

    public void addChannelWithButton(Snowflake channelId, Snowflake buttonId) {
        channel2ButtonMessageIds.putIfAbsent(channelId, new ConcurrentSkipListSet<>());
        channel2ButtonMessageIds.get(channelId).add(buttonId);
    }

    public List<Snowflake> getAllWithoutOneAndRemoveThem(Snowflake channelId, Snowflake buttonToKeepId) {
        List<Snowflake> allButtonsWithoutTheOneToKeep = new ArrayList<>(Optional.ofNullable(channel2ButtonMessageIds.get(channelId))
                .orElse(new HashSet<>()));
        allButtonsWithoutTheOneToKeep.remove(buttonToKeepId);
        allButtonsWithoutTheOneToKeep.forEach(channel2ButtonMessageIds.get(channelId)::remove);
        return allButtonsWithoutTheOneToKeep;
    }

    public void removeChannel(Snowflake channelId) {
        channel2ButtonMessageIds.remove(channelId);
    }

}
