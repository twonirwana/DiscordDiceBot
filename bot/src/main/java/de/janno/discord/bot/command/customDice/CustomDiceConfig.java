package de.janno.discord.bot.command.customDice;

import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.LabelAndDiceExpression;
import lombok.*;

import java.util.List;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString
public class CustomDiceConfig extends Config {
    @NonNull
    private final List<LabelAndDiceExpression> labelAndExpression;

    public CustomDiceConfig(Long answerTargetChannelId, @NonNull List<LabelAndDiceExpression> labelAndExpression) {
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
