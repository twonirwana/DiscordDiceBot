package de.janno.discord.bot.command;

import com.google.common.base.Joiner;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

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
    List<RollResults> multiRollResults;
    @Nullable
    Supplier<? extends InputStream> image;
    @Nullable
    String warning;

    public String toShortString() {
        String fieldStringList = Optional.ofNullable(multiRollResults)
                .map(f -> f.stream()
                        .map(RollResults::toShortString)
                        .toList().toString())
                .orElse(null);
        return String.format("%s=%s", expression,
                        Joiner.on(",").skipNulls().join(result, rollDetails, errorMessage, warning, fieldStringList))
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
