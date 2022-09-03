package de.janno.discord.bot.dice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.message.EmbedDefinition;
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

class DiceParserHelperTest {

    DiceParserHelper underTest;

    private static Stream<Arguments> generateValidateData() {
        return Stream.of(
                Arguments.of(ImmutableList.of(), "You must configure at least one dice expression. Use '/custom_dice help' to get more information on how to use the command."),
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
                Arguments.of(ImmutableList.of("2x[2d6]&1d8"), "The following dice expression is invalid: '2x[2d6]&1d8'. Use /custom_dice help to get more information on how to use the command."),
                Arguments.of(ImmutableList.of("1d6@Attack", "1d6@Parry"), "The dice expression '1d6' is not unique. Each dice expression must only once."),
                Arguments.of(ImmutableList.of("1d6@a,b"), "The button definition '1d6@a,b' is not allowed to contain ','"),
                Arguments.of(ImmutableList.of(" 1d6 @ Attack "), null),
                Arguments.of(ImmutableList.of("a"), "The following dice expression is invalid: 'a'. Use /custom_dice help to get more information on how to use the command."),
                Arguments.of(ImmutableList.of("@"), "The button definition '@' should have the diceExpression@Label"),
                Arguments.of(ImmutableList.of("a@Attack"), "The following dice expression is invalid: 'a'. Use /custom_dice help to get more information on how to use the command."),
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
                Arguments.of("1d6>3?t:f", new DiceParserHelper.BooleanExpression("1d6", ImmutableList.of(new DiceParserHelper.ValueCompereResult(BooleanOperator.GREATER, 3, "t")), "f")),
                Arguments.of("1d6<3?t:f", new DiceParserHelper.BooleanExpression("1d6", ImmutableList.of(new DiceParserHelper.ValueCompereResult(BooleanOperator.LESSER, 3, "t")), "f")),
                Arguments.of("1d6=3?t:f", new DiceParserHelper.BooleanExpression("1d6", ImmutableList.of(new DiceParserHelper.ValueCompereResult(BooleanOperator.EQUAL, 3, "t")), "f")),
                Arguments.of("1d6<=3?t:f", new DiceParserHelper.BooleanExpression("1d6", ImmutableList.of(new DiceParserHelper.ValueCompereResult(BooleanOperator.LESSER_EQUAL, 3, "t")), "f")),
                Arguments.of("1d6>=3?t:f", new DiceParserHelper.BooleanExpression("1d6", ImmutableList.of(new DiceParserHelper.ValueCompereResult(BooleanOperator.GREATER_EQUAL, 3, "t")), "f")),
                Arguments.of("1d6<>3?t:f", new DiceParserHelper.BooleanExpression("1d6", ImmutableList.of(new DiceParserHelper.ValueCompereResult(BooleanOperator.NOT_EQUAL, 3, "t")), "f")),
                Arguments.of("11d66<>33?t:f", new DiceParserHelper.BooleanExpression("11d66", ImmutableList.of(new DiceParserHelper.ValueCompereResult(BooleanOperator.NOT_EQUAL, 33, "t")), "f")),
                Arguments.of("11d66<10<>33?t:f", new DiceParserHelper.BooleanExpression("11d66<10", ImmutableList.of(new DiceParserHelper.ValueCompereResult(BooleanOperator.NOT_EQUAL, 33, "t")), "f")),
                Arguments.of("1d6<=1?a<2?b>3?c>=4?d==5?e<>6?f:g", new DiceParserHelper.BooleanExpression("1d6", ImmutableList.of(
                        new DiceParserHelper.ValueCompereResult(BooleanOperator.LESSER_EQUAL, 1, "a"),
                        new DiceParserHelper.ValueCompereResult(BooleanOperator.LESSER, 2, "b"),
                        new DiceParserHelper.ValueCompereResult(BooleanOperator.GREATER, 3, "c"),
                        new DiceParserHelper.ValueCompereResult(BooleanOperator.GREATER_EQUAL, 4, "d"),
                        new DiceParserHelper.ValueCompereResult(BooleanOperator.EQUAL, 5, "e"),
                        new DiceParserHelper.ValueCompereResult(BooleanOperator.NOT_EQUAL, 6, "f")
                ), "g")),
                Arguments.of("11d66<10<>33?<>=:<=>", new DiceParserHelper.BooleanExpression("11d66<10", ImmutableList.of(), "<=>"))

        );
    }

    static Stream<Arguments> generateBooleanExpressionRolls() {
        return Stream.of(
                Arguments.of("1d6>3?t:f", null, new EmbedDefinition("1d6: f", "[3] = 3 ⟹ f", ImmutableList.of())),
                Arguments.of("1d6<3?t:f", null, new EmbedDefinition("1d6: f", "[3] = 3 ⟹ f", ImmutableList.of())),
                Arguments.of("1d6=3?t:f", null, new EmbedDefinition("1d6: t", "[3] = 3=3 ⟹ t", ImmutableList.of())),
                Arguments.of("1d6<=3?t:f", null, new EmbedDefinition("1d6: t", "[3] = 3≤3 ⟹ t", ImmutableList.of())),
                Arguments.of("1d6>=3?t:f", null, new EmbedDefinition("1d6: t", "[3] = 3≥3 ⟹ t", ImmutableList.of())),
                Arguments.of("1d6<>3?t:f", null, new EmbedDefinition("1d6: f", "[3] = 3 ⟹ f", ImmutableList.of())),

                Arguments.of("1d6>3?t:f", "label", new EmbedDefinition("label: f", "[3] = 3 ⟹ f", ImmutableList.of())),
                Arguments.of("1d6<3?t:f", "label", new EmbedDefinition("label: f", "[3] = 3 ⟹ f", ImmutableList.of())),
                Arguments.of("1d6=3?t:f", "label", new EmbedDefinition("label: t", "[3] = 3=3 ⟹ t", ImmutableList.of())),
                Arguments.of("1d6<=3?t:f", "label", new EmbedDefinition("label: t", "[3] = 3≤3 ⟹ t", ImmutableList.of())),
                Arguments.of("1d6>=3?t:f", "label", new EmbedDefinition("label: t", "[3] = 3≥3 ⟹ t", ImmutableList.of())),
                Arguments.of("1d6<>3?t:f", "label", new EmbedDefinition("label: f", "[3] = 3 ⟹ f", ImmutableList.of())),

                Arguments.of("1d6<=1?a<2?b>3?c>=4?d==5?e<>6?f:g", "label", new EmbedDefinition("label: f", "[3] = 3≠6 ⟹ f", ImmutableList.of()))
        );
    }

    @BeforeEach
    void setup() {
        underTest = new DiceParserHelper();
    }

    @ParameterizedTest(name = "{index} input:{0}, label:{1} -> {2}")
    @MethodSource("generateBooleanExpressionRolls")
    void rollBooleanExpression(String diceExpression, String label, EmbedDefinition expected) {
        Dice diceMock = mock(Dice.class);
        DiceParserHelper underTest = new DiceParserHelper(diceMock);
        when(diceMock.roll(any())).thenReturn(3);
        when(diceMock.detailedRoll(any())).thenReturn(new ResultTree(mock(DiceExpression.class), 3, ImmutableList.of()));

        EmbedDefinition res = underTest.roll(diceExpression, label);
        assertThat(res).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} input:{0} -> {1}")
    @MethodSource("generateBooleanExpressionData")
    void parsingBooleanExpression(String diceExpression, DiceParserHelper.BooleanExpression expected) {
        DiceParserHelper.BooleanExpression res = underTest.getBooleanExpression(diceExpression);
        assertThat(res).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("generateValidateData")
    void validate(List<String> optionValue, String expected) {
        if (expected == null) {
            assertThat(underTest.validateListOfExpressions(optionValue, "@", ",", "/custom_dice help", 87)).isEmpty();
        } else {
            assertThat(underTest.validateListOfExpressions(optionValue, "@", ",", "/custom_dice help", 87)).contains(expected);
        }
    }


    @ParameterizedTest(name = "{index} input:{0} -> {1}")
    @MethodSource("generateMultipleExecutionData")
    void multipleExecution(String diceExpression, boolean expected) {
        boolean res = DiceParserHelper.isMultipleRoll(diceExpression);
        assertThat(res).isEqualTo(expected);
    }

    @Test
    void getMultipleExecution() {
        int res = DiceParserHelper.getNumberOfMultipleRolls("11x[1d6 + [1d20]!!]");
        assertThat(res).isEqualTo(11);
    }

    @Test
    void getMultipleExecution_limit() {
        int res = DiceParserHelper.getNumberOfMultipleRolls("26x[1d6 + [1d20]!!]");
        assertThat(res).isEqualTo(25);
    }


    @Test
    void getInnerDiceExpression() {
        String res = DiceParserHelper.getInnerDiceExpressionFromMultiRoll("11x[1d6 + [1d20]!!]");
        assertThat(res).isEqualTo("1d6 + [1d20]!!");
    }

    @Test
    void validateDiceExpressions() {
        assertThat(underTest.validateDiceExpression("1d4/", "test", 87))
                .contains("The following dice expression is invalid: '1d4/'. Use test to get more information on how to use the command.");
    }

    @Test
    void roll_3x3d6() {
        EmbedDefinition res = underTest.roll("3x[3d6]", null);

        assertThat(res.getFields()).hasSize(3);
        assertThat(res.getDescription()).isNull();
        assertThat(res.getTitle()).isEqualTo("Multiple Results");
    }

    @Test
    void roll_3d6() {
        EmbedDefinition res = underTest.roll("3d6", null);

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getDescription()).isNotEmpty();
        assertThat(res.getTitle()).startsWith("3d6 = ");
    }

    @Test
    void roll_plus3d6() {
        EmbedDefinition res = underTest.roll("+3d6", null);

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getDescription()).isNotEmpty();
        assertThat(res.getTitle()).startsWith("3d6 = ");
    }

    @Test
    void roll_3x3d6Label() {
        EmbedDefinition res = underTest.roll("3x[3d6]", "Label");

        assertThat(res.getFields()).hasSize(3);
        assertThat(res.getDescription()).isNull();
        assertThat(res.getTitle()).isEqualTo("Label");
    }

    @Test
    void roll_3d6Label() {
        EmbedDefinition res = underTest.roll("3d6", "Label");

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getDescription()).isNotEmpty();
        assertThat(res.getTitle()).startsWith("Label: 3d6 = ");
    }

    @Test
    void roll_boolean3d6() {
        EmbedDefinition res = underTest.roll("3d6>3<2?Success:Failure", "3d6 Test");

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getDescription()).isNotEmpty();
        assertThat(res.getTitle()).startsWith("3d6 Test: ");
    }

    @Test
    void roll_overflow() {
        EmbedDefinition res = underTest.roll("2147483647+1", "Label");

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getDescription()).isNotEmpty();
        assertThat(res.getTitle()).isEqualTo("Label: Arithmetic Error");
        assertThat(res.getDescription()).isEqualTo("Executing '2147483647+1' resulting in: integer overflow");
    }

    @Test
    void roll_overflow_multiple() {
        EmbedDefinition res = underTest.roll("3x[2147483647+1]", "Label");

        assertThat(res.getFields()).hasSize(3);
        assertThat(res.getDescription()).isNull();
        assertThat(res.getTitle()).isEqualTo("Label");
        assertThat(res.getFields().get(0).getName()).isEqualTo("Arithmetic Error");
        assertThat(res.getFields().get(0).getValue()).isEqualTo("Executing '2147483647+1' resulting in: integer overflow");
    }

    @Test
    void roll_3x3d6Bool() {
        EmbedDefinition res = underTest.roll("2x[3d6>3<2?Success:Failure]", "3d6 2*Test");

        assertThat(res.getFields()).hasSize(2);
        assertThat(res.getDescription()).isNull();
        assertThat(res.getTitle()).isEqualTo("3d6 2*Test");
    }

    @Test
    void roll_multiDiff() {
        EmbedDefinition res = underTest.roll("2d6&3d10", null);

        assertThat(res.getFields()).hasSize(2);
        assertThat(res.getDescription()).isNull();
        assertThat(res.getTitle()).isEqualTo("Multiple Results");
    }

    @Test
    void roll_multiDiffLabel() {
        EmbedDefinition res = underTest.roll("2d6&3d10", "Test");

        assertThat(res.getFields()).hasSize(2);
        assertThat(res.getFields().get(0).getName()).startsWith("2d6");
        assertThat(res.getFields().get(1).getName()).startsWith("3d10");

        assertThat(res.getDescription()).isNull();
        assertThat(res.getTitle()).isEqualTo("Test");
    }

    @Test
    void roll_multiDiffBoolLabel() {
        EmbedDefinition res = underTest.roll("2d6>4?a:b&3d10<6?c:d", "Test");

        assertThat(res.getFields()).hasSize(2);
        assertThat(res.getFields().get(0).getName()).startsWith("2d6");
        assertThat(res.getFields().get(1).getName()).startsWith("3d10");
        assertThat(res.getDescription()).isNull();
        assertThat(res.getTitle()).isEqualTo("Test");
    }
}