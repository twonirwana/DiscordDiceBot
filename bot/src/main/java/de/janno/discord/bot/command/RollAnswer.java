package de.janno.discord.bot.command;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public EmbedOrMessageDefinition toEmbedOrMessageDefinition() {
        if (errorMessage != null) {
            EmbedOrMessageDefinition.Type type = answerFormatType == AnswerFormatType.full ? EmbedOrMessageDefinition.Type.EMBED : EmbedOrMessageDefinition.Type.MESSAGE;
            return EmbedOrMessageDefinition.builder()
                    .title("Error in `%s`".formatted(expression))
                    .descriptionOrContent(errorMessage)
                    .type(type)
                    .build();
        }

        return switch (answerFormatType) {
            case full -> {
                if (multiRollResults != null) {
                    yield EmbedOrMessageDefinition.builder()
                            .title(Optional.ofNullable(expressionLabel).orElse(expression))
                            .fields(multiRollResults.stream()
                                    .limit(25) //max number of embedFields
                                    .map(r -> new EmbedOrMessageDefinition.Field(
                                            "%s ⇒ %s".formatted(r.getExpression(), r.getResult()),
                                            r.getRollDetails(),
                                            false))
                                    .collect(ImmutableList.toImmutableList()))
                            .type(EmbedOrMessageDefinition.Type.EMBED)
                            .build();

                } else {
                    final String description;
                    if (expressionLabel != null) {
                        description = Joiner.on(": ").skipNulls().join(expression, rollDetails);
                    } else {
                        description = Optional.ofNullable(rollDetails).orElse("");
                    }
                    yield EmbedOrMessageDefinition.builder()
                            .title("%s ⇒ %s".formatted(Optional.ofNullable(expressionLabel).orElse(expression), result))
                            .descriptionOrContent(description)
                            .type(EmbedOrMessageDefinition.Type.EMBED)
                            .build();
                }
            }
            case compact -> {
                final String description;
                if (multiRollResults != null) {
                    description = "__**%s**__\n%s".formatted(Optional.ofNullable(expressionLabel).orElse(expression),
                            multiRollResults.stream()
                                    .map(r -> "\t\t__**%s ⇒ %s**__ %s".formatted(r.getExpression(), r.getResult(), r.getRollDetails()))
                                    .collect(Collectors.joining("\n")));
                } else {
                    String descriptionDetails = "";
                    if (expressionLabel != null) {
                        descriptionDetails += expression;
                        if (rollDetails != null) {
                            descriptionDetails += ": ";
                            descriptionDetails += rollDetails;
                        }
                    } else {
                        descriptionDetails += Optional.ofNullable(rollDetails).orElse("");
                    }

                    description = "__**%s ⇒ %s**__ %s".formatted(Optional.ofNullable(expressionLabel).orElse(expression),
                            result,
                            Strings.isNullOrEmpty(descriptionDetails) ? "" : " %s".formatted(descriptionDetails)
                    );

                }

                yield EmbedOrMessageDefinition.builder()
                        .descriptionOrContent(description)
                        .type(EmbedOrMessageDefinition.Type.MESSAGE)
                        .build();
            }
            case minimal -> {
                final String description;
                if (multiRollResults != null) {
                    description = "%s: %s".formatted(Optional.ofNullable(expressionLabel).orElse(expression),
                            multiRollResults.stream()
                                    .map(r -> "%s ⇒ %s".formatted(r.getExpression(), r.getResult()))
                                    .collect(Collectors.joining(", ")));
                } else {
                    description = "%s ⇒ %s".formatted(Optional.ofNullable(expressionLabel).orElse(expression),
                            result
                    );

                }

                yield EmbedOrMessageDefinition.builder()
                        .descriptionOrContent(description)
                        .type(EmbedOrMessageDefinition.Type.MESSAGE)
                        .build();
            }
        };

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
