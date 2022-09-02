package de.janno.discord.bot.command.holdReroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.StateData;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class HoldRerollStateData extends StateData {

    @NonNull List<Integer> currentResults;
    int rerollCounter;

    @JsonCreator
    public HoldRerollStateData(
            @JsonProperty("currentResults") @NonNull List<Integer> currentResults,
            @JsonProperty("rerollCounter") int rerollCounter) {
        this.currentResults = currentResults;
        this.rerollCounter = rerollCounter;
    }

    @JsonIgnore
    @Override
    public String getShortStringValues() {
        return String.format("%s, %s", currentResults, rerollCounter);
    }

}
