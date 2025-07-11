package de.janno.discord.bot.command;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RollAnswerConverter {
    private static final Set<AnswerFormatType> EMBED_ANSWER_TYPES = ImmutableSet.of(AnswerFormatType.full, AnswerFormatType.without_expression, AnswerFormatType.only_result, AnswerFormatType.only_dice);

    private static EmbedOrMessageDefinition.Type getMessageType(AnswerFormatType answerFormatType) {
        return EMBED_ANSWER_TYPES.contains(answerFormatType) ? EmbedOrMessageDefinition.Type.EMBED : EmbedOrMessageDefinition.Type.MESSAGE;
    }

    public static EmbedOrMessageDefinition toEmbedOrMessageDefinition(RollAnswer rollAnswer) {
        final EmbedOrMessageDefinition.Type type = getMessageType(rollAnswer.getAnswerFormatType());
        if (rollAnswer.getErrorMessage() != null) {
            if (type == EmbedOrMessageDefinition.Type.EMBED) {
                return EmbedOrMessageDefinition.builder()
                        .shortedTitle("Error in: %s".formatted(rollAnswer.getErrorLocation()))
                        .shortedDescription(rollAnswer.getErrorMessage())
                        .type(type)
                        .userReference(true)
                        .build();
            }

            return EmbedOrMessageDefinition.builder()
                    .shortedDescription(rollAnswer.getErrorMessage())
                    .type(type)
                    .userReference(true)
                    .build();
        }

        return switch (rollAnswer.getAnswerFormatType()) {
            case full -> {
                if (rollAnswer.getMultiRollResults() != null) {
                    yield EmbedOrMessageDefinition.builder()
                            .shortedTitle(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse(rollAnswer.getExpression()))
                            .fields(rollAnswer.getMultiRollResults().stream()
                                    .limit(25) //max number of embedFields
                                    .map(r -> new EmbedOrMessageDefinition.Field(
                                            "%s ⇒ %s".formatted(r.getExpression(), r.getResult()),
                                            r.getRollDetails(),
                                            false))
                                    .collect(ImmutableList.toImmutableList()))
                            .type(EmbedOrMessageDefinition.Type.EMBED)
                            .image(rollAnswer.getImage())
                            .fileAltText(getRollAltText(rollAnswer))
                            .userReference(true)
                            .build();

                } else {
                    final String diceDetailsString = rollAnswer.getImage() != null ? null : rollAnswer.getRollDetails();
                    final String description;
                    if (rollAnswer.getExpressionLabel() != null) {
                        description = Joiner.on(": ").skipNulls().join(rollAnswer.getExpression(), diceDetailsString);
                    } else {
                        description = Optional.ofNullable(diceDetailsString).orElse("");
                    }
                    yield EmbedOrMessageDefinition.builder()
                            .shortedTitle("%s ⇒ %s".formatted(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse(rollAnswer.getExpression()), rollAnswer.getResult()))
                            .shortedDescription(description)
                            .image(rollAnswer.getImage())
                            .fileAltText(getRollAltText(rollAnswer))
                            .userReference(true)
                            .type(EmbedOrMessageDefinition.Type.EMBED)
                            .build();
                }
            }
            case without_expression -> {
                if (rollAnswer.getMultiRollResults() != null) {
                    yield EmbedOrMessageDefinition.builder()
                            .shortedTitle(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse("Roll"))
                            .fields(rollAnswer.getMultiRollResults().stream()
                                    .limit(25) //max number of embedFields
                                    .map(r -> new EmbedOrMessageDefinition.Field(
                                            r.getResult(),
                                            r.getRollDetails(),
                                            false))
                                    .collect(ImmutableList.toImmutableList()))
                            .type(EmbedOrMessageDefinition.Type.EMBED)
                            .image(rollAnswer.getImage())
                            .fileAltText(getRollAltText(rollAnswer))
                            .userReference(true)
                            .build();

                } else {
                    final String diceDetailsString = rollAnswer.getImage() != null ? null : rollAnswer.getRollDetails();
                    final String description = Optional.ofNullable(diceDetailsString).orElse("");
                    yield EmbedOrMessageDefinition.builder()
                            .shortedTitle("%s ⇒ %s".formatted(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse("Roll"), rollAnswer.getResult()))
                            .shortedDescription(description)
                            .type(EmbedOrMessageDefinition.Type.EMBED)
                            .image(rollAnswer.getImage())
                            .fileAltText(getRollAltText(rollAnswer))
                            .userReference(true)
                            .build();
                }
            }
            case only_result -> {
                if (rollAnswer.getMultiRollResults() != null) {
                    yield EmbedOrMessageDefinition.builder()
                            .shortedTitle(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse("Roll"))
                            .shortedContent(rollAnswer.getMultiRollResults().stream()
                                    .map(RollAnswer.RollResults::getResult)
                                    .map("**%s**"::formatted)
                                    .collect(Collectors.joining("\n")))
                            .type(EmbedOrMessageDefinition.Type.EMBED)
                            .image(rollAnswer.getImage())
                            .fileAltText(getRollAltText(rollAnswer))
                            .userReference(true)
                            .build();

                } else {
                    yield EmbedOrMessageDefinition.builder()
                            .shortedTitle("%s ⇒ %s".formatted(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse("Roll"), rollAnswer.getResult()))
                            .type(EmbedOrMessageDefinition.Type.EMBED)
                            .image(rollAnswer.getImage())
                            .fileAltText(getRollAltText(rollAnswer))
                            .userReference(true)
                            .build();
                }
            }
            case only_dice -> {
                if (rollAnswer.getMultiRollResults() != null) {
                    final String diceDetailsString = rollAnswer.getImage() != null ? null : rollAnswer.getMultiRollResults().stream()
                            .map(RollAnswer.RollResults::getRollDetails)
                            .collect(Collectors.joining("\n"));
                    final String description = Optional.ofNullable(diceDetailsString).orElse("");
                    yield EmbedOrMessageDefinition.builder()
                            .shortedDescription(description)
                            .type(EmbedOrMessageDefinition.Type.EMBED)
                            .image(rollAnswer.getImage())
                            .fileAltText(getRollAltText(rollAnswer))
                            .userReference(true)
                            .build();
                } else {
                    final String diceDetailsString = rollAnswer.getImage() != null ? null : rollAnswer.getRollDetails();
                    final String description = Optional.ofNullable(diceDetailsString).orElse("");
                    yield EmbedOrMessageDefinition.builder()
                            .shortedDescription(description)
                            .type(EmbedOrMessageDefinition.Type.EMBED)
                            .image(rollAnswer.getImage())
                            .fileAltText(getRollAltText(rollAnswer))
                            .userReference(true)
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
                    String descriptionDetails = getDescriptionDetails(rollAnswer);

                    description = "__**%s ⇒ %s**__ %s".formatted(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse(rollAnswer.getExpression()),
                            rollAnswer.getResult(),
                            Strings.isNullOrEmpty(descriptionDetails) ? "" : " %s".formatted(descriptionDetails)
                    );

                }

                yield EmbedOrMessageDefinition.builder()
                        .shortedContent(description)
                        .type(EmbedOrMessageDefinition.Type.MESSAGE)
                        .userReference(true)
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
                        .shortedContent(description)
                        .type(EmbedOrMessageDefinition.Type.MESSAGE)
                        .userReference(true)
                        .build();
            }
        };

    }

    private static String getDescriptionDetails(RollAnswer rollAnswer) {
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
        return descriptionDetails;
    }

    private static String getRollAltText(RollAnswer rollAnswer) {
        if (rollAnswer.getMultiRollResults() != null) {
            return rollAnswer.getMultiRollResults().stream().map(RollAnswer.RollResults::getRollDetails).collect(Collectors.joining(" - "));
        }
        return rollAnswer.getRollDetails();
    }
}
