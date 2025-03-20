package de.janno.discord.bot.command;

import com.google.common.base.Joiner;
import de.janno.discord.bot.command.reroll.DieIdTypeAndValue;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;


import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

@Value
@Builder(toBuilder = true)
public class RollAnswer {
    @NonNull
    AnswerFormatType answerFormatType;
    @NonNull
    String expression;
    
    String expressionLabel;
    
    String result;
    
    String rollDetails;
    
    String errorMessage;
    
    String errorLocation;
    
    List<RollResults> multiRollResults;
    
    Supplier<? extends InputStream> image;
    
    String warning;
    @NonNull
    @Singular
    List<DieIdTypeAndValue> dieIdTypeAndValues;

    public String toShortString() {
        return Joiner.on(",").skipNulls().join(expression, errorMessage).replace("\n", " ");
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
