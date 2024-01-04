package de.janno.discord.bot.command.sumCustomSet;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.Dice;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.*;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(SnapshotExtension.class)
class SumCustomSetCommandTest {
    SumCustomSetCommand underTest;
    PersistenceManager persistenceManager = mock(PersistenceManager.class);

    SumCustomSetConfig defaultConfig = new SumCustomSetConfig(null, List.of(
            new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6"),
            new ButtonIdLabelAndDiceExpression("2_button", "3d6", "add 3d6"),
            new ButtonIdLabelAndDiceExpression("3_button", "4", "4"),
            new ButtonIdLabelAndDiceExpression("4_button", "2d10min10", "min10")
    ), DiceParserSystem.DICE_EVALUATOR, true, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);

    Dice diceMock;
    private Expect expect;

    static Stream<Arguments> generateGetEditButtonMessageData() {
        return Stream.of(
                Arguments.of(new State<>("1d4", new SumCustomSetStateDataV2(List.of(), "user1")), "Click the buttons to add dice to the set and then on Roll"),
                Arguments.of(new State<>("1d4", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d4", "1d4")), "user1")), "user1: 1d4"),
                Arguments.of(new State<>("1d4", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d4", "1d4")), null)), "1d4"),
                Arguments.of(new State<>("-1d4", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("-1d4", "-1d4")), "user1")), "user1: -1d4")
        );
    }

    @BeforeEach
    void setup() {
        diceMock = mock(Dice.class);
        underTest = new SumCustomSetCommand(persistenceManager, diceMock, new CachingDiceEvaluator((minExcl, maxIncl) -> 0, 1000, 0));


    }

    @ParameterizedTest(name = "{index} config={0}, buttonId={1} -> {2}")
    @MethodSource("generateGetEditButtonMessageData")
    void getEditButtonMessage(State<SumCustomSetStateDataV2> state, String expected) {
        Optional<String> res = underTest.getCurrentMessageContentChange(defaultConfig, state);
        assertThat(res).contains(expected);
    }

    @Test
    void getButtonMessageWithState_clear() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("clear", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6")), "user1")), 1L, 2L);
        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessageWithState_roll() {
        String res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("roll", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6")), "user1")), 1L, 2L)
                .orElseThrow().getDescriptionOrContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }

    @Test
    void getEditButtonMessage_backLast() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("back", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6")), "user1")), 1L, 2L);

        assertThat(res).isEmpty();
    }

    @Test
    void getEditButtonMessage_back() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("back", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("+1d6", "+1d6"), new ExpressionAndLabel("+1d6", "+1d6")), "user1")), 1L, 2L);

        assertThat(res).isEmpty();
    }

    @Test
    void getCurrentMessageContentChange_clear() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("clear", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("+1d6", "+1d6"), new ExpressionAndLabel("+1d6", "+1d6")), "user1")), 1L, 2L);

        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig).getDescriptionOrContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }

    @Test
    void createNewButtonMessageWithState() {
        String res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("roll", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6")), "user1")), 1L, 2L)
                .orElseThrow().getDescriptionOrContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }

    @Test
    void getName() {
        assertThat(underTest.getCommandId()).isEqualTo("sum_custom_set");
    }

    @Test
    void matchingComponentCustomId_match_legacy() {
        assertThat(underTest.matchingComponentCustomId("sum_custom_set\u0000x")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch_legacy() {
        assertThat(underTest.matchingComponentCustomId("sum_custom_se")).isFalse();
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("sum_custom_set2_button")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("sum_custom_set22_button")).isFalse();
    }

    @Test
    void copyButtonMessageToTheEnd_roll_true() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("roll", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6")), "user1")), 1L, 2L);
        assertThat(res).isNotEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_rollNoConfig_false() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("roll", new SumCustomSetStateDataV2(List.of(), "user1")), 1L, 2L);
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_modifyMessage_false() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("+1d6", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6")), "user1")), 1L, 2L);
        assertThat(res).isEmpty();
    }

    @Test
    void getCurrentMessageContentChange_1d6() {
        Optional<String> res = underTest.getCurrentMessageContentChange(defaultConfig, new State<>("+1d6", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6")), "user1")));
        assertThat(res).contains("user1: 1d6");
    }

    @Test
    void getConfigValuesFromStartOptions() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6@Label;2d4")
                        .build())
                .build();

        SumCustomSetConfig res = underTest.getConfigFromStartOptions(option, Locale.ENGLISH);

        assertThat(res).isEqualTo(new SumCustomSetConfig(null, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "2d4", "2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"), Locale.ENGLISH));
    }

    @Test
    void rollDice_1d6plus10() {


        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(defaultConfig, new State<>("roll", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6"), new ExpressionAndLabel("+", "+"), new ExpressionAndLabel("10", "10")), "user1")), 0L, 0L)
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d6 + 10 ⇒ 10");
        assertThat(res.getDescriptionOrContent()).isEqualTo("1d6+10: [0]");
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
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("roll", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6")), "user1")), 1L, 2L)
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "3d6", "4", "2d10min10", "Roll", "Clear", "Back");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set1_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set2_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set3_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set4_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_setroll00000000-0000-0000-0000-000000000000",
                        "sum_custom_setclear00000000-0000-0000-0000-000000000000",
                        "sum_custom_setback00000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "3d6", "4", "2d10min10", "Roll", "Clear", "Back");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set1_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set2_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set3_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set4_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_setroll00000000-0000-0000-0000-000000000000",
                        "sum_custom_setclear00000000-0000-0000-0000-000000000000",
                        "sum_custom_setback00000000-0000-0000-0000-000000000000");
    }

    @Test
    void checkPersistence() {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new SumCustomSetCommand(persistenceManager, diceMock, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));

        UUID configUUID = UUID.randomUUID();
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        SumCustomSetConfig config = new SumCustomSetConfig(123L, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, channelId, config);
        assertThat(toSave).isPresent();

        persistenceManager.saveMessageConfig(toSave.get());
        MessageConfigDTO loaded = persistenceManager.getMessageConfig(configUUID).orElseThrow();

        assertThat(toSave.get()).isEqualTo(loaded);
        SumCustomSetStateDataV2 stateData = new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("+2d4", "+2d4")), "testUser");

        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, channelId, messageId, "sum_custom_set", "SumCustomSetStateDataV2", Mapper.serializedObject(stateData));
        ConfigAndState<SumCustomSetConfig, SumCustomSetStateDataV2> configAndState = underTest.deserializeAndUpdateState(loaded, messageDataDTO, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("+2d4", "+2d4"), new ExpressionAndLabel("+1d6", "Label")), "testUser"));
    }

    @Test
    void deserialization_legacy() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_custom_set",
                "SumCustomSetStateData", """
                ---
                diceExpressions:
                - "2d4"
                lockedForUserName: "testUser"
                """);

        ConfigAndState<SumCustomSetConfig, SumCustomSetStateDataV2> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICEROLL_PARSER, true, false, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("2d4", "2d4"), new ExpressionAndLabel("+1d6", "Label")), "testUser"));
    }


    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                diceParserSystem: "DICE_EVALUATOR"
                alwaysSumResult: true
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_custom_set",
                "SumCustomSetStateData", """
                ---
                diceExpressions:
                - "2d4"
                lockedForUserName: "testUser"
                """);

        ConfigAndState<SumCustomSetConfig, SumCustomSetStateDataV2> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, false, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("2d4", "2d4"), new ExpressionAndLabel("+1d6", "Label")), "testUser"));
    }

    @Test
    void deserialization_legacy3() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                diceParserSystem: "DICE_EVALUATOR"
                alwaysSumResult: true
                answerFormatType: compact
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_custom_set",
                "SumCustomSetStateData", """
                ---
                diceExpressions:
                - "2d4"
                lockedForUserName: "testUser"
                """);

        ConfigAndState<SumCustomSetConfig, SumCustomSetStateDataV2> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, false, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("2d4", "2d4"), new ExpressionAndLabel("+1d6", "Label")), "testUser"));
    }

    @Test
    void deserialization_legacy4() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                diceParserSystem: "DICE_EVALUATOR"
                alwaysSumResult: true
                answerFormatType: full
                diceStyleAndColor:
                    diceImageStyle: "polyhedral_alies_v2"
                    configuredDefaultColor: "blue_and_silver"
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_custom_set",
                "SumCustomSetStateData", """
                ---
                diceExpressions:
                - "2d4"
                lockedForUserName: "testUser"
                """);

        ConfigAndState<SumCustomSetConfig, SumCustomSetStateDataV2> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, false, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("2d4", "2d4"), new ExpressionAndLabel("+1d6", "Label")), "testUser"));
    }

    @Test
    void deserialization_legacy5() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                diceParserSystem: "DICE_EVALUATOR"
                alwaysSumResult: true
                configLocale: "de"
                answerFormatType: full
                diceStyleAndColor:
                    diceImageStyle: "polyhedral_alies_v2"
                    configuredDefaultColor: "blue_and_silver"
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_custom_set",
                "SumCustomSetStateData", """
                ---
                diceExpressions:
                - "2d4"
                lockedForUserName: "testUser"
                """);

        ConfigAndState<SumCustomSetConfig, SumCustomSetStateDataV2> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, false, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("2d4", "2d4"), new ExpressionAndLabel("+1d6", "Label")), "testUser"));
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "+1d6"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "Bonus"
                  diceExpression: "+2d4"
                diceParserSystem: "DICE_EVALUATOR"
                alwaysSumResult: true
                hideExpressionInStatusAndAnswer: true
                answerFormatType: "full"
                configLocale: "de"
                diceStyleAndColor:
                  diceImageStyle: "polyhedral_alies_v2"
                  configuredDefaultColor: "blue_and_silver"
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_custom_set",
                "SumCustomSetStateDataV2", """
                ---
                diceExpressions:
                - expression: "+2d4"
                  label: "Bonus"
                lockedForUserName: "testUser"
                 """);

        ConfigAndState<SumCustomSetConfig, SumCustomSetStateDataV2> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "+1d6", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "Bonus", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("+2d4", "Bonus"), new ExpressionAndLabel("+1d6", "+1d6")), "testUser"));
    }

    @Test
    void configSerialization() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        SumCustomSetConfig config = new SumCustomSetConfig(123L, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN);
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, 2L, config);
        assertThat(toSave).isPresent();

        expect.toMatchSnapshot(toSave.get());
    }

}