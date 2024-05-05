package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RollAnswerConverterTest {

    static Stream<Arguments> generateConversionData() {
        return Stream.of(
                //full
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.full)
                        .expression("2d6=")
                        .expressionLabel("label")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), "label ⇒ 3", "2d6=: [1,2]", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.full)
                        .expression("2d6=")
                        .errorLocation("__d__")
                        .errorMessage("error")
                        .build(), "Error in __d__", "error", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.full)
                        .expression("2d6=")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), "2d6= ⇒ 3", "[1,2]", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.full)
                        .expression("2d6=")
                        .rollDetails("[1,2]")
                        .result("3")
                        .image(() -> new ByteArrayInputStream(new byte[0]))
                        .build(), "2d6= ⇒ 3", "", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), true),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.full)
                        .expression("2d6=,3d6=")
                        .multiRollResults(ImmutableList.of(
                                new RollAnswer.RollResults("2d6=", "3", "[1,2]"),
                                new RollAnswer.RollResults("3d6=", "6", "[1,2,3]")
                        ))
                        .build(), "2d6=,3d6=", null, EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(
                        new EmbedOrMessageDefinition.Field("2d6= ⇒ 3", "[1,2]", false),
                        new EmbedOrMessageDefinition.Field("3d6= ⇒ 6", "[1,2,3]", false)
                ), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.full)
                        .expression("2d6=,3d6=")
                        .expressionLabel("label")
                        .multiRollResults(ImmutableList.of(
                                new RollAnswer.RollResults("2d6=", "3", "[1,2]"),
                                new RollAnswer.RollResults("3d6=", "6", "[1,2,3]")
                        ))
                        .build(), "label", null, EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(
                        new EmbedOrMessageDefinition.Field("2d6= ⇒ 3", "[1,2]", false),
                        new EmbedOrMessageDefinition.Field("3d6= ⇒ 6", "[1,2,3]", false)
                ), false),

                //without_expression
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.without_expression)
                        .expression("2d6=")
                        .expressionLabel("label")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), "label ⇒ 3", "[1,2]", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.without_expression)
                        .expression("2d6=")
                        .errorLocation("__d__")
                        .errorMessage("error")
                        .build(), "Error in __d__", "error", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.without_expression)
                        .expression("2d6=")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), "Roll ⇒ 3", "[1,2]", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.without_expression)
                        .expression("2d6=")
                        .image(() -> new ByteArrayInputStream(new byte[0]))
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), "Roll ⇒ 3", "", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), true),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.without_expression)
                        .expression("Roll")
                        .multiRollResults(ImmutableList.of(
                                new RollAnswer.RollResults("2d6=", "3", "[1,2]"),
                                new RollAnswer.RollResults("3d6=", "6", "[1,2,3]")
                        ))
                        .build(), "Roll", null, EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(
                        new EmbedOrMessageDefinition.Field("3", "[1,2]", false),
                        new EmbedOrMessageDefinition.Field("6", "[1,2,3]", false)
                ), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.without_expression)
                        .expression("2d6=,3d6=")
                        .expressionLabel("label")
                        .multiRollResults(ImmutableList.of(
                                new RollAnswer.RollResults("2d6=", "3", "[1,2]"),
                                new RollAnswer.RollResults("3d6=", "6", "[1,2,3]")
                        ))
                        .build(), "label", null, EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(
                        new EmbedOrMessageDefinition.Field("3", "[1,2]", false),
                        new EmbedOrMessageDefinition.Field("6", "[1,2,3]", false)
                ), false),

                //only_dice
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.only_dice)
                        .expression("2d6=")
                        .expressionLabel("label")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), null, "[1,2]", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.only_dice)
                        .expression("2d6=")
                        .errorMessage("error")
                        .errorLocation("__d__")
                        .build(), "Error in __d__", "error", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.only_dice)
                        .expression("2d6=")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), null, "[1,2]", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.only_dice)
                        .expression("2d6=")
                        .rollDetails("[1,2]")
                        .image(() -> new ByteArrayInputStream(new byte[0]))
                        .result("3")
                        .build(), null, "", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), true),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.only_dice)
                        .expression("2d6=,3d6=")
                        .multiRollResults(ImmutableList.of(
                                new RollAnswer.RollResults("2d6=", "3", "[1,2]"),
                                new RollAnswer.RollResults("3d6=", "6", "[1,2,3]")
                        ))
                        .build(), null, "[1,2]\n[1,2,3]", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                                .answerFormatType(AnswerFormatType.only_dice)
                                .expression("2d6=,3d6=")
                                .multiRollResults(ImmutableList.of(
                                        new RollAnswer.RollResults("2d6=", "3", "[1,2]"),
                                        new RollAnswer.RollResults("3d6=", "6", "[1,2,3]")
                                ))
                                .build(), null, "[1,2]\n[1,2,3]", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of(),
                        false),

                //compact
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.compact)
                        .expression("2d6=")
                        .expressionLabel("label")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), null, "__**label ⇒ 3**__  2d6=: [1,2]", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.compact)
                        .expression("2d6=")
                        .errorMessage("error")
                        .build(), null, "error", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.compact)
                        .expression("2d6=")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), null, "__**2d6= ⇒ 3**__  [1,2]", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.compact)
                        .expression("2d6=")
                        .rollDetails("[1,2]")
                        .image(() -> new ByteArrayInputStream(new byte[0]))
                        .result("3")
                        .build(), null, "__**2d6= ⇒ 3**__  [1,2]", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.compact)
                        .expression("2d6=,3d6=")
                        .multiRollResults(ImmutableList.of(
                                new RollAnswer.RollResults("2d6=", "3", "[1,2]"),
                                new RollAnswer.RollResults("3d6=", "6", "[1,2,3]")
                        ))
                        .build(), null, """
                        __**2d6=,3d6=**__
                        \t\t__**2d6= ⇒ 3**__ [1,2]
                        \t\t__**3d6= ⇒ 6**__ [1,2,3]""", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                                .answerFormatType(AnswerFormatType.compact)
                                .expression("2d6=,3d6=")
                                .expressionLabel("label")
                                .multiRollResults(ImmutableList.of(
                                        new RollAnswer.RollResults("2d6=", "3", "[1,2]"),
                                        new RollAnswer.RollResults("3d6=", "6", "[1,2,3]")
                                ))
                                .build(), null, """
                                __**label**__
                                \t\t__**2d6= ⇒ 3**__ [1,2]
                                \t\t__**3d6= ⇒ 6**__ [1,2,3]""", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of(),
                        false),

                //minimal
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.minimal)
                        .expression("2d6=")
                        .expressionLabel("label")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), null, "label ⇒ 3", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.minimal)
                        .expression("2d6=")
                        .errorMessage("error")
                        .build(), null, "error", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.minimal)
                        .expression("2d6=")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), null, "2d6= ⇒ 3", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.minimal)
                        .expression("2d6=")
                        .rollDetails("[1,2]")
                        .image(() -> new ByteArrayInputStream(new byte[0]))
                        .result("3")
                        .build(), null, "2d6= ⇒ 3", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.minimal)
                        .expression("2d6=,3d6=")
                        .multiRollResults(ImmutableList.of(
                                new RollAnswer.RollResults("2d6=", "3", "[1,2]"),
                                new RollAnswer.RollResults("3d6=", "6", "[1,2,3]")
                        ))
                        .build(), null, "2d6=,3d6=: 2d6= ⇒ 3, 3d6= ⇒ 6", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of(), false),
                Arguments.of(RollAnswer.builder()
                                .answerFormatType(AnswerFormatType.minimal)
                                .expression("2d6=,3d6=")
                                .expressionLabel("label")
                                .multiRollResults(ImmutableList.of(
                                        new RollAnswer.RollResults("2d6=", "3", "[1,2]"),
                                        new RollAnswer.RollResults("3d6=", "6", "[1,2,3]")
                                ))
                                .build(), null, "label: 2d6= ⇒ 3, 3d6= ⇒ 6", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of(),
                        false)
        );
    }

    @ParameterizedTest(name = "{index} rollAnswer={0} -> expectedTitle={1}, expectedDescription={2}, expectedType={3}, expectedFile={4}")
    @MethodSource("generateConversionData")
    void testConversion(RollAnswer rollAnswer, String expectedTitle, String expectedDescription, EmbedOrMessageDefinition.Type expectedType, ImmutableList<EmbedOrMessageDefinition.Field> expectedFields, boolean expectedFile) {
        EmbedOrMessageDefinition embedOrMessageDefinition = RollAnswerConverter.toEmbedOrMessageDefinition(rollAnswer);
        assertThat(embedOrMessageDefinition.getTitle()).isEqualTo(expectedTitle);
        assertThat(embedOrMessageDefinition.getDescriptionOrContent()).isEqualTo(expectedDescription);
        assertThat(embedOrMessageDefinition.getType()).isEqualTo(expectedType);
        assertThat(embedOrMessageDefinition.getFields()).isEqualTo(expectedFields);
        assertThat(embedOrMessageDefinition.getImage() != null).isEqualTo(expectedFile);
    }

}