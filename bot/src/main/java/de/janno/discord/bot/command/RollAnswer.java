package de.janno.discord.bot.command;

import com.google.common.base.Joiner;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@Value
@Builder
public class RollAnswer {
    private static final String MINUS = "\u2212";
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
    List<RollResults> multiRollResults;

    public String toShortString() {
        String fieldStringList = Optional.ofNullable(multiRollResults)
                .map(f -> f.stream()
                        .map(RollResults::toShortString)
                        .toList().toString())
                .orElse(null);
        return String.format("%s=%s", expression,
                        Joiner.on(",").skipNulls().join(result, rollDetails, errorMessage, fieldStringList))
                .replace("▢", "0")
                .replace("＋", "+")
                .replace(MINUS, "-")
                .replace("*", "");
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
