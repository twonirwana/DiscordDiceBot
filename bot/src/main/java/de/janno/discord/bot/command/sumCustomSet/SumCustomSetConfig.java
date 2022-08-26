package de.janno.discord.bot.command.sumCustomSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString
public class SumCustomSetConfig extends Config {
    @NonNull
    private final List<ButtonIdLabelAndDiceExpression> labelAndExpression;

    @JsonCreator
    public SumCustomSetConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                              @JsonProperty("labelAndExpression") @NonNull List<ButtonIdLabelAndDiceExpression> labelAndExpression) {
        super(answerTargetChannelId);
        this.labelAndExpression = labelAndExpression;
    }

    @Override
    public String toShortString() {
        return Stream.concat(labelAndExpression.stream()
                                .map(ButtonIdLabelAndDiceExpression::toShortString),
                        Stream.of(getTargetChannelShortString()))
                .toList()
                .toString();
    }
}
