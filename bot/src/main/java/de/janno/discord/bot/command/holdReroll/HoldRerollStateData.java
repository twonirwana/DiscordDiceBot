package de.janno.discord.bot.command.holdReroll;

import de.janno.discord.bot.command.StateData;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class HoldRerollStateData implements StateData {

    @NonNull List<Integer> currentResults;
    int rerollCounter;

    @Override
    public String getShortStringValues() {
        return String.format("%s, %s", currentResults, rerollCounter);
    }

}
