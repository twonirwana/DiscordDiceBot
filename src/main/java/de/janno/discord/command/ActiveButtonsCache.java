package de.janno.discord.command;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class ActiveButtonsCache {

    Cache<Snowflake, Set<ButtonWithConfigHash>> channel2ButtonMessageIds = CacheBuilder.newBuilder()
            .maximumSize(10_000)
            .build();

    public ActiveButtonsCache(String systemName) {
        SharedMetricRegistries.getDefault().register("channelInCache." + systemName, (Gauge<Long>) () -> channel2ButtonMessageIds.size());
    }

    public void addChannelWithButton(Snowflake channelId, Snowflake buttonId, List<String> config) {
        try {
            Set<ButtonWithConfigHash> buttonIds = channel2ButtonMessageIds.get(channelId, ConcurrentSkipListSet::new);
            buttonIds.add(new ButtonWithConfigHash(buttonId, config.hashCode()));
        } catch (ExecutionException e) {
            log.error("Error in putting buttonId into cache: ", e);
        }
    }

    public List<Snowflake> getAllWithoutOneAndRemoveThem(Snowflake channelId, Snowflake buttonToKeepId, List<String> config) {
        try {
            Set<ButtonWithConfigHash> buttonIdCache = channel2ButtonMessageIds.get(channelId, ConcurrentSkipListSet::new);
            return buttonIdCache.stream()
                    .filter(bc -> !buttonToKeepId.equals( bc.getButtonId()))
                    .filter(bc -> config.hashCode() == bc.getConfigHash())
                    .peek(buttonIdCache::remove)
                    .map(ButtonWithConfigHash::getButtonId)
                    .collect(Collectors.toList());
        } catch (ExecutionException e) {
            log.error("Error in getting button ids from cache: ", e);
        }
        return ImmutableList.of();
    }

    public void removeChannel(Snowflake channelId) {
        channel2ButtonMessageIds.invalidate(channelId);
    }
}
