package de.janno.discord.bot.command.holdReroll;

import de.janno.discord.bot.command.State;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Getter
public class HoldRerollState extends State {
    @NonNull
    private final List<Integer> currentResults;
    private final int rerollCounter;

    public HoldRerollState(@NonNull String buttonValue, @NonNull List<Integer> currentResults, int rerollCounter) {
        super(buttonValue);
        this.currentResults = currentResults;
        this.rerollCounter = rerollCounter;
    }

    @Override
    public String toShortString() {
        return String.format("[%s, %s, %d]", getButtonValue(), currentResults, rerollCounter);
    }
}
