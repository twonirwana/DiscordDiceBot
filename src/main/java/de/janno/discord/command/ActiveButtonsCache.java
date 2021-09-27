package de.janno.discord.command;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ActiveButtonsCache {

    Cache<Snowflake, Set<Snowflake>> channel2ButtonMessageIds = CacheBuilder.newBuilder()
            .maximumSize(10_000)
            .build();

    public ActiveButtonsCache(String systemName) {
        SharedMetricRegistries.getDefault().register("channelInCache." + systemName, (Gauge<Long>) () -> channel2ButtonMessageIds.size());
    }

    public void addChannelWithButton(Snowflake channelId, Snowflake buttonId) {
        try {
            Set<Snowflake> buttonIds = channel2ButtonMessageIds.get(channelId, ConcurrentSkipListSet::new);
            buttonIds.add(buttonId);
        } catch (ExecutionException e) {
            log.error("Error in putting buttonId into cache: ", e);
        }
    }

    public List<Snowflake> getAllWithoutOneAndRemoveThem(Snowflake channelId, Snowflake buttonToKeepId) {
        try {
            Set<Snowflake> buttonIdCache = channel2ButtonMessageIds.get(channelId, ConcurrentSkipListSet::new);
            List<Snowflake> allButtonsWithoutTheOneToKeep = new ArrayList<>(buttonIdCache);
            allButtonsWithoutTheOneToKeep.remove(buttonToKeepId);
            allButtonsWithoutTheOneToKeep.forEach(buttonIdCache::remove);
            return allButtonsWithoutTheOneToKeep;
        } catch (ExecutionException e) {
            log.error("Error in getting button ids from cache: ", e);
        }
        return ImmutableList.of();
    }

    public void removeChannel(Snowflake channelId) {
        channel2ButtonMessageIds.invalidate(channelId);
    }
}
