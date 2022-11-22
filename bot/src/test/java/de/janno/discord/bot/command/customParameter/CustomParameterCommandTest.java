package de.janno.discord.bot.command.customParameter;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.RollAnswerConverter;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.Dice;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static de.janno.discord.bot.command.customParameter.CustomParameterCommand.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomParameterCommandTest {

    private static final String FIRST_SELECT_CUSTOM_ID = "custom_parameter\u0000{n}d{s}\u0000\u0000\u00001\u0000";
    private static final String LAST_SELECT_CUSTOM_ID = "custom_parameter\u0000{n}d{s}\u0000\u00001\u00002\u0000";
    private static final String LAST_SELECT_WITH_TARGET_CUSTOM_ID = "custom_parameter\u0000{n}d{s}\u00001234\u00001\u00002\u0000";
    private static final String LAST_SELECT_WITH_LABEL_CUSTOM_ID = "custom_parameter\u0000{n}d{s}@{label:Att/Par/Dam}\u0000\u00001\t2\u0000Att\u0000";
    private static final String CLEAR_CUSTOM_ID = "custom_parameter\u0000{n}d{s}\u0000\u00001\u0000clear\u0000";
    CustomParameterCommand underTest;
    MessageDataDAO messageDataDAO = mock(MessageDataDAO.class);

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
                Arguments.of("{number}d{sides:3/4/ab}", null)
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
        assertThat(Optional.ofNullable(res.getData()).map(CustomParameterStateData::getSelectedParameterValues)).contains(selectedParameterValues);
        assertThat(getFilledExpression(config, res)).isEqualTo(filledExpression);
        assertThat(getCurrentParameterExpression(config, res)).isEqualTo(currentParameterExpression);
        assertThat(getCurrentParameterName(config, res)).isEqualTo(currentParameterName);
        assertThat(hasMissingParameter(getFilledExpression(config, res))).isEqualTo(hasMissingParameter);
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("expression");
    }

    @Test
    void getAnswer_complete() {
        Optional<EmbedOrMessageDefinition> res = underTest.getAnswer(createConfigFromCustomId(LAST_SELECT_CUSTOM_ID), createParameterStateFromLegacyId(LAST_SELECT_CUSTOM_ID,
                "", "")).map(RollAnswerConverter::toEmbedOrMessageDefinition);

        assertThat(res).isPresent();
        assertThat(res.map(EmbedOrMessageDefinition::getTitle).orElseThrow()).startsWith("1d2 ⇒ ");
    }

    @Test
    void getAnswer_completeAndLabel() {
        Optional<EmbedOrMessageDefinition> res = underTest.getAnswer(createConfigFromCustomId(LAST_SELECT_WITH_LABEL_CUSTOM_ID), createParameterStateFromLegacyId(LAST_SELECT_WITH_LABEL_CUSTOM_ID,
                "", "")).map(RollAnswerConverter::toEmbedOrMessageDefinition);

        assertThat(res).isPresent();
        assertThat(res.map(EmbedOrMessageDefinition::getTitle).orElseThrow()).startsWith("Att ⇒ ");
        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent).orElseThrow()).startsWith("1d2: [");
    }

    @Test
    void getAnswer_notComplete() {
        Optional<EmbedOrMessageDefinition> res = underTest.getAnswer(createConfigFromCustomId(FIRST_SELECT_CUSTOM_ID), createParameterStateFromLegacyId(FIRST_SELECT_CUSTOM_ID,
                "", "")).map(RollAnswerConverter::toEmbedOrMessageDefinition);

        assertThat(res).isEmpty();
    }

    @Test
    void createNewButtonMessage() {
        MessageDefinition res = underTest.createNewButtonMessage(createConfigFromCustomId(LAST_SELECT_CUSTOM_ID));

        assertThat(res.getContent()).isEqualTo("*{n}*d*{s}*: Please select value for *{n}*");
        assertThat(res.getComponentRowDefinitions().stream()
                .flatMap(c -> c.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId)
        ).containsExactly("custom_parameter1",
                "custom_parameter2",
                "custom_parameter3",
                "custom_parameter4",
                "custom_parameter5",
                "custom_parameter6",
                "custom_parameter7",
                "custom_parameter8",
                "custom_parameter9",
                "custom_parameter10",
                "custom_parameter11",
                "custom_parameter12",
                "custom_parameter13",
                "custom_parameter14",
                "custom_parameter15");

        assertThat(res.getComponentRowDefinitions().stream()
                .flatMap(c -> c.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel)
        ).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15");
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
    void getCurrentMessageComponentChange_inSelection() {

        Optional<List<ComponentRowDefinition>> res = underTest.getCurrentMessageComponentChange(createConfigFromCustomId(FIRST_SELECT_CUSTOM_ID), createParameterStateFromLegacyId(FIRST_SELECT_CUSTOM_ID, "", ""));

        assertThat(res).isPresent();
        assertThat(res.orElseThrow().stream()
                .flatMap(c -> c.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId)
        ).containsExactly("custom_parameter1",
                "custom_parameter2",
                "custom_parameter3",
                "custom_parameter4",
                "custom_parameter5",
                "custom_parameter6",
                "custom_parameter7",
                "custom_parameter8",
                "custom_parameter9",
                "custom_parameter10",
                "custom_parameter11",
                "custom_parameter12",
                "custom_parameter13",
                "custom_parameter14",
                "custom_parameter15",
                "custom_parameterclear");
    }

    @Test
    void getCurrentMessageComponentChange_complete() {

        Optional<List<ComponentRowDefinition>> res = underTest.getCurrentMessageComponentChange(createConfigFromCustomId(LAST_SELECT_CUSTOM_ID), createParameterStateFromLegacyId(LAST_SELECT_CUSTOM_ID, "", ""));

        assertThat(res).isEmpty();
    }

    @Test
    void getCurrentMessageContentChange_complete() {

        Optional<String> res = underTest.getCurrentMessageContentChange(createConfigFromCustomId(LAST_SELECT_CUSTOM_ID), createParameterStateFromLegacyId(LAST_SELECT_CUSTOM_ID, "", ""));

        assertThat(res).isEmpty();
    }

    @Test
    void getCurrentMessageContentChange_inSelection() {

        Optional<String> res = underTest.getCurrentMessageContentChange(createConfigFromCustomId(FIRST_SELECT_CUSTOM_ID), createParameterStateFromLegacyId(FIRST_SELECT_CUSTOM_ID, "", ""));

        assertThat(res).contains("∶1d*{s}*: Please select value for *{s}*");
    }

    @Test
    void getCurrentMessageContentChange_inSelectionLocked() {

        Optional<String> res = underTest.getCurrentMessageContentChange(createConfigFromCustomId(FIRST_SELECT_CUSTOM_ID), createParameterStateFromLegacyId(FIRST_SELECT_CUSTOM_ID, "user1\u22361d{s}: Please select value for {s}", "user1"));

        assertThat(res).contains("user1∶1d*{s}*: Please select value for *{s}*");
    }

    @Test
    void createNewButtonMessageWithState_complete() {

        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(createConfigFromCustomId(LAST_SELECT_CUSTOM_ID), createParameterStateFromLegacyId(LAST_SELECT_CUSTOM_ID, "user1\u22361d{s}: Please select value for {s}", "user1"));

        assertThat(res.orElseThrow().getContent()).isEqualTo("*{n}*d*{s}*: Please select value for *{n}*");
        assertThat(res.orElseThrow().getComponentRowDefinitions().stream()
                .flatMap(c -> c.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId)
        ).containsExactly("custom_parameter1",
                "custom_parameter2",
                "custom_parameter3",
                "custom_parameter4",
                "custom_parameter5",
                "custom_parameter6",
                "custom_parameter7",
                "custom_parameter8",
                "custom_parameter9",
                "custom_parameter10",
                "custom_parameter11",
                "custom_parameter12",
                "custom_parameter13",
                "custom_parameter14",
                "custom_parameter15");
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
        State<CustomParameterStateData> state = new State<>("5", new CustomParameterStateData(ImmutableList.of("5"), "userName"));
        Optional<MessageDataDTO> toSave = underTest.createMessageDataForNewMessage(configUUID, 1L, channelId, messageId, config, state);
        messageDataDAO.saveMessageData(toSave.orElseThrow());
        underTest.updateCurrentMessageStateData(channelId, messageId, config, state);

        MessageDataDTO loaded = messageDataDAO.getDataForMessage(channelId, messageId).orElseThrow();

        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(loaded, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(ImmutableList.of("5", "3"), "userName"));
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
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(ImmutableList.of("5", "3"), "userName"));
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
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(ImmutableList.of("5", "3"), "userName"));
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
                "CustomParameterStateData", """
                ---
                selectedParameterValues:
                - "5"
                lockedForUserName: "userName"
                """);


        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(ImmutableList.of("5", "3"), "userName"));
    }

}