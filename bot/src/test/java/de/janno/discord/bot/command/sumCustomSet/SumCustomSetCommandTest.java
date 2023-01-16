package de.janno.discord.bot.command.sumCustomSet;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.Dice;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistanceManager;
import de.janno.discord.bot.persistance.PersistanceManagerImpl;
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
    PersistanceManager persistanceManager = mock(PersistanceManager.class);

    SumCustomSetConfig defaultConfig = new SumCustomSetConfig(null, ImmutableList.of(
            new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6"),
            new ButtonIdLabelAndDiceExpression("2_button", "3d6", "add 3d6"),
            new ButtonIdLabelAndDiceExpression("3_button", "4", "4"),
            new ButtonIdLabelAndDiceExpression("4_button", "2d10min10", "min10")
    ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, ResultImage.none);

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
        underTest = new SumCustomSetCommand(persistanceManager, diceMock, new CachingDiceEvaluator((minExcl, maxIncl) -> 0, 1000, 0));
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
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("clear", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessageWithState_roll() {
        String res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")))
                .orElseThrow().getContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }

    @Test
    void getEditButtonMessage_backLast() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("back", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));

        assertThat(res).isEmpty();
    }

    @Test
    void getEditButtonMessage_back() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("back", new SumCustomSetStateData(ImmutableList.of("+1d6", "+1d6"), "user1")));

        assertThat(res).isEmpty();
    }

    @Test
    void getCurrentMessageContentChange_clear() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("clear", new SumCustomSetStateData(ImmutableList.of("+1d6", "+1d6"), "user1")));

        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(defaultConfig).getContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }

    @Test
    void createNewButtonMessageWithState() {
        String res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")))
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
    void getAnswer_roll_true() {
        Optional<RollAnswer> res = underTest.getAnswer(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
        assertThat(res).isNotEmpty();
    }

    @Test
    void getAnswer_rollNoConfig_false() {
        Optional<RollAnswer> res = underTest.getAnswer(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of(), "user1")));
        assertThat(res).isEmpty();
    }

    @Test
    void getAnswer_modifyMessage_false() {
        Optional<RollAnswer> res = underTest.getAnswer(defaultConfig, new State<>("1_button", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_roll_true() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
        assertThat(res).isNotEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_rollNoConfig_false() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of(), "user1")));
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_modifyMessage_false() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("+1d6", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
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
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, ResultImage.polyhedral_3d_red_and_white));
    }

    @Test
    void getStartOptionsValidationMessage_valid() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6@Label;2d4")
                        .build())
                .build();

        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEmpty();
    }

    @Test
    void getStartOptionsValidationMessage_invalid() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6@Label;2d4;2d6*10")
                        .build())
                .build();

        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEmpty();
    }

    @Test
    void rollDice_1d6plus10() {


        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6", "+", "10"), "user1")))
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
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "3d6", "4", "2d10min10", "Roll", "Clear", "Back");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set1_button",
                        "sum_custom_set2_button",
                        "sum_custom_set3_button",
                        "sum_custom_set4_button",
                        "sum_custom_setroll",
                        "sum_custom_setclear",
                        "sum_custom_setback");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(defaultConfig).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "3d6", "4", "2d10min10", "Roll", "Clear", "Back");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set1_button",
                        "sum_custom_set2_button",
                        "sum_custom_set3_button",
                        "sum_custom_set4_button",
                        "sum_custom_setroll",
                        "sum_custom_setclear",
                        "sum_custom_setback");
    }

    @Test
    void checkPersistence() {
        PersistanceManager persistanceManager = new PersistanceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new SumCustomSetCommand(persistanceManager, diceMock, new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        UUID configUUID = UUID.randomUUID();
        SumCustomSetConfig config = new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, ResultImage.none);
        State<SumCustomSetStateData> state = new State<>("2_button", new SumCustomSetStateData(ImmutableList.of("+2d4"), "testUser"));
        Optional<MessageDataDTO> toSave = underTest.createMessageDataForNewMessage(configUUID, 1L, channelId, messageId, config, state);
        persistanceManager.saveMessageData(toSave.orElseThrow());

        underTest.updateCurrentMessageStateData(channelId, messageId, config, state);

        MessageDataDTO loaded = persistanceManager.getDataForMessage(channelId, messageId).orElseThrow();

        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(loaded, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("+2d4", "+1d6"), "testUser"));
    }

    @Test
    void deserialization_legacy() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                """,
                "SumCustomSetStateData", """
                ---
                diceExpressions:
                - "2d4"
                lockedForUserName: "testUser"
                """);


        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICEROLL_PARSER, true, AnswerFormatType.full, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("2d4", "+1d6"), "testUser"));
    }

    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
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
                """,
                "SumCustomSetStateData", """
                ---
                diceExpressions:
                - "2d4"
                lockedForUserName: "testUser"
                """);


        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("2d4", "+1d6"), "testUser"));
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
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
                """,
                "SumCustomSetStateData", """
                ---
                diceExpressions:
                - "2d4"
                lockedForUserName: "testUser"
                """);


        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.compact, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("2d4", "+1d6"), "testUser"));
    }


}