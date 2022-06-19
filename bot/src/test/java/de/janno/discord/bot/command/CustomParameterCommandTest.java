package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.CustomParameterCommand.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CustomParameterCommandTest {

    CustomParameterCommand underTest;

    private static Stream<Arguments> generateParameterExpression2ButtonValuesData() {
        return Stream.of(
                Arguments.of("{test}", ImmutableList.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15")),
                Arguments.of("{test:2<=>4}", ImmutableList.of("2", "3", "4")),
                Arguments.of("{test:2<=>1}", ImmutableList.of("2")),
                Arguments.of("{test:-2<=>1}", ImmutableList.of("-2", "-1", "0", "1")),
                Arguments.of("{test:-10<=>-5}", ImmutableList.of("-10", "-9", "-8", "-7", "-6", "-5")),
                Arguments.of("{test:1d6/+5/abc}", ImmutableList.of("1d6", "+5", "abc"))
        );
    }

    public static Stream<Arguments> generateValidationData() {
        return Stream.of(
                Arguments.of(new Config("{number}d{sides}", null), Optional.empty()),
                Arguments.of(new Config("{number}d{sides:3<=>6}", null), Optional.empty()),
                Arguments.of(new Config("{number}d{sides:3/4/ab}", null), Optional.of("The following dice expression is invalid: '3dab'. Use /custom_parameter help to get more information on how to use the command."))
        );
    }

    @BeforeEach
    void setup() {
        underTest = new CustomParameterCommand();
    }

    @ParameterizedTest
    @CsvSource({
            "{test},            1",
            "{test:2<=>4},      2",
            "{test:-2<=>4},     -2",
            "{test:0<=>4},      0"
    })
    void getMinButtonFrom(String parameterExpression, int expectedResult) {
        int res = underTest.getMinButtonFrom(parameterExpression);

        assertThat(res).isEqualTo(expectedResult);

    }

    @ParameterizedTest
    @CsvSource({
            "{test},            15",
            "{test:2<=>4},      4",
            "{test:2<=>1},      2",
            "{test:-10<=>-5},   -5",
            "{test:1<=>27},     24",
    })
    void getMaxButtonFrom(String parameterExpression, int expectedResult) {
        int res = underTest.getMaxButtonFrom(parameterExpression);

        assertThat(res).isEqualTo(expectedResult);

    }

    @ParameterizedTest(name = "{index} {0} -> {1}")
    @MethodSource("generateParameterExpression2ButtonValuesData")
    void getButtonValues(String parameterExpression, List<String> expectedResult) {
        List<String> res = underTest.getButtonValues(parameterExpression);
        assertThat(res).isEqualTo(expectedResult);
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("generateValidationData")
    void validate(Config config, Optional<String> expectedResult) {
        Optional<String> res = underTest.validateAllPossibleStates(config);
        assertThat(res).isEqualTo(expectedResult);
    }
}