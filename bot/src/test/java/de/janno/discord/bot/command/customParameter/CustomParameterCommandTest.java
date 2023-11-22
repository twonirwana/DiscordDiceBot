package de.janno.discord.bot.command.customParameter;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.Dice;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.*;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(SnapshotExtension.class)
class CustomParameterCommandTest {

    CustomParameterCommand underTest;
    PersistenceManager persistenceManager = mock(PersistenceManager.class);
    private Expect expect;

    private static Stream<Arguments> generateParameterExpression2ButtonValuesData() {
        return Stream.of(
                Arguments.of("{test}", IntStream.range(1, 16).mapToObj(i -> new CustomParameterCommand.ButtonLabelAndValue(String.valueOf(i), "id%d".formatted(i))).toList()),
                Arguments.of("{test:2<=>4}", IntStream.range(1, 4).mapToObj(i -> new CustomParameterCommand.ButtonLabelAndValue(String.valueOf(i + 1), "id%d".formatted(i))).toList()),
                Arguments.of("{test:2<=>1}", ImmutableList.of(new CustomParameterCommand.ButtonLabelAndValue("2", "id1"))),
                Arguments.of("{test:-2<=>1}", IntStream.range(1, 5).mapToObj(i -> new CustomParameterCommand.ButtonLabelAndValue(String.valueOf(i - 3), "id%d".formatted(i))).toList()),
                Arguments.of("{test:-10<=>-5}", IntStream.range(1, 7).mapToObj(i -> new CustomParameterCommand.ButtonLabelAndValue(String.valueOf(i - 11), "id%d".formatted(i))).toList()),
                Arguments.of("{test:1d6/+5/abc}", ImmutableList.of(new CustomParameterCommand.ButtonLabelAndValue("1d6", "id1"), new CustomParameterCommand.ButtonLabelAndValue("+5", "id2"), new CustomParameterCommand.ButtonLabelAndValue("abc", "id3"))),
                Arguments.of("{test:1d6@d6/+5@Bonus/abc}", ImmutableList.of(new CustomParameterCommand.ButtonLabelAndValue("d6", "id1"), new CustomParameterCommand.ButtonLabelAndValue("Bonus", "id2"), new CustomParameterCommand.ButtonLabelAndValue("abc", "id3")))
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
                Arguments.of("{number}d{sides\t}", "Expression contains invalid character: `\t`"),
                Arguments.of("{number}d\t{sides}", "Expression contains invalid character: `\t`"),
                Arguments.of("{number}d{sides:/}", null), //invalid range is mapped to 1-15
                Arguments.of("{number}d{}", "A parameter expression must not be empty"),
                Arguments.of("1d6", "The expression needs at least one parameter expression like `{name}`"),
                Arguments.of("{a1}{a2}{a3}{a4}{a6}", "The expression is allowed a maximum of 4 variables"),
                Arguments.of("{number:3<=>6}d{sides:6/10/12}", null),
                Arguments.of("{number}{a:a/c/b/d/d}{sides:3<=>6}", "The following expression is invalid: `1a3`. The error is: No matching operator for 'a3', non-functional text and value names must to be surrounded by '' or []. Use /custom_parameter help to get more information on how to use the command."),
                Arguments.of("{number}d{sides:3/4/'ab'}", null),
                Arguments.of("{number}d{sides:3/4/'ab'}@roll", null),
                Arguments.of("{number}d{sides:['11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111','21111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111']@big,6}@roll", null)
        );
    }

    public static Stream<Arguments> generateExpression2Parameters() {
        return Stream.of(
                Arguments.of("{number}d{sides}", List.of(new Parameter("{number}", "number", IntStream.range(1, 16)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ParameterOption(s, s, "id%s".formatted(s)))
                        .toList()), new Parameter("{sides}", "sides", IntStream.range(1, 16)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ParameterOption(s, s, "id%s".formatted(s)))
                        .toList()))),
                Arguments.of("{number}d{sides:-2<=>2}", List.of(new Parameter("{number}", "number", IntStream.range(1, 16)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ParameterOption(s, s, "id%s".formatted(s)))
                        .toList()), new Parameter("{sides:-2<=>2}", "sides", IntStream.range(1, 6)
                        .boxed()
                        .map(s -> new Parameter.ParameterOption(String.valueOf(s - 3), String.valueOf(s - 3), "id%s".formatted(s)))
                        .toList()))),
                Arguments.of("{number}d{sides:4/12/+5}", List.of(new Parameter("{number}", "number", IntStream.range(1, 16)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ParameterOption(s, s, "id%s".formatted(s)))
                        .toList()), new Parameter("{sides:4/12/+5}", "sides", List.of(new Parameter.ParameterOption("4", "4", "id1"),
                        new Parameter.ParameterOption("12", "12", "id2"),
                        new Parameter.ParameterOption("+5", "+5", "id3"))))),
                Arguments.of("{number}d{sides:4@D4/12@D12/+5@Bonus}", List.of(new Parameter("{number}", "number", IntStream.range(1, 16)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ParameterOption(s, s, "id%s".formatted(s)))
                        .toList()), new Parameter("{sides:4@D4/12@D12/+5@Bonus}", "sides", List.of(new Parameter.ParameterOption("4", "D4", "id1"),
                        new Parameter.ParameterOption("12", "D12", "id2"),
                        new Parameter.ParameterOption("+5", "Bonus", "id3")))))
        );
    }

    @BeforeEach
    void setup() {
        underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
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
    void getButtonValues(String parameterExpression, List<CustomParameterCommand.ButtonLabelAndValue> expectedResult) {
        CustomParameterConfig config = new CustomParameterConfig(null, "1d6 + {a} + " + parameterExpression, DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        List<CustomParameterCommand.ButtonLabelAndValue> res = underTest.getButtons(config, parameterExpression);
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
                .build(), 0L, 0L, Locale.ENGLISH);
        if (expectedResult == null) {
            assertThat(res).isEmpty();
        } else {
            assertThat(res).contains(expectedResult);
        }
    }

    @Test
    void debug() {
        Optional<String> res = underTest.getStartOptionsValidationMessage(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("expression")
                        .stringValue("{number}d{sides}@{label:attack/damage}").build())
                .build(), 0L, 0L, Locale.ENGLISH);
        assertThat(res).isEmpty();
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

    @Test
    public void getCommandDefinition() {
        expect.toMatchSnapshot(underTest.getCommandDefinition());
    }

    @Test
    public void getId() {
        expect.toMatchSnapshot(underTest.getCommandId());
    }

    @Test
    void checkPersistence() {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new CustomParameterCommand(persistenceManager, mock(Dice.class), new CachingDiceEvaluator((minExcl, maxIncl) -> 0, 10, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
        UUID configUUID = UUID.randomUUID();
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        CustomParameterConfig config = new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, channelId, config);
        assertThat(toSave).isPresent();

        persistenceManager.saveMessageConfig(toSave.get());
        MessageConfigDTO loaded = persistenceManager.getMessageConfig(configUUID).orElseThrow();

        assertThat(toSave.get()).isEqualTo(loaded);
        CustomParameterStateData stateData = new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", null, null),
                new SelectedParameter("{s}", "s", null, null)), "userName");
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, channelId, messageId, "custom_parameter", "CustomParameterStateDataV2", Mapper.serializedObject(stateData));
        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(loaded, messageDataDTO, "5", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "5", "5"),
                new SelectedParameter("{s}", "s", null, null)), "userName"));
    }

    @Test
    void deserialization_legacy() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_parameter", "CustomParameterConfig", """
                ---
                answerTargetChannelId: 123
                baseExpression: "{n}d{s}"
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_parameter",
                "CustomParameterStateData", """
                ---
                selectedParameterValues:
                - "5"
                lockedForUserName: "userName"
                """);

        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "3", "3"),
                new SelectedParameter("{s}", "s", null, null)), "userName"));
    }

    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_parameter", "CustomParameterConfig", """
                ---
                answerTargetChannelId: 123
                baseExpression: "{n}d{s}"
                diceParserSystem: "DICE_EVALUATOR"
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_parameter",
                "CustomParameterStateData", """
                ---
                selectedParameterValues:
                - "5"
                lockedForUserName: "userName"
                """);

        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "3", "3"),
                new SelectedParameter("{s}", "s", null, null)), "userName"));
    }

    @Test
    void deserialization_legacy3() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_parameter", "CustomParameterConfig", """
                ---
                answerTargetChannelId: 123
                baseExpression: "{n}d{s}"
                diceParserSystem: "DICE_EVALUATOR"
                answerFormatType: compact
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_parameter",
                "CustomParameterStateData", """
                ---
                selectedParameterValues:
                - "5"
                lockedForUserName: "userName"
                """);

        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "3", "3"),
                new SelectedParameter("{s}", "s", null, null)), "userName"));
    }

    @Test
    void deserialization_legacy4() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_parameter", "CustomParameterConfig", """
                ---
                answerTargetChannelId: 123
                baseExpression: "{n}d{s}"
                diceParserSystem: "DICE_EVALUATOR"
                answerFormatType: compact
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_parameter",
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

        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "5", "bonus"),
                new SelectedParameter("{s}", "s", "3", "3")), "userName"));
    }

    @Test
    void deserialization_legacy5() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_parameter", "CustomParameterConfig", """
                ---
                answerTargetChannelId: 123
                baseExpression: "{n}d{s}"
                diceParserSystem: "DICE_EVALUATOR"
                answerFormatType: full
                diceStyleAndColor:
                    diceImageStyle: "polyhedral_alies_v2"
                    configuredDefaultColor: "blue_and_silver"
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_parameter",
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

        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "5", "bonus"),
                new SelectedParameter("{s}", "s", "3", "3")), "userName"));
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_parameter", "CustomParameterConfig", """
                ---
                answerTargetChannelId: 123
                baseExpression: "{n}d{s}"
                diceParserSystem: "DICE_EVALUATOR"
                answerFormatType: full
                configLocale: "de"
                diceStyleAndColor:
                    diceImageStyle: "polyhedral_alies_v2"
                    configuredDefaultColor: "blue_and_silver"
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_parameter",
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

        ConfigAndState<CustomParameterConfig, CustomParameterStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "3", "userName");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new CustomParameterStateData(List.of(
                new SelectedParameter("{n}", "n", "5", "bonus"),
                new SelectedParameter("{s}", "s", "3", "3")), "userName"));
    }

    @Test
    void configSerialization() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        CustomParameterConfig config = new CustomParameterConfig(123L, "{n}d{s}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN);
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, 2L, config);
        assertThat(toSave).isPresent();

        expect.toMatchSnapshot(toSave.get());
    }
}