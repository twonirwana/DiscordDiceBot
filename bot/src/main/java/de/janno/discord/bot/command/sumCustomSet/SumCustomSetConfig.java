package de.janno.discord.bot.command.sumCustomSet;

import de.janno.discord.bot.command.Config;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Getter
public class SumCustomSetConfig extends Config {
    @NonNull
    private final List<LabelAndDiceExpression> labelAndExpression;

    public SumCustomSetConfig(Long answerTargetChannelId, @NonNull List<LabelAndDiceExpression> labelAndExpression) {
        super(answerTargetChannelId);
        this.labelAndExpression = labelAndExpression;
    }

    @Override
    public String toShortString() {
        return Stream.concat(labelAndExpression.stream()
                                .map(LabelAndDiceExpression::toShortString),
                        Stream.of(getTargetChannelShortString()))
                .toList()
                .toString();
    }
}
