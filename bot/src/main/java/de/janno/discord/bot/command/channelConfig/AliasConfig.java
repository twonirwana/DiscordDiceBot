package de.janno.discord.bot.command.channelConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.Config;
import lombok.NonNull;
import lombok.Value;


import java.util.List;
import java.util.stream.Collectors;

import static de.janno.discord.bot.command.channelConfig.ChannelConfigCommand.NAME_VALUE_DELIMITER;

@Value
public class AliasConfig implements Config {

    @NonNull
    List<Alias> aliasList;

    
    String name;

    @JsonCreator
    public AliasConfig(@NonNull @JsonProperty("aliasList") List<Alias> aliasList, @JsonProperty("name") String name) {
        this.aliasList = aliasList;
        this.name = name;
    }

    @Override
    public String toCommandOptionsString() {
        return aliasList.stream().map(Alias::toCommandOptionsString).collect(Collectors.joining(NAME_VALUE_DELIMITER));
    }
}
