package de.janno.discord.bot.dice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.command.RollAnswerConverter;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import dev.diceroll.parser.DiceExpression;
import dev.diceroll.parser.ResultTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiceParserAdapterTest {

    DiceParserAdapter underTest;

    private static Stream<Arguments> generateValidateData() {
        return Stream.of(
                Arguments.of(ImmutableList.of(), "You must configure at least one dice expression"),
                Arguments.of(ImmutableList.of("1d6"), null),
                Arguments.of(ImmutableList.of("+1d6"), null),
                Arguments.of(ImmutableList.of("1d6 "), null),
                Arguments.of(ImmutableList.of(" 1d6 "), null),
                Arguments.of(ImmutableList.of("2x[1d6]"), null),
                Arguments.of(ImmutableList.of("1d6@Attack"), null),
                Arguments.of(ImmutableList.of("1d2=2?Head:Tails@Toss a coin"), null),
                Arguments.of(ImmutableList.of("3d6>3<2?Success:Failure@3d6 Test"), null),
                Arguments.of(ImmutableList.of("2x[3d6>3<2?Success:Failure]@3d6 2*Test"), null),
                Arguments.of(ImmutableList.of("2d6&3d10"), null),
                Arguments.of(ImmutableList.of("2d6>4?a:b&3d10<6?c:d"), null),
                Arguments.of(ImmutableList.of("2d6&3d10@Test"), null),
                Arguments.of(ImmutableList.of("2d6>4?a:b&3d10<6?c:d@Test"), null),
                Arguments.of(ImmutableList.of("2x[2d6]&1d8"), "The following dice expression is invalid: '2x[2d6]&1d8'"),
                Arguments.of(ImmutableList.of("1d6@Attack", "1d6@Parry"), null),
                Arguments.of(ImmutableList.of("1d6@a,b"), null),
                Arguments.of(ImmutableList.of("1d6@a\u001eb"), "The button definition '1d6@a\u001Eb' is not allowed to contain '\u001E'"),
                Arguments.of(ImmutableList.of(" 1d6 @ Attack "), null),
                Arguments.of(ImmutableList.of("a"), "The following dice expression is invalid: 'a'"),
                Arguments.of(ImmutableList.of("@"), "The button definition '@' should have the diceExpression@Label"),
                Arguments.of(ImmutableList.of("a@Attack"), "The following dice expression is invalid: 'a'"),
                Arguments.of(ImmutableList.of("a@"), "The button definition 'a@' should have the diceExpression@Label"),
                Arguments.of(ImmutableList.of("@Attack"), "Dice expression for '@Attack' is empty"),
                Arguments.of(ImmutableList.of("1d6@1d6"), null),
                Arguments.of(ImmutableList.of("1d6@1d6@1d6"), "The button definition '1d6@1d6@1d6' should have the diceExpression@Label"),
                Arguments.of(ImmutableList.of("1d6@@1d6"), "The button definition '1d6@@1d6' should have the diceExpression@Label"),
                Arguments.of(ImmutableList.of("1d6@@"), "The button definition '1d6@@' should have the diceExpression@Label"),
                Arguments.of(ImmutableList.of("@1d6"), "Dice expression for '@1d6' is empty")

        );
    }

    static Stream<Arguments> generateMultipleExecutionData() {
        return Stream.of(
                Arguments.of("1d6", false),
                Arguments.of("2x[1d6]", true),
                Arguments.of("2[1d6]", false),
                Arguments.of("-2x[1d6]", false),
                Arguments.of("x[1d6]", false),
                Arguments.of("-x[1d6]", false),
                Arguments.of("ax[1d6]", false),
                Arguments.of("1x[1d6", false),
                Arguments.of("12x[1d6]", true),
                Arguments.of("2x[3d6>3<2?Success:Failure]", true),
                Arguments.of("2d6&3d10", true),
                Arguments.of("2d6&3d10@Test", true),
                Arguments.of("2d6>4?a:b&3d10<6?c:d", true),
                Arguments.of("2x[2d6]&1d8", false)
        );
    }

    static Stream<Arguments> generateBooleanExpressionData() {
        return Stream.of(
                Arguments.of("1d6>3?t:f", new DiceParserAdapter.BooleanExpression("1d6", ImmutableList.of(new DiceParserAdapter.ValueCompereResult(BooleanOperator.GREATER, 3, "t")), "f")),
                Arguments.of("1d6<3?t:f", new DiceParserAdapter.BooleanExpression("1d6", ImmutableList.of(new DiceParserAdapter.ValueCompereResult(BooleanOperator.LESSER, 3, "t")), "f")),
                Arguments.of("1d6=3?t:f", new DiceParserAdapter.BooleanExpression("1d6", ImmutableList.of(new DiceParserAdapter.ValueCompereResult(BooleanOperator.EQUAL, 3, "t")), "f")),
                Arguments.of("1d6<=3?t:f", new DiceParserAdapter.BooleanExpression("1d6", ImmutableList.of(new DiceParserAdapter.ValueCompereResult(BooleanOperator.LESSER_EQUAL, 3, "t")), "f")),
                Arguments.of("1d6>=3?t:f", new DiceParserAdapter.BooleanExpression("1d6", ImmutableList.of(new DiceParserAdapter.ValueCompereResult(BooleanOperator.GREATER_EQUAL, 3, "t")), "f")),
                Arguments.of("1d6<>3?t:f", new DiceParserAdapter.BooleanExpression("1d6", ImmutableList.of(new DiceParserAdapter.ValueCompereResult(BooleanOperator.NOT_EQUAL, 3, "t")), "f")),
                Arguments.of("11d66<>33?t:f", new DiceParserAdapter.BooleanExpression("11d66", ImmutableList.of(new DiceParserAdapter.ValueCompereResult(BooleanOperator.NOT_EQUAL, 33, "t")), "f")),
                Arguments.of("11d66<10<>33?t:f", new DiceParserAdapter.BooleanExpression("11d66<10", ImmutableList.of(new DiceParserAdapter.ValueCompereResult(BooleanOperator.NOT_EQUAL, 33, "t")), "f")),
                Arguments.of("1d6<=1?a<2?b>3?c>=4?d==5?e<>6?f:g", new DiceParserAdapter.BooleanExpression("1d6", ImmutableList.of(
                        new DiceParserAdapter.ValueCompereResult(BooleanOperator.LESSER_EQUAL, 1, "a"),
                        new DiceParserAdapter.ValueCompereResult(BooleanOperator.LESSER, 2, "b"),
                        new DiceParserAdapter.ValueCompereResult(BooleanOperator.GREATER, 3, "c"),
                        new DiceParserAdapter.ValueCompereResult(BooleanOperator.GREATER_EQUAL, 4, "d"),
                        new DiceParserAdapter.ValueCompereResult(BooleanOperator.EQUAL, 5, "e"),
                        new DiceParserAdapter.ValueCompereResult(BooleanOperator.NOT_EQUAL, 6, "f")
                ), "g")),
                Arguments.of("11d66<10<>33?<>=:<=>", new DiceParserAdapter.BooleanExpression("11d66<10", ImmutableList.of(), "<=>"))

        );
    }

    static Stream<Arguments> generateBooleanExpressionRolls() {
        return Stream.of(
                Arguments.of("1d6>3?t:f", null, new EmbedOrMessageDefinition("1d6 ⇒ f", "[3] = 3 ⟹ f", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)),
                Arguments.of("1d6<3?t:f", null, new EmbedOrMessageDefinition("1d6 ⇒ f", "[3] = 3 ⟹ f", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)),
                Arguments.of("1d6=3?t:f", null, new EmbedOrMessageDefinition("1d6 ⇒ t", "[3] = 3=3 ⟹ t", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)),
                Arguments.of("1d6<=3?t:f", null, new EmbedOrMessageDefinition("1d6 ⇒ t", "[3] = 3≤3 ⟹ t", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)),
                Arguments.of("1d6>=3?t:f", null, new EmbedOrMessageDefinition("1d6 ⇒ t", "[3] = 3≥3 ⟹ t", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)),
                Arguments.of("1d6<>3?t:f", null, new EmbedOrMessageDefinition("1d6 ⇒ f", "[3] = 3 ⟹ f", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)),

                Arguments.of("1d6>3?t:f", "label", new EmbedOrMessageDefinition("label ⇒ f", "1d6: [3] = 3 ⟹ f", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)),
                Arguments.of("1d6<3?t:f", "label", new EmbedOrMessageDefinition("label ⇒ f", "1d6: [3] = 3 ⟹ f", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)),
                Arguments.of("1d6=3?t:f", "label", new EmbedOrMessageDefinition("label ⇒ t", "1d6: [3] = 3=3 ⟹ t", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)),
                Arguments.of("1d6<=3?t:f", "label", new EmbedOrMessageDefinition("label ⇒ t", "1d6: [3] = 3≤3 ⟹ t", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)),
                Arguments.of("1d6>=3?t:f", "label", new EmbedOrMessageDefinition("label ⇒ t", "1d6: [3] = 3≥3 ⟹ t", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)),
                Arguments.of("1d6<>3?t:f", "label", new EmbedOrMessageDefinition("label ⇒ f", "1d6: [3] = 3 ⟹ f", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)),

                Arguments.of("1d6<=1?a<2?b>3?c>=4?d==5?e<>6?f:g", "label", new EmbedOrMessageDefinition("label ⇒ f", "1d6: [3] = 3≠6 ⟹ f", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED))
        );
    }

    @BeforeEach
    void setup() {
        underTest = new DiceParserAdapter();
    }

    @ParameterizedTest(name = "{index} input:{0}, label:{1} -> {2}")
    @MethodSource("generateBooleanExpressionRolls")
    void rollBooleanExpression(String diceExpression, String label, EmbedOrMessageDefinition expected) {
        Dice diceMock = mock(Dice.class);
        DiceParserAdapter underTest = new DiceParserAdapter(diceMock);
        when(diceMock.detailedRoll(any())).thenReturn(new ResultTree(mock(DiceExpression.class), 3, ImmutableList.of()));

        RollAnswer res = underTest.answerRollWithGivenLabel(diceExpression, label, AnswerFormatType.full);

        assertThat(RollAnswerConverter.toEmbedOrMessageDefinition(res)).isEqualTo(expected);
    }

    @Test
    void booleanExpression() {
        Dice diceMock = mock(Dice.class);
        DiceParserAdapter underTest = new DiceParserAdapter(diceMock);
        when(diceMock.detailedRoll(any())).thenReturn(new ResultTree(mock(DiceExpression.class), 3, ImmutableList.of()));

        RollAnswer res = underTest.answerRollWithGivenLabel("1d6>3?t:f", null, AnswerFormatType.full);

        assertThat(RollAnswerConverter.toEmbedOrMessageDefinition(res).getTitle()).isEqualTo("1d6 ⇒ f");
        assertThat(RollAnswerConverter.toEmbedOrMessageDefinition(res).getDescriptionOrContent()).isEqualTo("[3] = 3 ⟹ f");
    }

    @ParameterizedTest(name = "{index} input:{0} -> {1}")
    @MethodSource("generateBooleanExpressionData")
    void parsingBooleanExpression(String diceExpression, DiceParserAdapter.BooleanExpression expected) {
        DiceParserAdapter.BooleanExpression res = underTest.getBooleanExpression(diceExpression);
        assertThat(res).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("generateValidateData")
    void validate(List<String> optionValue, String expected) {
        if (expected == null) {
            assertThat(underTest.validateListOfExpressions(optionValue, "@")).isEmpty();
        } else {
            assertThat(underTest.validateListOfExpressions(optionValue, "@")).contains(expected);
        }
    }


    @ParameterizedTest(name = "{index} input:{0} -> {1}")
    @MethodSource("generateMultipleExecutionData")
    void multipleExecution(String diceExpression, boolean expected) {
        boolean res = DiceParserAdapter.isMultipleRoll(diceExpression);
        assertThat(res).isEqualTo(expected);
    }

    @Test
    void getMultipleExecution() {
        int res = DiceParserAdapter.getNumberOfMultipleRolls("11x[1d6 + [1d20]!!]");
        assertThat(res).isEqualTo(11);
    }

    @Test
    void getMultipleExecution_limit() {
        int res = DiceParserAdapter.getNumberOfMultipleRolls("26x[1d6 + [1d20]!!]");
        assertThat(res).isEqualTo(25);
    }


    @Test
    void getInnerDiceExpression() {
        String res = DiceParserAdapter.getInnerDiceExpressionFromMultiRoll("11x[1d6 + [1d20]!!]");
        assertThat(res).isEqualTo("1d6 + [1d20]!!");
    }

    @Test
    void roll_3x3d6() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.answerRollWithGivenLabel("3x[3d6]", null, AnswerFormatType.full));

        assertThat(res.getFields()).hasSize(3);
        assertThat(res.getDescriptionOrContent()).isNull();
        assertThat(res.getTitle()).isEqualTo("Multiple Results");
    }

    @Test
    void roll_3d6() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.answerRollWithGivenLabel("3d6", null, AnswerFormatType.full));

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getDescriptionOrContent()).isNotEmpty();
        assertThat(res.getTitle()).startsWith("3d6 ⇒ ");
    }

    @Test
    void roll_plus3d6() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.answerRollWithGivenLabel("+3d6", null, AnswerFormatType.full));

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getDescriptionOrContent()).isNotEmpty();
        assertThat(res.getTitle()).startsWith("+3d6 ⇒ ");
    }

    @Test
    void roll_3x3d6Label() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.answerRollWithGivenLabel("3x[3d6]", "Label", AnswerFormatType.full));

        assertThat(res.getFields()).hasSize(3);
        assertThat(res.getDescriptionOrContent()).isNull();
        assertThat(res.getTitle()).isEqualTo("Label");
    }

    @Test
    void roll_3d6Label() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.answerRollWithGivenLabel("3d6", "Label", AnswerFormatType.full));

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getDescriptionOrContent()).isNotEmpty();
        assertThat(res.getTitle()).startsWith("Label ⇒ ");
    }

    @Test
    void roll_boolean3d6() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.answerRollWithGivenLabel("3d6>3<2?Success:Failure", "3d6 Test", AnswerFormatType.full));

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getDescriptionOrContent()).isNotEmpty();
        assertThat(res.getTitle()).startsWith("3d6 Test ⇒ ");
    }

    @Test
    void roll_overflow() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.answerRollWithGivenLabel("2147483647+1", "Label", AnswerFormatType.full));

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getDescriptionOrContent()).isNotEmpty();
        assertThat(res.getTitle()).isEqualTo("Error in `2147483647+1`");
        assertThat(res.getDescriptionOrContent()).isEqualTo("integer overflow");
    }

    @Test
    void roll_overflow_multiple() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.answerRollWithGivenLabel("3x[2147483647+1]", "Label", AnswerFormatType.full));

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("Error in `3x[2147483647+1]`");
        assertThat(res.getDescriptionOrContent()).isEqualTo("integer overflow");
    }

    @Test
    void roll_3x3d6Bool() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.answerRollWithGivenLabel("2x[3d6>3<2?Success:Failure]", "3d6 2*Test", AnswerFormatType.full));

        assertThat(res.getFields()).hasSize(2);
        assertThat(res.getDescriptionOrContent()).isNull();
        assertThat(res.getTitle()).isEqualTo("3d6 2*Test");
    }

    @Test
    void roll_multiDiff() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.answerRollWithGivenLabel("2d6&3d10", null, AnswerFormatType.full));

        assertThat(res.getFields()).hasSize(2);
        assertThat(res.getDescriptionOrContent()).isNull();
        assertThat(res.getTitle()).isEqualTo("Multiple Results");
    }

    @Test
    void roll_multiDiffLabel() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.answerRollWithGivenLabel("2d6&3d10", "Test", AnswerFormatType.full));

        assertThat(res.getFields()).hasSize(2);
        assertThat(res.getFields().get(0).getName()).startsWith("2d6");
        assertThat(res.getFields().get(1).getName()).startsWith("3d10");

        assertThat(res.getDescriptionOrContent()).isNull();
        assertThat(res.getTitle()).isEqualTo("Test");
    }

    @Test
    void roll_multiDiffBoolLabel() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.answerRollWithGivenLabel("2d6>4?a:b&3d10<6?c:d", "Test", AnswerFormatType.full));

        assertThat(res.getFields()).hasSize(2);
        assertThat(res.getFields().get(0).getName()).startsWith("2d6");
        assertThat(res.getFields().get(1).getName()).startsWith("3d10");
        assertThat(res.getDescriptionOrContent()).isNull();
        assertThat(res.getTitle()).isEqualTo("Test");
    }
}