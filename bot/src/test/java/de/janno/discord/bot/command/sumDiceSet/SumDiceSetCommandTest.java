package de.janno.discord.bot.command.sumDiceSet;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.*;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SumDiceSetCommandTest {
    SumDiceSetCommand underTest;

    static Stream<Arguments> generateGetEditButtonMessageData() {
        return Stream.of(
                Arguments.of(new State<>("+1d4", new SumDiceSetStateData(ImmutableList.of())), "1d4"),
                Arguments.of(new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of())), "1d6"),
                Arguments.of(new State<>("+1d8", new SumDiceSetStateData(ImmutableList.of())), "1d8"),
                Arguments.of(new State<>("+1d10", new SumDiceSetStateData(ImmutableList.of())), "1d10"),
                Arguments.of(new State<>("+1d12", new SumDiceSetStateData(ImmutableList.of())), "1d12"),
                Arguments.of(new State<>("+1d20", new SumDiceSetStateData(ImmutableList.of())), "1d20"),
                Arguments.of(new State<>("+1", new SumDiceSetStateData(ImmutableList.of())), "1"),
                Arguments.of(new State<>("+5", new SumDiceSetStateData(ImmutableList.of())), "5"),
                Arguments.of(new State<>("+10", new SumDiceSetStateData(ImmutableList.of())), "10"),

                Arguments.of(new State<>("-1d4", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("-1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("-1d8", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d8", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("-1d10", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d10", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("-1d12", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d12", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("-1d20", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d20", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("-1", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("m", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1d4", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", -1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", -1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1d8", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d8", -1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1d10", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d10", -1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1d12", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d12", -1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1d20", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d20", -1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("m", -1)))), "Click on the buttons to add dice to the set"),

                Arguments.of(new State<>("-1d4", new SumDiceSetStateData(ImmutableList.of())), "-1d4"),
                Arguments.of(new State<>("-1d6", new SumDiceSetStateData(ImmutableList.of())), "-1d6"),
                Arguments.of(new State<>("-1d8", new SumDiceSetStateData(ImmutableList.of())), "-1d8"),
                Arguments.of(new State<>("-1d10", new SumDiceSetStateData(ImmutableList.of())), "-1d10"),
                Arguments.of(new State<>("-1d12", new SumDiceSetStateData(ImmutableList.of())), "-1d12"),
                Arguments.of(new State<>("-1d20", new SumDiceSetStateData(ImmutableList.of())), "-1d20"),
                Arguments.of(new State<>("-1", new SumDiceSetStateData(ImmutableList.of())), "-1"),
                Arguments.of(new State<>("-5", new SumDiceSetStateData(ImmutableList.of())), "-5"),

                Arguments.of(new State<>("-5", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("m", 10)))), "5"),
                Arguments.of(new State<>("-5", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("m", 2)))), "-3"),
                Arguments.of(new State<>("+5", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("m", -2)))), "3"),
                Arguments.of(new State<>("+5", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("m", -10)))), "-5"),

                Arguments.of(new State<>("+1d4", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "2d4 +1d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +2d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new State<>("+1d8", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +2d8 +1d10 +1d12 +1d20"),
                Arguments.of(new State<>("+1d10", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +2d10 +1d12 +1d20"),
                Arguments.of(new State<>("+1d12", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d10 +2d12 +1d20"),
                Arguments.of(new State<>("+1d20", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d10 +1d12 +2d20"),
                Arguments.of(new State<>("+1", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d10 +1d12 +1d20 +1"),

                Arguments.of(new State<>("-1d4", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new State<>("-1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new State<>("-1d8", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d10 +1d12 +1d20"),
                Arguments.of(new State<>("-1d10", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d12 +1d20"),
                Arguments.of(new State<>("-1d12", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d10 +1d20"),
                Arguments.of(new State<>("-1d20", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d10 +1d12"),
                Arguments.of(new State<>("-1", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d10 +1d12 +1d20 -1"),

                Arguments.of(new State<>("+1d4", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 100), new DiceKeyAndValue("d6", 100), new DiceKeyAndValue("d8", 100), new DiceKeyAndValue("d10", 100), new DiceKeyAndValue("d12", 100), new DiceKeyAndValue("d20", 100)))), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 100), new DiceKeyAndValue("d6", 100), new DiceKeyAndValue("d8", 100), new DiceKeyAndValue("d10", 100), new DiceKeyAndValue("d12", 100), new DiceKeyAndValue("d20", 100)))), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new State<>("+1d8", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 100), new DiceKeyAndValue("d6", 100), new DiceKeyAndValue("d8", 100), new DiceKeyAndValue("d10", 100), new DiceKeyAndValue("d12", 100), new DiceKeyAndValue("d20", 100)))), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new State<>("+1d10", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 100), new DiceKeyAndValue("d6", 100), new DiceKeyAndValue("d8", 100), new DiceKeyAndValue("d10", 100), new DiceKeyAndValue("d12", 100), new DiceKeyAndValue("d20", 100)))), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new State<>("+1d12", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 100), new DiceKeyAndValue("d6", 100), new DiceKeyAndValue("d8", 100), new DiceKeyAndValue("d10", 100), new DiceKeyAndValue("d12", 100), new DiceKeyAndValue("d20", 100)))), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new State<>("+1d20", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 100), new DiceKeyAndValue("d6", 100), new DiceKeyAndValue("d8", 100), new DiceKeyAndValue("d10", 100), new DiceKeyAndValue("d12", 100), new DiceKeyAndValue("d20", 100)))), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20")
        );
    }

    @BeforeEach
    void setup() {
        underTest = new SumDiceSetCommand(mock(PersistenceManager.class), new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

    }

    @Test
    void getButtonMessageWithState_clear() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("clear", new SumDiceSetStateData(ImmutableList.of(
                        new DiceKeyAndValue("d4", 1),
                        new DiceKeyAndValue("d6", 1),
                        new DiceKeyAndValue("d8", 1),
                        new DiceKeyAndValue("d10", 1),
                        new DiceKeyAndValue("d12", 1),
                        new DiceKeyAndValue("d20", 1)))),
                1L, 2L);
        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessageWithState_roll() {
        String res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(
                                new DiceKeyAndValue("d4", 1),
                                new DiceKeyAndValue("d6", 1),
                                new DiceKeyAndValue("d8", 1),
                                new DiceKeyAndValue("d10", 1),
                                new DiceKeyAndValue("d12", 1),
                                new DiceKeyAndValue("d20", 1)))),
                        1L, 2L)
                .orElseThrow().getDescriptionOrContent();
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void updateDiceSet_NegativeModifier_x2() {
        List<DiceKeyAndValue> res = underTest.updateDiceSet(ImmutableList.of(
                new DiceKeyAndValue("d4", -1),
                new DiceKeyAndValue("d6", -2),
                new DiceKeyAndValue("d8", -3),
                new DiceKeyAndValue("d10", -4),
                new DiceKeyAndValue("d12", 5),
                new DiceKeyAndValue("m", -10)), "x2");
        assertThat(res).isEqualTo(ImmutableList.of(
                new DiceKeyAndValue("d4", -2),
                new DiceKeyAndValue("d6", -4),
                new DiceKeyAndValue("d8", -6),
                new DiceKeyAndValue("d10", -8),
                new DiceKeyAndValue("d12", 10),
                new DiceKeyAndValue("m", -20)));
    }

    @Test
    void updateDiceSet_limit() {
        List<DiceKeyAndValue> res = underTest.updateDiceSet(ImmutableList.of(new DiceKeyAndValue("d4", 51)), "x2");
        assertThat(res).isEqualTo(ImmutableList.of(new DiceKeyAndValue("d4", 100)));
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)).getDescriptionOrContent();
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void createNewButtonMessageWithState() {
        String res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 51)))), 1L, 2L)
                .orElseThrow().getDescriptionOrContent();
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @ParameterizedTest(name = "{index} config={0}, buttonId={1} -> {2}")
    @MethodSource("generateGetEditButtonMessageData")
    void getCurrentMessageContentChange(State<SumDiceSetStateData> state, String expected) {
        State<SumDiceSetStateData> updated = underTest.updateState(state.getButtonValue(), state.getData());
        Optional<String> res = underTest.getCurrentMessageContentChange(new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), updated);
        assertThat(res).contains(expected);
    }

    @Test
    void getName() {
        assertThat(underTest.getCommandId()).isEqualTo("sum_dice_set");
    }

    @Test
    void matchingComponentCustomId_match_legacy() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_set\u0000x")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch_legacy() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_se")).isFalse();
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_set+1")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_set2+1")).isFalse();
    }

    @Test
    void getCommandDefinition() {
        CommandDefinition res = underTest.getCommandDefinition();

        assertThat(res.toString()).isEqualTo("CommandDefinition(name=sum_dice_set, description=Legacy command, use /sum_custom_set, nameLocales=[], descriptionLocales=[], options=[CommandDefinitionOption(type=SUB_COMMAND, name=start, nameLocales=[], description=Legacy command, use /sum_custom_set, descriptionLocales=[], required=false, choices=[], options=[CommandDefinitionOption(type=CHANNEL, name=target_channel, nameLocales=[], description=Another channel where the answer will be given, descriptionLocales=[], required=false, choices=[], options=[], minValue=null, maxValue=null, autoComplete=false), CommandDefinitionOption(type=STRING, name=answer_format, nameLocales=[], description=How the answer will be displayed, descriptionLocales=[], required=false, choices=[CommandDefinitionOptionChoice(name=full, value=full, nameLocales=[]), CommandDefinitionOptionChoice(name=without_expression, value=without_expression, nameLocales=[]), CommandDefinitionOptionChoice(name=only_dice, value=only_dice, nameLocales=[]), CommandDefinitionOptionChoice(name=compact, value=compact, nameLocales=[]), CommandDefinitionOptionChoice(name=minimal, value=minimal, nameLocales=[])], options=[], minValue=null, maxValue=null, autoComplete=false)], minValue=null, maxValue=null, autoComplete=false), CommandDefinitionOption(type=SUB_COMMAND, name=help, nameLocales=[LocaleValue[locale=de, value=hilfe]], description=Get help for /sum_dice_set, descriptionLocales=[LocaleValue[locale=de, value=Get help for /[sum_dice_set]]], required=false, choices=[], options=[], minValue=null, maxValue=null, autoComplete=false)])");
    }

    @Test
    void getAnswer_roll_true() {
        Optional<RollAnswer> res = underTest.getAnswer(new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                "d6", 1
        )))), 0L, 0L);
        assertThat(res).isNotEmpty();
    }

    @Test
    void getAnswer_rollNoConfig_false() {
        Optional<RollAnswer> res = underTest.getAnswer(new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("roll", new SumDiceSetStateData(ImmutableList.of())), 0L, 0L);
        assertThat(res).isEmpty();
    }

    @Test
    void getAnswer_modifyMessage_false() {
        Optional<RollAnswer> res = underTest.getAnswer(new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                "d6", 1
        )))), 0L, 0L);
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_roll_true() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                "d6", 1
        )))), 1L, 2L);
        assertThat(res).isNotEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_rollNoConfig_false() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("roll", new SumDiceSetStateData(ImmutableList.of())), 1L, 2L);
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_modifyMessage_false() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                "d6", 1
        )))), 1L, 2L);
        assertThat(res).isEmpty();
    }

    @Test
    void getCurrentMessageContentChange_1d6() {
        Optional<String> res = underTest.getCurrentMessageContentChange(new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                "d6", 1
        )))));
        assertThat(res).contains("1d6");
    }

    @Test
    void getConfigValuesFromStartOptions() {
        Config res = underTest.getConfigFromStartOptions(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("target_channel")
                        .channelIdValue(null)
                        .build())
                .build(), Locale.ENGLISH);

        assertThat(res).isEqualTo(new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
    }


    @Test
    void rollDice_1d4plus1d6plus10() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                        "d4", 1),
                new DiceKeyAndValue("d6", 1),
                new DiceKeyAndValue("m", 10)
        ))), 0L, 0L).orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d4 +1d6 +10 ⇒ 12");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[1, 1, 10]");
    }

    @Test
    void rollDice_minus1d4plus1d6minux10() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                        "d4", -1),
                new DiceKeyAndValue("d6", 1),
                new DiceKeyAndValue("m", -10)
        ))), 0L, 0L).orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("-1d4 +1d6 -10 ⇒ -10");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[-1, 1, -10]");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getCommandDefinition().getOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("start", "help");
    }

    @Test
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(
                        new DiceKeyAndValue("d4", -1),
                        new DiceKeyAndValue("d6", 1),
                        new DiceKeyAndValue("m", -10)
                ))), 1L, 2L)
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("+1d4", "-1d4", "+1d6", "-1d6", "x2", "+1d8", "-1d8", "+1d10", "-1d10", "Clear", "+1d12", "-1d12", "+1d20", "-1d20", "Roll", "+1", "-1", "+5", "-5", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_dice_set+1d400000000-0000-0000-0000-000000000000",
                        "sum_dice_set-1d400000000-0000-0000-0000-000000000000",
                        "sum_dice_set+1d600000000-0000-0000-0000-000000000000",
                        "sum_dice_set-1d600000000-0000-0000-0000-000000000000",
                        "sum_dice_setx200000000-0000-0000-0000-000000000000",
                        "sum_dice_set+1d800000000-0000-0000-0000-000000000000",
                        "sum_dice_set-1d800000000-0000-0000-0000-000000000000",
                        "sum_dice_set+1d1000000000-0000-0000-0000-000000000000",
                        "sum_dice_set-1d1000000000-0000-0000-0000-000000000000",
                        "sum_dice_setclear00000000-0000-0000-0000-000000000000",
                        "sum_dice_set+1d1200000000-0000-0000-0000-000000000000",
                        "sum_dice_set-1d1200000000-0000-0000-0000-000000000000",
                        "sum_dice_set+1d2000000000-0000-0000-0000-000000000000",
                        "sum_dice_set-1d2000000000-0000-0000-0000-000000000000",
                        "sum_dice_setroll00000000-0000-0000-0000-000000000000",
                        "sum_dice_set+100000000-0000-0000-0000-000000000000",
                        "sum_dice_set-100000000-0000-0000-0000-000000000000",
                        "sum_dice_set+500000000-0000-0000-0000-000000000000",
                        "sum_dice_set-500000000-0000-0000-0000-000000000000",
                        "sum_dice_set+1000000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("+1d4", "-1d4", "+1d6", "-1d6", "x2", "+1d8", "-1d8", "+1d10", "-1d10", "Clear", "+1d12", "-1d12", "+1d20", "-1d20", "Roll", "+1", "-1", "+5", "-5", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_dice_set+1d400000000-0000-0000-0000-000000000000",
                        "sum_dice_set-1d400000000-0000-0000-0000-000000000000",
                        "sum_dice_set+1d600000000-0000-0000-0000-000000000000",
                        "sum_dice_set-1d600000000-0000-0000-0000-000000000000",
                        "sum_dice_setx200000000-0000-0000-0000-000000000000",
                        "sum_dice_set+1d800000000-0000-0000-0000-000000000000",
                        "sum_dice_set-1d800000000-0000-0000-0000-000000000000",
                        "sum_dice_set+1d1000000000-0000-0000-0000-000000000000",
                        "sum_dice_set-1d1000000000-0000-0000-0000-000000000000",
                        "sum_dice_setclear00000000-0000-0000-0000-000000000000",
                        "sum_dice_set+1d1200000000-0000-0000-0000-000000000000",
                        "sum_dice_set-1d1200000000-0000-0000-0000-000000000000",
                        "sum_dice_set+1d2000000000-0000-0000-0000-000000000000",
                        "sum_dice_set-1d2000000000-0000-0000-0000-000000000000",
                        "sum_dice_setroll00000000-0000-0000-0000-000000000000",
                        "sum_dice_set+100000000-0000-0000-0000-000000000000",
                        "sum_dice_set-100000000-0000-0000-0000-000000000000",
                        "sum_dice_set+500000000-0000-0000-0000-000000000000",
                        "sum_dice_set-500000000-0000-0000-0000-000000000000",
                        "sum_dice_set+1000000000-0000-0000-0000-000000000000");
    }

    @Test
    void checkPersistence() {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new SumDiceSetCommand(persistenceManager, mock(DiceUtils.class));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        Config config = new Config(123L, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, channelId, config);
        assertThat(toSave).isPresent();

        persistenceManager.saveMessageConfig(toSave.get());
        MessageConfigDTO loaded = persistenceManager.getMessageConfig(configUUID).orElseThrow();

        assertThat(toSave.get()).isEqualTo(loaded);
        SumDiceSetStateData stateData = new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", 3), new DiceKeyAndValue("m", -4)));

        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, channelId, messageId, "sum_dice_set", "SumDiceSetStateData", Mapper.serializedObject(stateData));
        ConfigAndState<Config, SumDiceSetStateData> configAndState = underTest.deserializeAndUpdateState(loaded, messageDataDTO, "+1d6");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", 4), new DiceKeyAndValue("m", -4))));
    }

    @Test
    void deserialization_legacy() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "sum_dice_set", "Config", """
                ---
                answerTargetChannelId: 123
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_dice_set",
                "SumDiceSetStateData", """
                ---
                diceSet:
                - diceKey: "d6"
                  value: 3
                - diceKey: "m"
                  value: -4
                """);

        ConfigAndState<Config, SumDiceSetStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "+1d6");
        assertThat(configAndState.getConfig()).isEqualTo(new Config(123L, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", 4), new DiceKeyAndValue("m", -4))));
    }


    @Test
    void deserialization() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "sum_dice_set", "Config", """
                ---
                answerTargetChannelId: 123
                answerFormatType: compact
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_dice_set",
                "SumDiceSetStateData", """
                ---
                diceSet:
                - diceKey: "d6"
                  value: 3
                - diceKey: "m"
                  value: -4
                """);

        ConfigAndState<Config, SumDiceSetStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "+1d6");
        assertThat(configAndState.getConfig()).isEqualTo(new Config(123L, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", 4), new DiceKeyAndValue("m", -4))));
    }

}