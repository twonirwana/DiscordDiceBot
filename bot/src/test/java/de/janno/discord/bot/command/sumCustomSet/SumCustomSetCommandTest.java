package de.janno.discord.bot.command.sumCustomSet;

import com.google.common.collect.ImmutableList;
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
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SumCustomSetCommandTest {
    SumCustomSetCommand underTest;
    PersistenceManager persistenceManager = mock(PersistenceManager.class);

    SumCustomSetConfig defaultConfig = new SumCustomSetConfig(null, ImmutableList.of(
            new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6"),
            new ButtonIdLabelAndDiceExpression("2_button", "3d6", "add 3d6"),
            new ButtonIdLabelAndDiceExpression("3_button", "4", "4"),
            new ButtonIdLabelAndDiceExpression("4_button", "2d10min10", "min10")
    ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));

    Dice diceMock;

    static Stream<Arguments> generateGetEditButtonMessageData() {
        return Stream.of(
                Arguments.of(new State<>("1d4", new SumCustomSetStateData(ImmutableList.of(), "user1")), "Click the buttons to add dice to the set and then on Roll"),
                Arguments.of(new State<>("1d4", new SumCustomSetStateData(ImmutableList.of("1d4"), "user1")), "user1: 1d4"),
                Arguments.of(new State<>("1d4", new SumCustomSetStateData(ImmutableList.of("1d4"), null)), "1d4"),
                Arguments.of(new State<>("-1d4", new SumCustomSetStateData(ImmutableList.of("-1d4"), "user1")), "user1: -1d4")
        );
    }

    @BeforeEach
    void setup() {
        diceMock = mock(Dice.class);
        underTest = new SumCustomSetCommand(persistenceManager, diceMock, new CachingDiceEvaluator((minExcl, maxIncl) -> 0, 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

    }


    @ParameterizedTest(name = "{index} config={0}, buttonId={1} -> {2}")
    @MethodSource("generateGetEditButtonMessageData")
    void getEditButtonMessage(State<SumCustomSetStateData> state, String expected) {
        Optional<String> res = underTest.getCurrentMessageContentChange(defaultConfig, state);
        assertThat(res).contains(expected);
    }

    @Test
    void getButtonMessageWithState_clear() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("clear", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")), 1L, 2L);
        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessageWithState_roll() {
        String res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")), 1L, 2L)
                .orElseThrow().getContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }

    @Test
    void getEditButtonMessage_backLast() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("back", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")), 1L, 2L);

        assertThat(res).isEmpty();
    }

    @Test
    void getEditButtonMessage_back() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("back", new SumCustomSetStateData(ImmutableList.of("+1d6", "+1d6"), "user1")), 1L, 2L);

        assertThat(res).isEmpty();
    }

    @Test
    void getCurrentMessageContentChange_clear() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("clear", new SumCustomSetStateData(ImmutableList.of("+1d6", "+1d6"), "user1")), 1L, 2L);

        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig).getContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }

    @Test
    void createNewButtonMessageWithState() {
        String res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")), 1L, 2L)
                .orElseThrow().getContent();
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
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")), 1L, 2L);
        assertThat(res).isNotEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_rollNoConfig_false() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of(), "user1")), 1L, 2L);
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_modifyMessage_false() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("+1d6", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")), 1L, 2L);
        assertThat(res).isEmpty();
    }

    @Test
    void getCurrentMessageContentChange_1d6() {
        Optional<String> res = underTest.getCurrentMessageContentChange(defaultConfig, new State<>("+1d6", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
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

        SumCustomSetConfig res = underTest.getConfigFromStartOptions(option);

        assertThat(res).isEqualTo(new SumCustomSetConfig(null, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "2d4", "2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white")));
    }


    @Test
    void rollDice_1d6plus10() {


        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6", "+", "10"), "user1")), 0L, 0L)
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d6+10 ⇒ 10");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[0]");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("buttons", "always_sum_result");
    }

    @Test
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")), 1L, 2L)
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
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
        UUID configUUID = UUID.randomUUID();
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        SumCustomSetConfig config = new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, channelId, config);
        assertThat(toSave).isPresent();

        persistenceManager.saveMessageConfig(toSave.get());
        MessageConfigDTO loaded = persistenceManager.getMessageConfig(configUUID).orElseThrow();

        assertThat(toSave.get()).isEqualTo(loaded);
        SumCustomSetStateData stateData = new SumCustomSetStateData(ImmutableList.of("+2d4"), "testUser");

        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, channelId, messageId, "sum_custom_set", "SumCustomSetStateData", Mapper.serializedObject(stateData));
        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(loaded, messageDataDTO, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("+2d4", "+1d6"), "testUser"));
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

        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICEROLL_PARSER, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("2d4", "+1d6"), "testUser"));
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

        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("2d4", "+1d6"), "testUser"));
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

        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none")));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("2d4", "+1d6"), "testUser"));
    }

    @Test
    void deserialization() {
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

        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver")));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("2d4", "+1d6"), "testUser"));
    }

}