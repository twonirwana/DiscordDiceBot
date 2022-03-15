package de.janno.discord.bot.cache;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.BotMetrics;
import io.micrometer.core.instrument.Tags;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
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
public class ButtonMessageCache {

    private final Cache<Long, Set<ButtonWithConfigHash>> channel2ButtonMessageIds = CacheBuilder.newBuilder()
            .maximumSize(10_000)
            .build();

    public ButtonMessageCache(String systemName) {
        globalRegistry.gaugeMapSize(BotMetrics.METRIC_PREFIX + "channelInCache", Tags.of("system", systemName), channel2ButtonMessageIds.asMap());
    }

    @VisibleForTesting
    public Map<Long, Set<ButtonWithConfigHash>> getCacheContent() {
        return channel2ButtonMessageIds.asMap().entrySet().stream()
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .map(b -> new ButtonWithConfigHash(b.buttonId, b.getConfigHash()))
                        .collect(ImmutableSet.toImmutableSet())));
    }

    public void addChannelWithButton(long channelId, long buttonId, int configHash) {
        try {
            Set<ButtonWithConfigHash> buttonIds = channel2ButtonMessageIds.get(channelId, ConcurrentSkipListSet::new);
            ButtonWithConfigHash newEntry = new ButtonWithConfigHash(buttonId, configHash);
            buttonIds.add(newEntry);
        } catch (ExecutionException e) {
            log.error("Error in putting buttonId into cache: ", e);
        }
    }

    public void removeButtonFromChannel(long channelId, long buttonId, int configHash) {
        try {
            Set<ButtonWithConfigHash> buttonIds = channel2ButtonMessageIds.get(channelId, ConcurrentSkipListSet::new);
            buttonIds.remove(new ButtonWithConfigHash(buttonId, configHash));
        } catch (ExecutionException e) {
            log.error("Error in removing buttonId into cache: ", e);
        }
    }

    public List<Long> getAllWithoutOneAndRemoveThem(long channelId, long buttonToKeepId, int configHash) {
        try {
            Set<ButtonWithConfigHash> buttonIdCache = channel2ButtonMessageIds.get(channelId, ConcurrentSkipListSet::new);
            return buttonIdCache.stream()
                    .filter(bc -> buttonToKeepId != bc.getButtonId())
                    .filter(bc -> configHash == bc.getConfigHash())
                    .peek(buttonIdCache::remove)
                    .map(ButtonWithConfigHash::getButtonId)
                    .collect(Collectors.toList());
        } catch (ExecutionException e) {
            log.error("Error in getting button ids from cache: ", e);
        }
        return ImmutableList.of();
    }


    @Value
    public static class ButtonWithConfigHash implements Comparable<ButtonWithConfigHash> {

        long buttonId;
        int configHash;

        @Override
        public int compareTo(ButtonWithConfigHash o) {
            int retVal = Long.compare(buttonId, o.getButtonId());
            if (retVal != 0) {
                return retVal;
            }
            return Integer.compare(configHash, o.configHash);
        }
    }
}
