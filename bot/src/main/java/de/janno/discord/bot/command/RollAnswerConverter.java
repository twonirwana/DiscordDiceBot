package de.janno.discord.bot.command;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;

import java.util.Optional;
import java.util.stream.Collectors;

public class RollAnswerConverter {
    static public EmbedOrMessageDefinition toEmbedOrMessageDefinition(RollAnswer rollAnswer) {
        if (rollAnswer.getErrorMessage() != null) {
            EmbedOrMessageDefinition.Type type = rollAnswer.getAnswerFormatType() == AnswerFormatType.full ? EmbedOrMessageDefinition.Type.EMBED : EmbedOrMessageDefinition.Type.MESSAGE;
            return EmbedOrMessageDefinition.builder()
                    .title("Error in `%s`".formatted(rollAnswer.getExpression()))
                    .descriptionOrContent(rollAnswer.getErrorMessage())
                    .type(type)
                    .build();
        }

        return switch (rollAnswer.getAnswerFormatType()) {
            case full -> {
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
                            .type(EmbedOrMessageDefinition.Type.EMBED)
                            .build();

                } else {
                    final String description;
                    if (rollAnswer.getExpressionLabel() != null) {
                        description = Joiner.on(": ").skipNulls().join(rollAnswer.getExpression(), rollAnswer.getRollDetails());
                    } else {
                        description = Optional.ofNullable(rollAnswer.getRollDetails()).orElse("");
                    }
                    yield EmbedOrMessageDefinition.builder()
                            .title("%s ⇒ %s".formatted(Optional.ofNullable(rollAnswer.getExpressionLabel()).orElse(rollAnswer.getExpression()), rollAnswer.getResult()))
                            .descriptionOrContent(description)
                            .type(EmbedOrMessageDefinition.Type.EMBED)
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
                        .type(EmbedOrMessageDefinition.Type.MESSAGE)
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
                        .type(EmbedOrMessageDefinition.Type.MESSAGE)
                        .build();
            }
        };

    }
}
