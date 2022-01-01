package de.janno.discord.command;

import discord4j.common.util.Snowflake;
import lombok.Value;

@Value
public class ButtonWithConfigHash implements Comparable<ButtonWithConfigHash> {
    Snowflake buttonId;
    int configHash;

    @Override
    public int compareTo(ButtonWithConfigHash o) {
        int retVal = Long.compare(buttonId.asLong(), o.getButtonId().asLong());
        if (retVal != 0) {
            return retVal;
        }
        return Integer.compare(configHash, o.configHash);
    }
}
