package de.janno.discord.bot.command.channelConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Value
public class AliasConfig {

    @NonNull List<Alias> aliasList;

    @JsonCreator
    public AliasConfig(@NotNull @JsonProperty("aliasList") List<Alias> aliasList) {
        this.aliasList = aliasList;
    }

}
