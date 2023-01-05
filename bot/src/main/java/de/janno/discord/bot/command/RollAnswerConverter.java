package de.janno.discord.bot.command;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RollAnswerConverter {
    private static final Set<AnswerFormatType> EMBED_ANSWER_TYPES = ImmutableSet.of(AnswerFormatType.full, AnswerFormatType.full_with_image, AnswerFormatType.without_expression_with_image, AnswerFormatType.without_expression);

    private static EmbedOrMessageDefinition.Type getMessageType(AnswerFormatType answerFormatType) {
        return EMBED_ANSWER_TYPES.contains(answerFormatType) ? EmbedOrMessageDefinition.Type.EMBED : EmbedOrMessageDefinition.Type.MESSAGE;
    }

    static public EmbedOrMessageDefinition toEmbedOrMessageDefinition(RollAnswer rollAnswer) {
        final EmbedOrMessageDefinition.Type type = getMessageType(rollAnswer.getAnswerFormatType());
        if (rollAnswer.getErrorMessage() != null) {
            return EmbedOrMessageDefinition.builder()
                    .title("Error in `%s`".formatted(rollAnswer.getExpression()))
                    .descriptionOrContent(rollAnswer.getErrorMessage())
                    .type(type)
                    .build();
        }

        return switch (rollAnswer.getAnswerFormatType()) {
            case full_with_image, full -> {
                if (rollAnswer.getMultiRollResults() != null) {
                    yield EmbedOrMessageDefinition.builder()
                            .title(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse(rollAnswer.getExpression()))
                            .fields(rollAnswer.getMultiRollResults().stream()
                                    .limit(25) //max number of embedFields
                                    .map(r -> new EmbedOrMessageDefinition.Field(
                                            "%s ⇒ %s".formatted(r.getExpression(), r.getResult()),
                                            r.getRollDetails(),
                                            false))
                                    .collect(ImmutableList.toImmutableList()))
                            .type(type)
                            .build();

                } else {
                    final File file = rollAnswer.getAnswerFormatType() == AnswerFormatType.full_with_image ? rollAnswer.getFile() : null;
                    final String diceDetailsString = file != null ? null : rollAnswer.getRollDetails();
                    final String description;
                    if (rollAnswer.getExpressionLabel() != null) {
                        description = Joiner.on(": ").skipNulls().join(rollAnswer.getExpression(), diceDetailsString);
                    } else {
                        description = Optional.ofNullable(diceDetailsString).orElse("");
                    }
                    yield EmbedOrMessageDefinition.builder()
                            .title("%s ⇒ %s".formatted(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse(rollAnswer.getExpression()), rollAnswer.getResult()))
                            .descriptionOrContent(description)
                            .file(file)
                            .type(type)
                            .build();
                }
            }
            case without_expression_with_image, without_expression -> {
                if (rollAnswer.getMultiRollResults() != null) {
                    yield EmbedOrMessageDefinition.builder()
                            .title(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse("Roll"))
                            .fields(rollAnswer.getMultiRollResults().stream()
                                    .limit(25) //max number of embedFields
                                    .map(r -> new EmbedOrMessageDefinition.Field(
                                            r.getResult(),
                                            r.getRollDetails(),
                                            false))
                                    .collect(ImmutableList.toImmutableList()))
                            .type(type)
                            .build();

                } else {
                    final File file = rollAnswer.getAnswerFormatType() == AnswerFormatType.without_expression_with_image ? rollAnswer.getFile() : null;
                    final String diceDetailsString = file != null ? null : rollAnswer.getRollDetails();
                    final String description = Optional.ofNullable(diceDetailsString).orElse("");
                    yield EmbedOrMessageDefinition.builder()
                            .title("%s ⇒ %s".formatted(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse("Roll"), rollAnswer.getResult()))
                            .descriptionOrContent(description)
                            .type(type)
                            .file(file)
                            .build();
                }
            }
            case compact -> {
                final String description;
                if (rollAnswer.getMultiRollResults() != null) {
                    description = "__**%s**__\n%s".formatted(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse(rollAnswer.getExpression()),
                            rollAnswer.getMultiRollResults().stream()
                                    .map(r -> "\t\t__**%s ⇒ %s**__ %s".formatted(r.getExpression(), r.getResult(), r.getRollDetails()))
                                    .collect(Collectors.joining("\n")));
                } else {
                    String descriptionDetails = "";
                    if (rollAnswer.getExpressionLabel() != null) {
                        descriptionDetails += rollAnswer.getExpression();
                        if (rollAnswer.getRollDetails() != null) {
                            descriptionDetails += ": ";
                            descriptionDetails += rollAnswer.getRollDetails();
                        }
                    } else {
                        descriptionDetails += Optional.ofNullable(rollAnswer.getRollDetails()).orElse("");
                    }

                    description = "__**%s ⇒ %s**__ %s".formatted(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse(rollAnswer.getExpression()),
                            rollAnswer.getResult(),
                            Strings.isNullOrEmpty(descriptionDetails) ? "" : " %s".formatted(descriptionDetails)
                    );

                }

                yield EmbedOrMessageDefinition.builder()
                        .descriptionOrContent(description)
                        .type(type)
                        .build();
            }
            case minimal -> {
                final String description;
                if (rollAnswer.getMultiRollResults() != null) {
                    description = "%s: %s".formatted(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse(rollAnswer.getExpression()),
                            rollAnswer.getMultiRollResults().stream()
                                    .map(r -> "%s ⇒ %s".formatted(r.getExpression(), r.getResult()))
                                    .collect(Collectors.joining(", ")));
                } else {
                    description = "%s ⇒ %s".formatted(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse(rollAnswer.getExpression()),
                            rollAnswer.getResult()
                    );

                }

                yield EmbedOrMessageDefinition.builder()
                        .descriptionOrContent(description)
                        .type(type)
                        .build();
            }
        };

    }
}
