package de.janno.discord.command;

import com.google.gson.Gson;
import de.janno.discord.persistance.SerializedChannelConfig;
import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ConfigRegistry<T> {

    private static final Gson gson = new Gson();
    private final Map<Snowflake, ChannelConfig<T>> channelConfigs = new ConcurrentHashMap<>();
    private final Function<T, String> serializer;
    private final Function<String, T> deserializer;
    private final String configForName;

    public ConfigRegistry(String configForName, Class<T> configClass) {
        this.configForName = configForName;
        if (configClass == null) {
            serializer = s -> null;
            deserializer = s -> null;
        } else {
            serializer = gson::toJson;
            deserializer = s -> gson.fromJson(s, configClass);
        }
    }

    public List<SerializedChannelConfig> getAllChannelConfig() {
        return channelConfigs.values().stream()
                .map(config -> new SerializedChannelConfig(configForName, config.getChannelId().asString(), serializer.apply(config.getConfig())))
                .collect(Collectors.toList());
    }

    public void setAllChannelConfig(List<SerializedChannelConfig> newChannelConfigs) {
        newChannelConfigs.forEach(c -> channelConfigs.put(Snowflake.of(c.getChannelId()), new ChannelConfig<>(Snowflake.of(c.getChannelId()), new CopyOnWriteArrayList<>(), deserializer.apply(c.getConfig()))));
    }

    public void addChannelWithButton(Snowflake channelId, Snowflake buttonId) {
        channelConfigs.putIfAbsent(channelId, new ChannelConfig<>(channelId, new CopyOnWriteArrayList<>(), null));
        channelConfigs.get(channelId).getButtonIds().add(buttonId);
    }

    public List<Snowflake> getAllWithoutOneAndRemoveThem(Snowflake channelId, Snowflake buttonToKeepId) {
        List<Snowflake> allButtonsWithoutTheOneToKeep = new ArrayList<>(Optional.ofNullable(channelConfigs.get(channelId))
                .map(ChannelConfig::getButtonIds)
                .orElse(new ArrayList<>()));
        allButtonsWithoutTheOneToKeep.remove(buttonToKeepId);
        channelConfigs.get(channelId).getButtonIds().removeAll(allButtonsWithoutTheOneToKeep);
        return allButtonsWithoutTheOneToKeep;
    }

    public void removeChannel(Snowflake channelId) {
        channelConfigs.remove(channelId);
    }

    public boolean channelIsRegistered(Snowflake channelId) {
        return channelConfigs.containsKey(channelId);
    }

    public T getConfigForChannelOrDefault(Snowflake channelId, T defaultValue) {
        return Optional.ofNullable(channelConfigs.get(channelId)).map(ChannelConfig::getConfig).orElse(defaultValue);
    }

    public void setChannelConfig(Snowflake channelId, T config) {
        channelConfigs.putIfAbsent(channelId, new ChannelConfig<>(channelId, new CopyOnWriteArrayList<>(), null));
        channelConfigs.get(channelId).setConfig(config);
    }
}
