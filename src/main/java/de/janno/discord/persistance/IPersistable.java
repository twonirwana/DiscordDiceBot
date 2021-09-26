package de.janno.discord.persistance;

import java.util.List;

public interface IPersistable {
    String getName();

    List<SerializedChannelConfig> getChannelConfig();

    void setChannelConfig(List<SerializedChannelConfig> channelConfigs);
}
