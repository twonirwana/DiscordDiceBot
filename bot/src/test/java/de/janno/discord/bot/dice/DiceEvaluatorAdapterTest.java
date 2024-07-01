package de.janno.discord.bot.dice;

import de.janno.evaluator.dice.ExpressionPosition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DiceEvaluatorAdapterTest {

    static Stream<Arguments> generateData() {
        return Stream.of(
                Arguments.of("1w3", ExpressionPosition.of(1, "w"), "`1`__w__`3`"),
                Arguments.of("  1w3 ", ExpressionPosition.of(1, "w"), "`1`__w__`3`"),
                Arguments.of("1d6 + 2d6 + asdfsd + 12d6 + 12312", ExpressionPosition.of(12, "a"), "`2d6 + `__a__`sdfsd `"),
                Arguments.of("a", ExpressionPosition.of(0, "a"), "__a__")
        );
    }

    @ParameterizedTest(name = "{index} expession:{0}, position:{1} -> {2}")
    @MethodSource("generateData")
    void getErrorLocationString(String expression, ExpressionPosition position, String expected) {
        String res = DiceEvaluatorAdapter.getErrorLocationString(expression, position);
        assertThat(res).isEqualTo(expected);
    }

}