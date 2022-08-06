package de.janno.discord.bot.command.sumCustomSet;

import de.janno.discord.bot.command.IConfig;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

@Value
public class SumCustomSetConfig implements IConfig {
    @NonNull
    List<LabelAndDiceExpression> labelAndExpression;
    Long answerTargetChannelId;

    @Override
    public String toShortString() {
        return Stream.concat(labelAndExpression.stream()
                                .map(LabelAndDiceExpression::toShortString),
                        Stream.of(targetChannelToString(answerTargetChannelId)))
                .toList()
                .toString();
    }
}
