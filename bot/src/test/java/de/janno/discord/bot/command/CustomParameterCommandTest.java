package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.CustomParameterCommand.State;
import de.janno.discord.connector.api.BotConstants;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static de.janno.discord.bot.command.CustomParameterCommand.Config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomParameterCommandTest {

    private static final String FIRST_SELECT_CUSTOM_ID = "custom_parameter\u0000{n}d{s}\u0000\u0000\u00001\u0000";
    private static final String LAST_SELECT_CUSTOM_ID = "custom_parameter\u0000{n}d{s}\u0000\u00001\u00002\u0000";
    private static final String CLEAR_CUSTOM_ID = "custom_parameter\u0000{n}d{s}\u0000\u00001\u0000clear\u0000";
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
                Arguments.of("{number}d{sides}", null),
                Arguments.of("1d6", "The expression needs at least one parameter expression like '{name}"),
                Arguments.of("{number:3<=>6}d{sides:6/10/12}", null),
                Arguments.of("{number}{a:a/c/b/d/d}{sides:3<=>6}", "Parameter '[a, c, b, d, d]' contains duplicate parameter option but they must be unique."),
                Arguments.of("{number}d{sides:1<=>20} + {modification:1/5/10/1000/2d6/100d1000} + 1d{last modification: 10001<=>10020}", "The following expression with parameters is 25 to long: 1d{sides:1<=>20} + {modification:1/5/10/1000/2d6/100d1000} + 1d{last modification: 10001<=>10020}"),
                Arguments.of("{number}d{sides:3/4/ab}", "The following dice expression is invalid: '1dab'. Use /custom_parameter help to get more information on how to use the command.")
        );
    }

    private static Stream<Arguments> getStateFromEvent() {
        return Stream.of(
                //first select
                Arguments.of(FIRST_SELECT_CUSTOM_ID, "{n}d{s}: Please select value for {n}", "user1", ImmutableList.of("1"), "1d{s}", "{s}", "*{s}*", true),
                //last select
                Arguments.of(LAST_SELECT_CUSTOM_ID, "user1\u22361d{s}: Please select value for {s}", "user1", ImmutableList.of("1", "2"), "1d2", null, null, false),
                //clear
                Arguments.of(CLEAR_CUSTOM_ID, "user1\u22361d{s}: Please select value for {s}", "user2", ImmutableList.of(), "{n}d{s}", "{n}", "*{n}*", true),
                //not action because click from other user
                Arguments.of(LAST_SELECT_CUSTOM_ID, "user1\u22361d{s}: Please select value for {s}", "user2", ImmutableList.of("1"), "1d{s}", "{s}", "*{s}*", true)
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
    void validate(String slashExpression, String expectedResult) {
        Optional<String> res = underTest.getStartOptionsValidationMessage(CommandInteractionOption.builder()
                .option(CommandInteractionOption.builder()
                        .name("expression")
                        .stringValue(slashExpression).build())
                .build());
        if (expectedResult == null) {
            assertThat(res).isEmpty();
        } else {
            assertThat(res).contains(expectedResult);
        }
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("custom_parameter\u0000{n}d6\u0000\u0000")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("custom_paramete")).isFalse();
    }

    @ParameterizedTest(name = "{index} customButtonId={0}")
    @MethodSource("getStateFromEvent")
    void getStateFromEvent(String customButtonId, String messageContent, String invokingUser,
                           //expected
                           List<String> selectedParameterValues, String filledExpression, String currentParameterExpression, String currentParameterName, boolean hasMissingParameter) {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn(customButtonId);
        when(buttonEventAdaptor.getMessageContent()).thenReturn(messageContent);
        when(buttonEventAdaptor.getInvokingGuildMemberName()).thenReturn(invokingUser);
        State res = underTest.getStateFromEvent(buttonEventAdaptor);
        assertThat(res.getSelectedParameterValues()).isEqualTo(selectedParameterValues);
        assertThat(res.getFilledExpression()).isEqualTo(filledExpression);
        assertThat(res.getCurrentParameterExpression()).isEqualTo(currentParameterExpression);
        assertThat(res.getCurrentParameterName()).isEqualTo(currentParameterName);
        assertThat(res.hasMissingParameter()).isEqualTo(hasMissingParameter);
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("expression", "target_channel");
    }

    @Test
    void getAnswer_complete() {
        Optional<EmbedDefinition> res = underTest.getAnswer(new State(LAST_SELECT_CUSTOM_ID.split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX),
                "", ""), new Config(LAST_SELECT_CUSTOM_ID.split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX)));

        assertThat(res).isPresent();
        assertThat(res.map(EmbedDefinition::getTitle).orElseThrow()).startsWith("1d2 = ");
    }

    @Test
    void getAnswer_notComplete() {
        Optional<EmbedDefinition> res = underTest.getAnswer(new State(FIRST_SELECT_CUSTOM_ID.split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX),
                "", ""), new Config(FIRST_SELECT_CUSTOM_ID.split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX)));

        assertThat(res).isEmpty();
    }


}