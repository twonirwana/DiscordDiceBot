package de.janno.discord.bot.command.holdReroll;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HoldRerollCommandTest {

    HoldRerollCommand underTest;

    private static Stream<Arguments> generateValidateData() {
        return Stream.of(
                Arguments.of(new HoldRerollConfig(null, 6, ImmutableSet.of(7), ImmutableSet.of(), ImmutableSet.of(), AnswerFormatType.full), "reroll set [7] contains a number bigger then the sides of the die 6"),
                Arguments.of(new HoldRerollConfig(null, 6, ImmutableSet.of(), ImmutableSet.of(7), ImmutableSet.of(), AnswerFormatType.full), "success set [7] contains a number bigger then the sides of the die 6"),
                Arguments.of(new HoldRerollConfig(null, 6, ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of(7), AnswerFormatType.full), "failure set [7] contains a number bigger then the sides of the die 6"),
                Arguments.of(new HoldRerollConfig(null, 6, ImmutableSet.of(1, 2, 3, 4, 5, 6), ImmutableSet.of(), ImmutableSet.of(), AnswerFormatType.full), "The reroll must not contain all numbers"),
                Arguments.of(new HoldRerollConfig(null, 6, ImmutableSet.of(), ImmutableSet.of(0), ImmutableSet.of(0), AnswerFormatType.full), null),
                Arguments.of(new HoldRerollConfig(null, 6, ImmutableSet.of(2, 3, 4), ImmutableSet.of(5, 6), ImmutableSet.of(1), AnswerFormatType.full), null)
        );
    }

    @BeforeEach
    void setup() {
        underTest = new HoldRerollCommand(mock(MessageDataDAO.class), new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
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
        EmbedOrMessageDefinition res = underTest.getAnswer(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full), new State<>("finish", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 0)))
                .orElseThrow().toEmbedOrMessageDefinition();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("6d6 ⇒ Success: 2 and Failure: 1");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[**1**,2,3,4,**5**,**6**]");
    }

    @Test
    void getDiceResult_withReroll() {
        EmbedOrMessageDefinition res = underTest.getAnswer(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full), new State<>("finish", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)))
                .orElseThrow().toEmbedOrMessageDefinition();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("6d6 ⇒ Success: 2, Failure: 1 and Rerolls: 2");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[**1**,2,3,4,**5**,**6**]");
    }

    @Test
    void getConfigFromEvent_roll3d6() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll\u00003\u0000EMPTY\u00006\u00002;3;4\u00005;6\u00001\u00000\u0000");

        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new HoldRerollConfig(
                null,
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1), AnswerFormatType.full));
    }

    @Test
    void getStateFromEvent_roll3d6() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll\u00003\u0000EMPTY\u00006\u00002;3;4\u00005;6\u00001\u00000\u0000");

        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("3", new HoldRerollStateData(ImmutableList.of(1, 1, 1), 0)));
    }

    @Test
    void getConfigFromEvent_finish() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll\u0000finish,1;2;3;4;5;6,6,2;3;4,5;6,1,0");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new HoldRerollConfig(
                null,
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1), AnswerFormatType.full));
    }

    @Test
    void getStateFromEvent_finish() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll\u0000finish,1;2;3;4;5;6,6,2;3;4,5;6,1,0");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("finish", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 0)));
    }

    @Test
    void getConfigFromEvent_clear() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll\u0000clear,1;2;3;4;5;6,6,2;3;4,5;6,1,0");
        assertThat(underTest.getConfigFromEvent(event))
                .isEqualTo(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full));
    }

    @Test
    void getStateFromEvent_clear() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll\u0000clear,1;2;3;4;5;6,6,2;3;4,5;6,1,0");
        assertThat(underTest.getStateFromEvent(event))
                .isEqualTo(new State<>("clear", new HoldRerollStateData(ImmutableList.of(), 0)));
    }

    @Test
    void getConfigFromEvent_reroll() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll\u0000reroll,1;2;3;4;5;6,6,2;3;4,5;6,1,1");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new HoldRerollConfig(
                null,
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1), AnswerFormatType.full));
    }

    @Test
    void getStateFromEvent_reroll() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll\u0000reroll,1;2;3;4;5;6,6,2;3;4,5;6,1,1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("reroll", new HoldRerollStateData(ImmutableList.of(1, 1, 1, 1, 5, 6), 2)));
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
        String res = underTest.createNewButtonMessageWithState(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full), new State<>("clear", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)))
                .orElseThrow().getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @Test
    void getButtonMessageWithState_finish() {
        String res = underTest.createNewButtonMessageWithState(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full), new State<>("finish", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)))
                .orElseThrow().getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @Test
    void getButtonMessageWithState_noRerollPossible() {
        String res = underTest.createNewButtonMessageWithState(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full), new State<>("reroll", new HoldRerollStateData(ImmutableList.of(1, 1, 1, 5, 5, 6), 2)))
                .orElseThrow().getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @Test
    void getCurrentMessageContentChange_rerollPossible() {
        String res = underTest.getCurrentMessageContentChange(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full), new State<>("reroll", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)))
                .orElseThrow();

        assertThat(res).isEqualTo("[**1**,2,3,4,**5**,**6**] = 2 successes and 1 failures");
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full))
                .getContent();

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
    void getStateFromEvent() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll\u0000finish,1;2;3;4;5;6,6,2;3;4,5;6,1,0");

        State<HoldRerollStateData> res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new State<>("finish", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 0)));
    }

    @Test
    void getCurrentMessageComponentChange_reroll() {
        List<ComponentRowDefinition> res = underTest.getCurrentMessageComponentChange(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full), new State<>("reroll", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)))
                .orElseThrow();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("Reroll", "Finish", "Clear");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("hold_rerollreroll", "hold_rerollfinish", "hold_rerollclear");
    }

    @Test
    void getButtonLayoutWithState_finish() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full), new State<>("finish", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("hold_reroll1",
                        "hold_reroll2",
                        "hold_reroll3",
                        "hold_reroll4",
                        "hold_reroll5",
                        "hold_reroll6",
                        "hold_reroll7",
                        "hold_reroll8",
                        "hold_reroll9",
                        "hold_reroll10",
                        "hold_reroll11",
                        "hold_reroll12",
                        "hold_reroll13",
                        "hold_reroll14",
                        "hold_reroll15");
    }

    @Test
    void getButtonLayoutWithState_clear() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full), new State<>("clear", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("hold_reroll1",
                        "hold_reroll2",
                        "hold_reroll3",
                        "hold_reroll4",
                        "hold_reroll5",
                        "hold_reroll6",
                        "hold_reroll7",
                        "hold_reroll8",
                        "hold_reroll9",
                        "hold_reroll10",
                        "hold_reroll11",
                        "hold_reroll12",
                        "hold_reroll13",
                        "hold_reroll14",
                        "hold_reroll15");
    }

    @Test
    void getCurrentMessageComponentChange_3() {
        List<ComponentRowDefinition> res = underTest.getCurrentMessageComponentChange(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full), new State<>("3", new HoldRerollStateData(ImmutableList.of(1, 2, 3, 4, 5, 6), 2)))
                .orElseThrow();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("Reroll", "Finish", "Clear");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("hold_rerollreroll", "hold_rerollfinish", "hold_rerollclear");
    }

    @Test
    void getButtonLayoutWithState_3_finished() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full), new State<>("3", new HoldRerollStateData(ImmutableList.of(1, 1, 1, 5, 5, 6), 2)))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("hold_reroll1",
                        "hold_reroll2",
                        "hold_reroll3",
                        "hold_reroll4",
                        "hold_reroll5",
                        "hold_reroll6",
                        "hold_reroll7",
                        "hold_reroll8",
                        "hold_reroll9",
                        "hold_reroll10",
                        "hold_reroll11",
                        "hold_reroll12",
                        "hold_reroll13",
                        "hold_reroll14",
                        "hold_reroll15");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(new HoldRerollConfig(
                        null,
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1), AnswerFormatType.full))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("hold_reroll1",
                        "hold_reroll2",
                        "hold_reroll3",
                        "hold_reroll4",
                        "hold_reroll5",
                        "hold_reroll6",
                        "hold_reroll7",
                        "hold_reroll8",
                        "hold_reroll9",
                        "hold_reroll10",
                        "hold_reroll11",
                        "hold_reroll12",
                        "hold_reroll13",
                        "hold_reroll14",
                        "hold_reroll15");
    }

    @Test
    void getCurrentMessageContentChange() {
        assertThat(underTest.getCurrentMessageContentChange(new HoldRerollConfig(
                null,
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1), AnswerFormatType.full), new State<>("3", new HoldRerollStateData(ImmutableList.of(1, 1, 1, 5, 5, 6), 2)))).isEmpty();
    }


    @Test
    void checkPersistence() {
        MessageDataDAO messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new HoldRerollCommand(messageDataDAO, mock(DiceUtils.class));

        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        UUID configUUID = UUID.randomUUID();
        HoldRerollConfig config = new HoldRerollConfig(123L, 10, ImmutableSet.of(9, 10), ImmutableSet.of(7, 8, 9, 10), ImmutableSet.of(1), AnswerFormatType.full);
        State<HoldRerollStateData> state = new State<>("reroll", new HoldRerollStateData(ImmutableList.of(1, 2, 10), 2));
        Optional<MessageDataDTO> toSave = underTest.createMessageDataForNewMessage(configUUID, 1L, channelId, messageId, config, state);
        messageDataDAO.saveMessageData(toSave.orElseThrow());
        underTest.updateCurrentMessageStateData(channelId, messageId, config, state);

        MessageDataDTO loaded = messageDataDAO.getDataForMessage(channelId, messageId).orElseThrow();

        ConfigAndState<HoldRerollConfig, HoldRerollStateData> configAndState = underTest.deserializeAndUpdateState(loaded, "reroll");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new HoldRerollStateData(ImmutableList.of(1, 2, 0), 3));
    }

    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "hold_reroll", "HoldRerollConfig", """
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
                """, "HoldRerollStateData", """
                ---
                currentResults:
                - 1
                - 2
                - 10
                rerollCounter: 2
                """);


        ConfigAndState<HoldRerollConfig, HoldRerollStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "reroll");
        assertThat(configAndState.getConfig()).isEqualTo(new HoldRerollConfig(123L, 10, ImmutableSet.of(9, 10), ImmutableSet.of(7, 8, 9, 10), ImmutableSet.of(1), AnswerFormatType.full));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new HoldRerollStateData(ImmutableList.of(1, 2, 1), 3));
    }
    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "hold_reroll", "HoldRerollConfig", """
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
                """, "HoldRerollStateData", """
                ---
                currentResults:
                - 1
                - 2
                - 10
                rerollCounter: 2
                """);


        ConfigAndState<HoldRerollConfig, HoldRerollStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "reroll");
        assertThat(configAndState.getConfig()).isEqualTo(new HoldRerollConfig(123L, 10, ImmutableSet.of(9, 10), ImmutableSet.of(7, 8, 9, 10), ImmutableSet.of(1), AnswerFormatType.compact));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new HoldRerollStateData(ImmutableList.of(1, 2, 1), 3));
    }

}