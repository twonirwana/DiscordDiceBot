package de.janno.discord.bot.command.holdReroll;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.RollAnswerConverter;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.*;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
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

class HoldRerollCommandTest {

    HoldRerollCommand underTest;

    private static Stream<Arguments> generateValidateData() {
        return Stream.of(
                Arguments.of(new HoldRerollConfig(null, 6, ImmutableSet.of(7), ImmutableSet.of(), ImmutableSet.of(), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), "reroll set [7] contains a number bigger then the sides of the die 6"),
                Arguments.of(new HoldRerollConfig(null, 6, ImmutableSet.of(), ImmutableSet.of(7), ImmutableSet.of(), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), "success set [7] contains a number bigger then the sides of the die 6"),
                Arguments.of(new HoldRerollConfig(null, 6, ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of(7), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), "failure set [7] contains a number bigger then the sides of the die 6"),
                Arguments.of(new HoldRerollConfig(null, 6, ImmutableSet.of(1, 2, 3, 4, 5, 6), ImmutableSet.of(), ImmutableSet.of(), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), "The reroll must not contain all numbers"),
                Arguments.of(new HoldRerollConfig(null, 6, ImmutableSet.of(), ImmutableSet.of(0), ImmutableSet.of(0), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), null),
                Arguments.of(new HoldRerollConfig(null, 6, ImmutableSet.of(2, 3, 4), ImmutableSet.of(5, 6), ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), null)
        );
    }

    @BeforeEach
    void setup() {
        underTest = new HoldRerollCommand(mock(PersistenceManager.class), new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

    }

    @Test
    void getName() {
        String res = underTest.getCommandId();
        assertThat(res).isEqualTo("hold_reroll");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("sides", "reroll_set", "success_set", "failure_set");
    }

    @Test
    void getDiceResult_withoutReroll() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("finish", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 0)), 0L, 0L)
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("6d6 ⇒ Success: 2 and Failure: 1");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[**1**,2,3,4,**5**,**6**]");
    }

    @Test
    void getDiceResult_withReroll() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("finish", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)), 0L, 0L)
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("6d6 ⇒ Success: 2, Failure: 1 and Rerolls: 2");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[**1**,2,3,4,**5**,**6**]");
    }

    @Test
    void matchingComponentCustomId_match_legacy() {
        assertThat(underTest.matchingComponentCustomId("hold_reroll\u00001;2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch_legacy() {
        assertThat(underTest.matchingComponentCustomId("hold_rerol")).isFalse();
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("hold_reroll2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("hold_reroll22")).isFalse();
    }

    @Test
    void getButtonMessageWithState_clear() {
        String res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("clear", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)), 1L, 2L)
                .orElseThrow().getDescriptionOrContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @Test
    void getButtonMessageWithState_finish() {
        String res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("finish", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)), 1L, 2L)
                .orElseThrow().getDescriptionOrContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @Test
    void getButtonMessageWithState_noRerollPossible() {
        String res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("reroll", new HoldRerollStateData(ImmutableList.of(1, 1, 1, 5, 5, 6), 2)), 1L, 2L)
                .orElseThrow().getDescriptionOrContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @Test
    void getCurrentMessageContentChange_rerollPossible() {
        String res = underTest.getCurrentMessageContentChange(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("reroll", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)))
                .orElseThrow();

        assertThat(res).isEqualTo("[**1**,2,3,4,**5**,**6**] = 2 successes and 1 failures");
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")))
                .getDescriptionOrContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("generateValidateData")
    void validate(HoldRerollConfig config, String expected) {
        if (expected == null) {
            assertThat(underTest.validate(config)).isEmpty();
        } else {
            assertThat(underTest.validate(config)).contains(expected);
        }
    }

    @Test
    void getCurrentMessageComponentChange_reroll() {
        List<ComponentRowDefinition> res = underTest.getCurrentMessageComponentChange(UUID.fromString("00000000-0000-0000-0000-000000000000"), new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("reroll", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)), 1L, 1L)
                .orElseThrow();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("Reroll", "Finish", "Clear");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("hold_rerollreroll00000000-0000-0000-0000-000000000000", "hold_rerollfinish00000000-0000-0000-0000-000000000000", "hold_rerollclear00000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayoutWithState_finish() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("finish", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)), 1L, 2L)
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("hold_reroll100000000-0000-0000-0000-000000000000",
                        "hold_reroll200000000-0000-0000-0000-000000000000",
                        "hold_reroll300000000-0000-0000-0000-000000000000",
                        "hold_reroll400000000-0000-0000-0000-000000000000",
                        "hold_reroll500000000-0000-0000-0000-000000000000",
                        "hold_reroll600000000-0000-0000-0000-000000000000",
                        "hold_reroll700000000-0000-0000-0000-000000000000",
                        "hold_reroll800000000-0000-0000-0000-000000000000",
                        "hold_reroll900000000-0000-0000-0000-000000000000",
                        "hold_reroll1000000000-0000-0000-0000-000000000000",
                        "hold_reroll1100000000-0000-0000-0000-000000000000",
                        "hold_reroll1200000000-0000-0000-0000-000000000000",
                        "hold_reroll1300000000-0000-0000-0000-000000000000",
                        "hold_reroll1400000000-0000-0000-0000-000000000000",
                        "hold_reroll1500000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayoutWithState_clear() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("clear", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)), 1L, 2L)
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("hold_reroll100000000-0000-0000-0000-000000000000",
                        "hold_reroll200000000-0000-0000-0000-000000000000",
                        "hold_reroll300000000-0000-0000-0000-000000000000",
                        "hold_reroll400000000-0000-0000-0000-000000000000",
                        "hold_reroll500000000-0000-0000-0000-000000000000",
                        "hold_reroll600000000-0000-0000-0000-000000000000",
                        "hold_reroll700000000-0000-0000-0000-000000000000",
                        "hold_reroll800000000-0000-0000-0000-000000000000",
                        "hold_reroll900000000-0000-0000-0000-000000000000",
                        "hold_reroll1000000000-0000-0000-0000-000000000000",
                        "hold_reroll1100000000-0000-0000-0000-000000000000",
                        "hold_reroll1200000000-0000-0000-0000-000000000000",
                        "hold_reroll1300000000-0000-0000-0000-000000000000",
                        "hold_reroll1400000000-0000-0000-0000-000000000000",
                        "hold_reroll1500000000-0000-0000-0000-000000000000");
    }

    @Test
    void getCurrentMessageComponentChange_3() {
        List<ComponentRowDefinition> res = underTest.getCurrentMessageComponentChange(UUID.fromString("00000000-0000-0000-0000-000000000000"), new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("3", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)), 1L, 1L)
                .orElseThrow();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("Reroll", "Finish", "Clear");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("hold_rerollreroll00000000-0000-0000-0000-000000000000", "hold_rerollfinish00000000-0000-0000-0000-000000000000", "hold_rerollclear00000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayoutWithState_3_finished() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("3", new HoldRerollStateData(ImmutableList.of(1, 1, 1, 5, 5, 6), 2)), 1L, 2L)
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("hold_reroll100000000-0000-0000-0000-000000000000",
                        "hold_reroll200000000-0000-0000-0000-000000000000",
                        "hold_reroll300000000-0000-0000-0000-000000000000",
                        "hold_reroll400000000-0000-0000-0000-000000000000",
                        "hold_reroll500000000-0000-0000-0000-000000000000",
                        "hold_reroll600000000-0000-0000-0000-000000000000",
                        "hold_reroll700000000-0000-0000-0000-000000000000",
                        "hold_reroll800000000-0000-0000-0000-000000000000",
                        "hold_reroll900000000-0000-0000-0000-000000000000",
                        "hold_reroll1000000000-0000-0000-0000-000000000000",
                        "hold_reroll1100000000-0000-0000-0000-000000000000",
                        "hold_reroll1200000000-0000-0000-0000-000000000000",
                        "hold_reroll1300000000-0000-0000-0000-000000000000",
                        "hold_reroll1400000000-0000-0000-0000-000000000000",
                        "hold_reroll1500000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("hold_reroll100000000-0000-0000-0000-000000000000",
                        "hold_reroll200000000-0000-0000-0000-000000000000",
                        "hold_reroll300000000-0000-0000-0000-000000000000",
                        "hold_reroll400000000-0000-0000-0000-000000000000",
                        "hold_reroll500000000-0000-0000-0000-000000000000",
                        "hold_reroll600000000-0000-0000-0000-000000000000",
                        "hold_reroll700000000-0000-0000-0000-000000000000",
                        "hold_reroll800000000-0000-0000-0000-000000000000",
                        "hold_reroll900000000-0000-0000-0000-000000000000",
                        "hold_reroll1000000000-0000-0000-0000-000000000000",
                        "hold_reroll1100000000-0000-0000-0000-000000000000",
                        "hold_reroll1200000000-0000-0000-0000-000000000000",
                        "hold_reroll1300000000-0000-0000-0000-000000000000",
                        "hold_reroll1400000000-0000-0000-0000-000000000000",
                        "hold_reroll1500000000-0000-0000-0000-000000000000");
    }

    @Test
    void getCurrentMessageContentChange() {
        assertThat(underTest.getCurrentMessageContentChange(new HoldRerollConfig(
                null,
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("3", new HoldRerollStateData(ImmutableList.of(1, 1, 1, 5, 5, 6), 2)))).isEmpty();
    }


    @Test
    void checkPersistence() {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new HoldRerollCommand(persistenceManager, mock(DiceUtils.class));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
        UUID configUUID = UUID.randomUUID();
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        HoldRerollConfig config = new HoldRerollConfig(123L, 10, ImmutableSet.of(9, 10), ImmutableSet.of(7, 8, 9, 10), ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, channelId, config);
        assertThat(toSave).isPresent();

        persistenceManager.saveMessageConfig(toSave.get());
        MessageConfigDTO loaded = persistenceManager.getMessageConfig(configUUID).orElseThrow();

        assertThat(toSave.get()).isEqualTo(loaded);
        HoldRerollStateData stateData = new HoldRerollStateData(ImmutableList.of(1, 2, 10), 2);

        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, channelId, messageId, "hold_reroll", "HoldRerollStateData", Mapper.serializedObject(stateData));
        ConfigAndState<HoldRerollConfig, HoldRerollStateData> configAndState = underTest.deserializeAndUpdateState(loaded, messageDataDTO, "reroll");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new HoldRerollStateData(ImmutableList.of(1, 2, 0), 3));
    }

    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "hold_reroll", "HoldRerollConfig", """
                ---
                answerTargetChannelId: 123
                sidesOfDie: 10
                rerollSet:
                - 9
                - 10
                successSet:
                - 7
                - 8
                - 9
                - 10
                failureSet:
                - 1
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "hold_reroll",
                "HoldRerollStateData", """
                ---
                currentResults:
                - 1
                - 2
                - 10
                rerollCounter: 2
                """);


        ConfigAndState<HoldRerollConfig, HoldRerollStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "reroll");
        assertThat(configAndState.getConfig()).isEqualTo(new HoldRerollConfig(123L, 10, ImmutableSet.of(9, 10), ImmutableSet.of(7, 8, 9, 10), ImmutableSet.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new HoldRerollStateData(ImmutableList.of(1, 2, 1), 3));
    }

    @Test
    void deserialization() {

        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "hold_reroll", "HoldRerollConfig", """
                ---
                answerTargetChannelId: 123
                sidesOfDie: 10
                rerollSet:
                - 9
                - 10
                successSet:
                - 7
                - 8
                - 9
                - 10
                failureSet:
                - 1
                answerFormatType: compact
                 """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "hold_reroll",
                "HoldRerollStateData", """
                ---
                  currentResults:
                  - 1
                  - 2
                  - 10
                  rerollCounter: 2
                  """);


        ConfigAndState<HoldRerollConfig, HoldRerollStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "reroll");
        assertThat(configAndState.getConfig()).isEqualTo(new HoldRerollConfig(123L, 10, ImmutableSet.of(9, 10), ImmutableSet.of(7, 8, 9, 10), ImmutableSet.of(1), AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none")));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new HoldRerollStateData(ImmutableList.of(1, 2, 1), 3));
    }

}