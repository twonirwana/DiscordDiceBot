package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RollAnswerTest {

    static Stream<Arguments> generateConversionData() {
        return Stream.of(
                //full
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.full)
                        .expression("2d6=")
                        .expressionLabel("label")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), "label ⇒ 3", "2d6=: [1,2]", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of()),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.full)
                        .expression("2d6=")
                        .errorMessage("error")
                        .build(), "Error in `2d6=`", "error", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of()),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.full)
                        .expression("2d6=")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), "2d6= ⇒ 3", "[1,2]", EmbedOrMessageDefinition.Type.EMBED, ImmutableList.of()),
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
                )),
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
                )),

                //compact
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.compact)
                        .expression("2d6=")
                        .expressionLabel("label")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), null, "__**label ⇒ 3**__  2d6=: [1,2]", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of()),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.compact)
                        .expression("2d6=")
                        .errorMessage("error")
                        .build(), "Error in `2d6=`", "error", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of()),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.compact)
                        .expression("2d6=")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), null, "__**2d6= ⇒ 3**__  [1,2]", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of()),
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
                        \t\t__**3d6= ⇒ 6**__ [1,2,3]""", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of()),
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
                        \t\t__**3d6= ⇒ 6**__ [1,2,3]""", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of()),

                //minimal
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.minimal)
                        .expression("2d6=")
                        .expressionLabel("label")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), null, "label ⇒ 3", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of()),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.minimal)
                        .expression("2d6=")
                        .errorMessage("error")
                        .build(), "Error in `2d6=`", "error", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of()),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.minimal)
                        .expression("2d6=")
                        .rollDetails("[1,2]")
                        .result("3")
                        .build(), null, "2d6= ⇒ 3", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of()),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.minimal)
                        .expression("2d6=,3d6=")
                        .multiRollResults(ImmutableList.of(
                                new RollAnswer.RollResults("2d6=", "3", "[1,2]"),
                                new RollAnswer.RollResults("3d6=", "6", "[1,2,3]")
                        ))
                        .build(), null, "2d6=,3d6=: 2d6= ⇒ 3, 3d6= ⇒ 6", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of()),
                Arguments.of(RollAnswer.builder()
                        .answerFormatType(AnswerFormatType.minimal)
                        .expression("2d6=,3d6=")
                        .expressionLabel("label")
                        .multiRollResults(ImmutableList.of(
                                new RollAnswer.RollResults("2d6=", "3", "[1,2]"),
                                new RollAnswer.RollResults("3d6=", "6", "[1,2,3]")
                        ))
                        .build(), null, "label: 2d6= ⇒ 3, 3d6= ⇒ 6", EmbedOrMessageDefinition.Type.MESSAGE, ImmutableList.of())
        );
    }

    @ParameterizedTest(name = "{index} rollAnswer={0} -> expectedTitle={1}, expectedDescription={2}")
    @MethodSource("generateConversionData")
    void testConversion(RollAnswer rollAnswer, String expectedTitle, String expectedDescription, EmbedOrMessageDefinition.Type expectedType, ImmutableList<EmbedOrMessageDefinition.Field> expectedFields) {

        assertThat(rollAnswer.toEmbedOrMessageDefinition().getTitle()).isEqualTo(expectedTitle);
        assertThat(rollAnswer.toEmbedOrMessageDefinition().getDescriptionOrContent()).isEqualTo(expectedDescription);
        assertThat(rollAnswer.toEmbedOrMessageDefinition().getType()).isEqualTo(expectedType);
        assertThat(rollAnswer.toEmbedOrMessageDefinition().getFields()).isEqualTo(expectedFields);
    }

}