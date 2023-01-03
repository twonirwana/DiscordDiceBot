package de.janno.discord.bot.command.customParameter;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.Dice;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static de.janno.discord.bot.command.customParameter.CustomParameterCommand.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomParameterCommandTest {

    private static final String FIRST_SELECT_CUSTOM_ID = "custom_parameter\u0000{n}d{s}\u0000\u0000\u00001\u0000";
    private static final String LAST_SELECT_CUSTOM_ID = "custom_parameter\u0000{n}d{s}\u0000\u00001\u00002\u0000";
    private static final String LAST_SELECT_WITH_TARGET_CUSTOM_ID = "custom_parameter\u0000{n}d{s}\u00001234\u00001\u00002\u0000";
    private static final String CLEAR_CUSTOM_ID = "custom_parameter\u0000{n}d{s}\u0000\u00001\u0000clear\u0000";
    CustomParameterCommand underTest;
    MessageDataDAO messageDataDAO = mock(MessageDataDAO.class);

    private static Stream<Arguments> generateParameterExpression2ButtonValuesData() {
        return Stream.of(
                Arguments.of("{test}", IntStream.range(1, 16).mapToObj(i -> new ButtonIdLabelAndDiceExpression("custom_parameter\u001E" + i, String.valueOf(i), String.valueOf(i))).toList()),
                Arguments.of("{test:2<=>4}", IntStream.range(2, 5).mapToObj(i -> new ButtonIdLabelAndDiceExpression("custom_parameter\u001E" + i, String.valueOf(i), String.valueOf(i))).toList()),
                Arguments.of("{test:2<=>1}", ImmutableList.of(new ButtonIdLabelAndDiceExpression("custom_parameter\u001E2", "2", "2"))),
                Arguments.of("{test:-2<=>1}", IntStream.range(-2, 2).mapToObj(i -> new ButtonIdLabelAndDiceExpression("custom_parameter\u001E" + i, String.valueOf(i), String.valueOf(i))).toList()),
                Arguments.of("{test:-10<=>-5}", IntStream.range(-10, -4).mapToObj(i -> new ButtonIdLabelAndDiceExpression("custom_parameter\u001E" + i, String.valueOf(i), String.valueOf(i))).toList()),
                Arguments.of("{test:1d6/+5/abc}", ImmutableList.of(new ButtonIdLabelAndDiceExpression("custom_parameter\u001E1d6", "1d6", "1d6"), new ButtonIdLabelAndDiceExpression("custom_parameter\u001E+5", "+5", "+5"), new ButtonIdLabelAndDiceExpression("custom_parameter\u001Eabc", "abc", "abc"))),
                Arguments.of("{test:1d6@d6/+5@Bonus/abc}", ImmutableList.of(new ButtonIdLabelAndDiceExpression("custom_parameter\u001E1d6", "d6", "1d6"), new ButtonIdLabelAndDiceExpression("custom_parameter\u001E+5", "Bonus", "+5"), new ButtonIdLabelAndDiceExpression("custom_parameter\u001Eabc", "abc", "abc")))
        );
    }

    public static Stream<Arguments> generateValidationData() {
        return Stream.of(
                Arguments.of("{number}d{sides}", null),
                Arguments.of("{number}d{sides}@{label:attack/damage}", null),
                Arguments.of("{number}d{{sides}}", "Nested brackets are not allowed"),
                Arguments.of("{number}d{{{sides}}}", "Nested brackets are not allowed"),
                Arguments.of("{number}d{sid{es}", "All brackets must be closed"),
                Arguments.of("{number}d{sid}es}", "All brackets must be closed"),
                Arguments.of("{number}d{sides\t}", "Expression contains invalid character: '\t'"),
                Arguments.of("{number}d\t{sides}", "Expression contains invalid character: '\t'"),
                Arguments.of("{number}d{sides:/}", null), //invalid range is mapped to 1-15
                Arguments.of("{number}d{}", "A parameter expression must not be empty"),
                Arguments.of("1d6", "The expression needs at least one parameter expression like '{name}"),
                Arguments.of("{number:3<=>6}d{sides:6/10/12}", null),
                Arguments.of("{number}{a:a/c/b/d/d}{sides:3<=>6}", "Parameter '[a, c, b, d, d]' contains duplicate parameter option but they must be unique."),
                Arguments.of("{number}d{sides:3/4/'ab'}", null),
                Arguments.of("{number}d{sides:3/4/'ab'}@roll", null)
        );
    }

    private static Stream<Arguments> getStateFromEvent() {
        return Stream.of(
                //first select
                Arguments.of(FIRST_SELECT_CUSTOM_ID, "{n}d{s}: Please select value for {n}", "user1", List.of(), "{n}d{s}", "{n}", "{n}", true),
                //last select
                Arguments.of(LAST_SELECT_CUSTOM_ID, "user1\u22361d{s}: Please select value for {s}", "user1", List.of(), "{n}d{s}", "{n}", "{n}", true),
                //clear
                Arguments.of(CLEAR_CUSTOM_ID, "user1\u22361d{s}: Please select value for {s}", "user2", List.of(), "{n}d{s}", "{n}", "{n}", true),
                //not action because click from other user
                Arguments.of(LAST_SELECT_CUSTOM_ID, "user1\u22361d{s}: Please select value for {s}", "user2", List.of(), "{n}d{s}", "{n}", "{n}", true)
        );
    }

    public static Stream<Arguments> generateExpression2Parameters() {
        return Stream.of(
                Arguments.of("{number}d{sides}", List.of(new Parameter("{number}", "number", IntStream.range(1, 16)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ValueAndLabel(s, s))
                        .toList()), new Parameter("{sides}", "sides", IntStream.range(1, 16)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ValueAndLabel(s, s))
                        .toList()))),
                Arguments.of("{number}d{sides:-2<=>2}", List.of(new Parameter("{number}", "number", IntStream.range(1, 16)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ValueAndLabel(s, s))
                        .toList()), new Parameter("{sides:-2<=>2}", "sides", IntStream.range(-2, 3)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ValueAndLabel(s, s))
                        .toList()))),
                Arguments.of("{number}d{sides:4/12/+5}", List.of(new Parameter("{number}", "number", IntStream.range(1, 16)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ValueAndLabel(s, s))
                        .toList()), new Parameter("{sides:4/12/+5}", "sides", List.of(new Parameter.ValueAndLabel("4", "4"),
                        new Parameter.ValueAndLabel("12", "12"),
                        new Parameter.ValueAndLabel("+5", "+5"))))),
                Arguments.of("{number}d{sides:4@D4/12@D12/+5@Bonus}", List.of(new Parameter("{number}", "number", IntStream.range(1, 16)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ValueAndLabel(s, s))
                        .toList()), new Parameter("{sides:4@D4/12@D12/+5@Bonus}", "sides", List.of(new Parameter.ValueAndLabel("4", "D4"),
                        new Parameter.ValueAndLabel("12", "D12"),
                        new Parameter.ValueAndLabel("+5", "Bonus")))))

        );
    }

    @BeforeEach
    void setup() {
        underTest = new CustomParameterCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
    }

    @ParameterizedTest
    @CsvSource({
            "{test},            1",
            "{test:2<=>4},      2",
            "{test:-2<=>4},     -2",
            "{test:0<=>4},      0"
    })
    void getMinButtonFrom(String parameterExpression, int expectedResult) {
        int res = CustomParameterCommand.getMinButtonFrom(parameterExpression);

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
        int res = CustomParameterCommand.getMaxButtonFrom(parameterExpression);

        assertThat(res).isEqualTo(expectedResult);
    }

    @ParameterizedTest(name = "{index} {0} -> {1}")
    @MethodSource("generateParameterExpression2ButtonValuesData")
    void getButtonValues(String parameterExpression, List<ButtonIdLabelAndDiceExpression> expectedResult) {
        CustomParameterConfig config = new CustomParameterConfig(null, "1d6 + {a} + " + parameterExpression, DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression);
        List<ButtonIdLabelAndDiceExpression> res = underTest.getButtons(config, parameterExpression);
        assertThat(res).isEqualTo(expectedResult);
    }

    @ParameterizedTest(name = "{index} {0} -> {1}")
    @MethodSource("generateExpression2Parameters")
    void createParameterListFromBaseExpression(String parameterExpression, List<Parameter> expectedResult) {
        List<Parameter> res = CustomParameterCommand.createParameterListFromBaseExpression(parameterExpression);
        assertThat(res).isEqualTo(expectedResult);
    }


    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("generateValidationData")
    void validate(String slashExpression, String expectedResult) {
        Optional<String> res = underTest.getStartOptionsValidationMessage(CommandInteractionOption.builder()
                .name("start")
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
    void matchingComponentCustomId_match_legacy() {
        assertThat(underTest.matchingComponentCustomId("custom_parameter\u0000{n}d6\u0000\u0000")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch_legacy() {
        assertThat(underTest.matchingComponentCustomId("custom_paramete")).isFalse();
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("custom_parameter5_button")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("custom_parameter25_button")).isFalse();
    }

    @ParameterizedTest(name = "{index} customButtonId={0}")
    @MethodSource("getStateFromEvent")
    void getStateFromEvent(String customButtonId, String messageContent, String invokingUser,
                           //expected
                           List<String> selectedParameterValues, String filledExpression, String currentParameterExpression, String currentParameterName, boolean hasMissingParameter) {
        ButtonEventAdaptor buttonEventAdaptor = mock(ButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn(customButtonId);
        when(buttonEventAdaptor.getMessageContent()).thenReturn(messageContent);
        when(buttonEventAdaptor.getInvokingGuildMemberName()).thenReturn(invokingUser);
        State<CustomParameterStateData> res = underTest.getStateFromEvent(buttonEventAdaptor);

        CustomParameterConfig config = createConfigFromCustomId(customButtonId);
        assertThat(res.getData()).isNotNull();
        assertThat(res.getData().getSelectedParameterValues().stream()
                .map(SelectedParameter::getSelectedValue)
                .filter(Objects::nonNull))
                .containsExactlyElementsOf(selectedParameterValues);
        assertThat(getFilledExpression(config, res)).isEqualTo(filledExpression);
        assertThat(getCurrentParameterExpression(res)).contains(currentParameterExpression);
        assertThat(getCurrentParameterName(res)).contains(currentParameterName);
        assertThat(hasMissingParameter(res)).isEqualTo(hasMissingParameter);
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("expression");
    }


    @Test
    void getAnswerTargetChannelId_local() {
        Long res = createConfigFromCustomId(LAST_SELECT_CUSTOM_ID).getAnswerTargetChannelId();

        assertThat(res).isNull();
    }

    @Test
    void getAnswerTargetChannelId_target() {
        Long res = createConfigFromCustomId(LAST_SELECT_WITH_TARGET_CUSTOM_ID).getAnswerTargetChannelId();

        assertThat(res).isEqualTo(1234L);
    }


    @Test
    void createNewButtonMessageWithState_inSelection() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(createConfigFromCustomId(FIRST_SELECT_CUSTOM_ID), createParameterStateFromLegacyId(FIRST_SELECT_CUSTOM_ID, "{n}d{s}: Please select value for {n}", "user1"));

        assertThat(res).isEmpty();
    }

    @Test
    void checkPersistence() {
        MessageDataDAO messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new CustomParameterCommand(messageDataDAO, mock(Dice.class), (minExcl, maxIncl) -> 0, 10);
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        UUID configUUID = UUID.randomUUID();
        CustomParameterConfig config = new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        State<CustomParameterStateData> state = new State<>("5", new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "5", "5"),
                new SelectedParameter("{s}", "s", null, null)), "userName"));
        Optional<MessageDataDTO> toSave = underTest.createMessageDataForNewMessage(configUUID, 1L, channelId, messageId, config, state);
        messageDataDAO.saveMessageData(toSave.orElseThrow());
        underTest.updateCurrentMessageStateData(channelId, messageId, config, state);

        MessageDataDTO loaded = messageDataDAO.getDataForMessage(channelId, messageId).orElseThrow();

        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(loaded, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "5", "5"),
                new SelectedParameter("{s}", "s", "3", "3")), "userName"));
    }

    @Test
    void deserialization_legacy() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_dice", "CustomParameterConfig", """
                ---
                answerTargetChannelId: 123
                baseExpression: "{n}d{s}"
                """,
                "CustomParameterStateData", """
                ---
                selectedParameterValues:
                - "5"
                lockedForUserName: "userName"
                """);


        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "3", "3"),
                new SelectedParameter("{s}", "s", null, null)), "userName"));
    }

    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_dice", "CustomParameterConfig", """
                ---
                answerTargetChannelId: 123
                baseExpression: "{n}d{s}"
                diceParserSystem: "DICE_EVALUATOR"
                """,
                "CustomParameterStateData", """
                ---
                selectedParameterValues:
                - "5"
                lockedForUserName: "userName"
                """);


        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "3", "3"),
                new SelectedParameter("{s}", "s", null, null)), "userName"));
    }

    @Test
    void deserialization_legacy3() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_dice", "CustomParameterConfig", """
                ---
                answerTargetChannelId: 123
                baseExpression: "{n}d{s}"
                diceParserSystem: "DICE_EVALUATOR"
                answerFormatType: compact
                """,
                "CustomParameterStateData", """
                ---
                selectedParameterValues:
                - "5"
                lockedForUserName: "userName"
                """);


        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "3", "3"),
                new SelectedParameter("{s}", "s", null, null)), "userName"));
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_dice", "CustomParameterConfig", """
                ---
                answerTargetChannelId: 123
                baseExpression: "{n}d{s}"
                diceParserSystem: "DICE_EVALUATOR"
                answerFormatType: compact
                """,
                "CustomParameterStateDataV2", """
                ---
                selectedParameterValues:
                - parameterExpression: "{n}"
                  name: "n"
                  selectedValue: "5"
                  labelOfSelectedValue: "bonus"
                - parameterExpression: "{s}"
                  name: "s"
                  selectedValue: null
                  labelOfSelectedValue: null
                lockedForUserName: "userName"
                """);


        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "5", "bonus"),
                new SelectedParameter("{s}", "s", "3", "3")), "userName"));
    }

}