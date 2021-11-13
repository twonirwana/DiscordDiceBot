package de.janno.discord.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import de.janno.discord.Metrics;
import discord4j.common.util.Snowflake;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.micrometer.core.instrument.Metrics.globalRegistry;


@Slf4j
/*
 The button cache allows us to resolve if two user click simultaneously on a button. This produces two answers and removes the same button message.
 The button cache allows us to access the messages that other user created and remove them too.
 */
public class ActiveButtonsCache {

    private final Cache<Snowflake, Set<ButtonWithConfigHash>> channel2ButtonMessageIds = CacheBuilder.newBuilder()
            .maximumSize(10_000)
            .build();

    public ActiveButtonsCache(String systemName) {
        globalRegistry.gaugeMapSize(Metrics.METRIC_PREFIX + "channelInCache", Tags.of("system", systemName), channel2ButtonMessageIds.asMap());
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
                    .filter(bc -> !buttonToKeepId.equals(bc.getButtonId()))
                    .filter(bc -> config.hashCode() == bc.getConfigHash())
                    .peek(buttonIdCache::remove)
                    .map(ButtonWithConfigHash::getButtonId)
                    .collect(Collectors.toList());
        } catch (ExecutionException e) {
            log.error("Error in getting button ids from cache: ", e);
        }
        return ImmutableList.of();
    }
}
