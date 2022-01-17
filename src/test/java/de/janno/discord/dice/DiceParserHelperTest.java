package de.janno.discord.dice;

import de.janno.discord.api.Answer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DiceParserHelperTest {

    DiceParserHelper diceParserHelper;

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
                Arguments.of("12x[1d6]", true)
        );
    }

    @BeforeEach
    void setup() {
        diceParserHelper = new DiceParserHelper();
    }

    @ParameterizedTest(name = "{index} input:{0} -> {2}")
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
        String res = DiceParserHelper.getInnerDiceExpression("11x[1d6 + [1d20]!!]");
        assertThat(res).isEqualTo("1d6 + [1d20]!!");
    }

    @Test
    void validateDiceExpressions() {
        assertThat(diceParserHelper.validateDiceExpression("1d4/", "test"))
                .isEqualTo("The following dice expression are invalid: '1d4/'. Use test to get more information on how to use the command.");
    }

    @Test
    void roll_3x3d6() {
        Answer res = diceParserHelper.roll("3x[3d6]", null);

        assertThat(res.getFields()).hasSize(3);
        assertThat(res.getContent()).isNull();
        assertThat(res.getTitle()).isEqualTo("Multiple Results");
    }

    @Test
    void roll_3d6() {
        Answer res = diceParserHelper.roll("3d6", null);

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getContent()).isNotEmpty();
        assertThat(res.getTitle()).startsWith("3d6 = ");
    }

    @Test
    void roll_3x3d6Label() {
        Answer res = diceParserHelper.roll("3x[3d6]", "Label");

        assertThat(res.getFields()).hasSize(3);
        assertThat(res.getContent()).isNull();
        assertThat(res.getTitle()).isEqualTo("Label");
    }

    @Test
    void roll_3d6Label() {
        Answer res = diceParserHelper.roll("3d6", "Label");

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getContent()).isNotEmpty();
        assertThat(res.getTitle()).startsWith("Label: 3d6 = ");
    }
}