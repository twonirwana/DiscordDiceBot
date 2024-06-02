package de.janno.discord.bot.command;

import com.google.common.base.Joiner;
import de.janno.discord.bot.command.reroll.DieIdAndValue;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Value
@Builder
public class RollAnswer {
    @NonNull
    AnswerFormatType answerFormatType;
    @NonNull
    String expression;
    @Nullable
    String expressionLabel;
    @Nullable
    String result;
    @Nullable
    String rollDetails;
    @Nullable
    String errorMessage;
    @Nullable
    String errorLocation;
    @Nullable
    List<RollResults> multiRollResults;
    @Nullable
    Supplier<? extends InputStream> image;
    @Nullable
    String warning;
    @NonNull
    @Singular
    //todo change to random element?
    List<DieIdAndValue> dieIdAndValues;

    public String toShortString() {
        String fieldStringList = Optional.ofNullable(multiRollResults)
                .map(f -> f.stream()
                        .map(RollResults::toShortString)
                        .toList().toString())
                .orElse(null);

        return String.format("%s=%s", expression,
                        Joiner.on(",").skipNulls().join(result, StringUtils.abbreviate(rollDetails, 100), errorMessage, warning, fieldStringList))
                .replace("\n", " ");
    }

    @Value
    public static class RollResults {
        @NonNull
        String expression;
        @NonNull
        String result;
        @NonNull
        String rollDetails;

        public String toShortString() {
            return String.format("%s %s%s", expression, result, rollDetails);
        }

    }
}
