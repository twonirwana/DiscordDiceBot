package de.janno.discord.bot.command.sumDiceSet;

import de.janno.discord.bot.command.IConfig;
import lombok.Value;

@Value
public class SumDiceSetConfig implements IConfig {
    Long answerTargetChannelId;

    @Override
    public String toShortString() {
        return String.format("[%s]", targetChannelToString(answerTargetChannelId));
    }
}
