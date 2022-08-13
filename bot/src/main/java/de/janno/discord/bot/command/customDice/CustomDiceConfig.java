package de.janno.discord.bot.command.customDice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.LabelAndDiceExpression;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString
public class CustomDiceConfig extends Config {
    @NonNull
    private final List<LabelAndDiceExpression> labelAndExpression;

    @JsonCreator
    public CustomDiceConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                            @JsonProperty("labelAndExpression") @NonNull List<LabelAndDiceExpression> labelAndExpression) {
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
