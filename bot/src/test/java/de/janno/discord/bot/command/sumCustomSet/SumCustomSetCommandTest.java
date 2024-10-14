package de.janno.discord.bot.command.sumCustomSet;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
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
            new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6", false, false, null),
            new ButtonIdLabelAndDiceExpression("2_button", "3d6", "add 3d6", false, false, null),
            new ButtonIdLabelAndDiceExpression("3_button", "4", "4", false, false, null),
            new ButtonIdLabelAndDiceExpression("4_button", "2d10min10", "min10", false, false, null)
    ), true, true, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);

    Expect expect;

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
        underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator((minExcl, maxIncl) -> 0));
    }

    @ParameterizedTest(name = "{index} config={0}, buttonId={1} -> {2}")
    @MethodSource("generateGetEditButtonMessageData")
    void getEditButtonMessage(State<SumCustomSetStateDataV2> state, String expected) {
        Optional<String> res = underTest.getCurrentMessageContentChange(defaultConfig, state, false);
        assertThat(res).contains(expected);
    }

    @Test
    void getButtonMessageWithState_clear() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("clear", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6")), "user1")), 1L, 2L);
        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessageWithState_roll() {
        EmbedOrMessageDefinition res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("roll", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6")), "user1")), 1L, 2L).orElseThrow();
        expect.toMatchSnapshot(res);
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
        EmbedOrMessageDefinition res = underTest.createSlashResponseMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, 1L);
        expect.toMatchSnapshot(res);
    }

    @Test
    void createSlashResponseMessageWithState() {
        EmbedOrMessageDefinition res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, new State<>("roll", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6")), "user1")), 1L, 2L).orElseThrow();
        expect.toMatchSnapshot(res);
    }

    @Test
    void getName() {
        assertThat(underTest.getCommandId()).isEqualTo("sum_custom_set");
    }

    @Test
    void matchingComponentCustomId_match_legacy() {
        assertThat(underTest.matchingComponentCustomId("sum_custom_set\u0000x")).isFalse();
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
        Optional<String> res = underTest.getCurrentMessageContentChange(defaultConfig, new State<>("+1d6", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("1d6", "1d6")), "user1")), false);
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
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "2d4", "2d4", false, false, null)
        ), true, true, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"), Locale.ENGLISH));
    }

    @Test
    void getConfigValuesFromStartOptionsSystemButtonsLineBreak() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6@Label;2d4;;")
                        .build())
                .build();

        SumCustomSetConfig res = underTest.getConfigFromStartOptions(option, Locale.ENGLISH);

        assertThat(res).isEqualTo(new SumCustomSetConfig(null, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "2d4", "2d4", false, false, null)
        ), true, true, true, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"), Locale.ENGLISH));
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
    void rollDice_PrePostfix() {
        underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator((minExcl, maxIncl) -> maxIncl));
        SumCustomSetConfig config = new SumCustomSetConfig(123L, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "5d6", "+5d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "2d10", "+2d10", true, false, null)
        ), true, true, true, "groupC(", ")", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.GERMAN);

        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(config, new State<>("roll", new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("+5d6", "5d6"), new ExpressionAndLabel("+2d10", "2d10")), "user1")), 0L, 0L)
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("5d6 2d10 ⇒ 5x6, 2x10");
        assertThat(res.getDescriptionOrContent()).isEqualTo("groupC(+5d6+2d10): [6, 6, 6, 6, 6] [10, 10]");
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
        List<ComponentRowDefinition> res = underTest.createSlashResponseMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, 1L).getComponentRowDefinitions();

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
    void directRollDisabled() {
        SumCustomSetConfig defaultConfig = new SumCustomSetConfig(null, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6", false, true, null),
                new ButtonIdLabelAndDiceExpression("2_button", "3d6", "add 3d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("3_button", "4*", "4*", false, true, null)
        ), true, true, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);

        List<ComponentRowDefinition> res = underTest.createSlashResponseMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), defaultConfig, 1L).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "3d6", "4*", "Roll", "Clear", "Back");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::isDisabled))
                .containsExactly(false, false, true, true, false, true);
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set1_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set2_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set3_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_setroll00000000-0000-0000-0000-000000000000",
                        "sum_custom_setclear00000000-0000-0000-0000-000000000000",
                        "sum_custom_setback00000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayout25Buttons() {
        List<ComponentRowDefinition> res = underTest.createSlashResponseMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new SumCustomSetConfig(null, ButtonHelper.parseString("+1d4;+1d6;+1d7;+1d8;+1d10;+1d12;+1d14;+1d16;+1d20;+1d24;+1d16;+1d30;+1d100;+1;+2;+3;+4;+5;-1;-2;-3;-4"), true, true, false, null, null, AnswerFormatType.without_expression, AnswerInteractionType.none, ResultImage.none, null, Locale.ENGLISH), 1L
        ).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("+1d4",
                        "+1d6",
                        "+1d7",
                        "+1d8",
                        "+1d10",
                        "+1d12",
                        "+1d14",
                        "+1d16",
                        "+1d20",
                        "+1d24",
                        "+1d16",
                        "+1d30",
                        "+1d100",
                        "+1",
                        "+2",
                        "+3",
                        "+4",
                        "+5",
                        "-1",
                        "-2",
                        "-3",
                        "-4",
                        "Roll",
                        "Clear",
                        "Back");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set1_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set2_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set3_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set4_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set5_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set6_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set7_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set8_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set9_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set10_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set11_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set12_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set13_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set14_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set15_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set16_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set17_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set18_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set19_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set20_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set21_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set22_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_setroll00000000-0000-0000-0000-000000000000",
                        "sum_custom_setclear00000000-0000-0000-0000-000000000000",
                        "sum_custom_setback00000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayoutLineBreakWithSystemLineBreak() {
        List<ComponentRowDefinition> res = underTest.createSlashResponseMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new SumCustomSetConfig(null, ButtonHelper.parseString("+1d4;+1d6;+1d7;+1d8;+1d10;+1d12;+1d14;+1d16;+1d20;;+1;+2;+3;+4;+5;-1;-2;-3;-4;;"), true, true, true, null, null, AnswerFormatType.without_expression, AnswerInteractionType.none, ResultImage.none, null, Locale.ENGLISH), 1L
        ).getComponentRowDefinitions();

        assertThat(res).hasSize(5);
        assertThat(res.get(0).getButtonDefinitions().stream().map(ButtonDefinition::getLabel)).containsExactly("+1d4",
                "+1d6",
                "+1d7",
                "+1d8",
                "+1d10");
        assertThat(res.get(1).getButtonDefinitions().stream().map(ButtonDefinition::getLabel)).containsExactly("+1d12",
                "+1d14",
                "+1d16",
                "+1d20");

        assertThat(res.get(2).getButtonDefinitions().stream().map(ButtonDefinition::getLabel)).containsExactly("+1",
                "+2",
                "+3",
                "+4",
                "+5");

        assertThat(res.get(3).getButtonDefinitions().stream().map(ButtonDefinition::getLabel)).containsExactly("-1",
                "-2",
                "-3",
                "-4");

        assertThat(res.get(4).getButtonDefinitions().stream().map(ButtonDefinition::getLabel)).containsExactly("Roll",
                "Clear",
                "Back");

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set1_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set2_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set3_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set4_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set5_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set6_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set7_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set8_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set9_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set10_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set11_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set12_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set13_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set14_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set15_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set16_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set17_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set18_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_setroll00000000-0000-0000-0000-000000000000",
                        "sum_custom_setclear00000000-0000-0000-0000-000000000000",
                        "sum_custom_setback00000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayoutLineBreakWithoutSystemLineBreak() {
        List<ComponentRowDefinition> res = underTest.createSlashResponseMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new SumCustomSetConfig(null, ButtonHelper.parseString("+1d4;+1d6;+1d7;+1d8;+1d10;+1d12;+1d14;+1d16;+1d20;;+1;+2;+3;+4;+5;-1;-2;-3;-4;;"), true, true, false, null, null, AnswerFormatType.without_expression, AnswerInteractionType.none, ResultImage.none, null, Locale.ENGLISH), 1L
        ).getComponentRowDefinitions();

        assertThat(res).hasSize(5);
        assertThat(res.get(0).getButtonDefinitions().stream().map(ButtonDefinition::getLabel)).containsExactly("+1d4",
                "+1d6",
                "+1d7",
                "+1d8",
                "+1d10");
        assertThat(res.get(1).getButtonDefinitions().stream().map(ButtonDefinition::getLabel)).containsExactly("+1d12",
                "+1d14",
                "+1d16",
                "+1d20");

        assertThat(res.get(2).getButtonDefinitions().stream().map(ButtonDefinition::getLabel)).containsExactly("+1",
                "+2",
                "+3",
                "+4",
                "+5");

        assertThat(res.get(3).getButtonDefinitions().stream().map(ButtonDefinition::getLabel)).containsExactly("-1",
                "-2",
                "-3",
                "-4",
                "Roll");

        assertThat(res.get(4).getButtonDefinitions().stream().map(ButtonDefinition::getLabel)).containsExactly(
                "Clear",
                "Back");

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set1_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set2_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set3_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set4_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set5_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set6_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set7_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set8_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set9_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set10_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set11_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set12_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set13_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set14_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set15_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set16_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set17_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set18_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_setroll00000000-0000-0000-0000-000000000000",
                        "sum_custom_setclear00000000-0000-0000-0000-000000000000",
                        "sum_custom_setback00000000-0000-0000-0000-000000000000");
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

        Optional<String> res = underTest.getStartOptionsValidationMessage(option, 0L, 0L, Locale.ENGLISH);

        assertThat(res).isEmpty();
    }

    @Test
    void getStartOptionsValidationMessage_invalidLayout() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6@Label;;;2d6")
                        .build())
                .build();

        Optional<String> res = underTest.getStartOptionsValidationMessage(option, 0L, 0L, Locale.ENGLISH);
        assertThat(res).contains("Empty rows is not allowed");
    }

    @Test
    void getStartOptionsValidationMessage_toManyButtons() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("R" +
                                "a;b;c;d; -4@-4; -3@-3; -2@-2; -1@-1;+0@0;+1@1;+2@2;+3@3;+4@4;+5@5;+6@6; 1d12; 1d6; 1d12@1D12; 2d12@2D12; 3d12@3D12; 4d12@4D12;1d4@1D4;1d6@1D6;")
                        .build())
                .build();

        Optional<String> res = underTest.getStartOptionsValidationMessage(option, 0L, 0L, Locale.ENGLISH);
        assertThat(res).contains("The maximum are 5 rows with each 5 buttons");
    }

    @Test
    void getStartOptionsValidationMessage_toManyRows() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("R" +
                                "a;;b;;c;;d;;e;;")
                        .build())
                .build();

        Optional<String> res = underTest.getStartOptionsValidationMessage(option, 0L, 0L, Locale.ENGLISH);
        assertThat(res).contains("The maximum are 5 rows with each 5 buttons");
    }


    @Test
    public void testToCommandString() {
        SumCustomSetConfig config = new SumCustomSetConfig(123L, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", true, false, null)
        ), true, true, true, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN);

        assertThat(config.toCommandOptionsString()).isEqualTo("buttons: +1d6@Label;;+2d4;; always_sum_result: true hide_expression_in_answer: true answer_format: full answer_interaction: none dice_image_style: polyhedral_alies_v2 dice_image_color: blue_and_silver target_channel: <#123>");
    }

    @Test
    void checkPersistence() {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        UUID configUUID = UUID.randomUUID();
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        SumCustomSetConfig config = new SumCustomSetConfig(123L, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", true, false, null)
        ), true, true, true, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
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
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", false, false, null)
        ), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
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
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", false, false, null)
        ), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
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
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", false, false, null)
        ), true, false, false, null, null, AnswerFormatType.compact, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
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
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", false, false, null)
        ), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.ENGLISH));
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
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", false, false, null)
        ), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("2d4", "2d4"), new ExpressionAndLabel("+1d6", "Label")), "testUser"));
    }

    @Test
    void deserialization_legacy6() {
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
                new ButtonIdLabelAndDiceExpression("1_button", "+1d6", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "Bonus", "+2d4", false, false, null)
        ), true, true, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("+2d4", "Bonus"), new ExpressionAndLabel("+1d6", "+1d6")), "testUser"));
    }

    @Test
    void deserialization_legacy7() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "+1d6"
                  diceExpression: "+1d6"
                  newLine: false
                - buttonId: "2_button"
                  label: "Bonus"
                  diceExpression: "+2d4"
                  newLine: true
                diceParserSystem: "DICE_EVALUATOR"
                alwaysSumResult: true
                hideExpressionInStatusAndAnswer: true
                systemButtonNewLine: true
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
                new ButtonIdLabelAndDiceExpression("1_button", "+1d6", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "Bonus", "+2d4", true, false, null)
        ), true, true, true, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("+2d4", "Bonus"), new ExpressionAndLabel("+1d6", "+1d6")), "testUser"));
    }

    @Test
    void deserialization_legancy8() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "+1d6"
                  diceExpression: "+1d6"
                  newLine: false
                  directRoll: true
                - buttonId: "2_button"
                  label: "Bonus"
                  diceExpression: "+2d4"
                  newLine: true
                  directRoll: false
                diceParserSystem: "DICE_EVALUATOR"
                alwaysSumResult: true
                hideExpressionInStatusAndAnswer: true
                systemButtonNewLine: true
                prefix: "groupC("
                postfix: ")"
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
                new ButtonIdLabelAndDiceExpression("1_button", "+1d6", "+1d6", false, true, null),
                new ButtonIdLabelAndDiceExpression("2_button", "Bonus", "+2d4", true, false, null)
        ), true, true, true, "groupC(", ")", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("+2d4", "Bonus"), new ExpressionAndLabel("+1d6", "+1d6")), "testUser"));
    }

    @Test
    void deserialization_legancy9() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "+1d6"
                  diceExpression: "+1d6"
                  newLine: false
                - buttonId: "2_button"
                  label: "Bonus"
                  diceExpression: "+2d4"
                  newLine: true
                diceParserSystem: "DICE_EVALUATOR"
                alwaysSumResult: true
                hideExpressionInStatusAndAnswer: true
                systemButtonNewLine: true
                prefix: "groupC("
                postfix: ")"
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
                new ButtonIdLabelAndDiceExpression("1_button", "+1d6", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "Bonus", "+2d4", true, false, null)
        ), true, true, true, "groupC(", ")", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("+2d4", "Bonus"), new ExpressionAndLabel("+1d6", "+1d6")), "testUser"));
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
                  newLine: false
                - buttonId: "2_button"
                  label: "Bonus"
                  diceExpression: "+2d4"
                  newLine: true
                  emoji: "🪙"
                diceParserSystem: "DICE_EVALUATOR"
                alwaysSumResult: true
                hideExpressionInStatusAndAnswer: true
                systemButtonNewLine: true
                prefix: "groupC("
                postfix: ")"
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
                new ButtonIdLabelAndDiceExpression("1_button", "+1d6", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "Bonus", "+2d4", true, false, "🪙")
        ), true, true, true, "groupC(", ")", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateDataV2(List.of(new ExpressionAndLabel("+2d4", "Bonus"), new ExpressionAndLabel("+1d6", "+1d6")), "testUser"));
    }

    @Test
    void configSerialization() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        SumCustomSetConfig config = new SumCustomSetConfig(123L, List.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", true, false, "🪙")
        ), true, true, true, "groupC(", ")", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN);
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, 2L, config);
        assertThat(toSave).isPresent();

        expect.toMatchSnapshot(toSave.get());
    }

}