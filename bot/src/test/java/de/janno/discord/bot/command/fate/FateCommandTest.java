package de.janno.discord.bot.command.fate;

import de.janno.discord.bot.command.*;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FateCommandTest {

    FateCommand underTest;


    @BeforeEach
    void setup() {
        underTest = new FateCommand(mock(MessageDataDAO.class), new DiceUtils(1, 2, 3, 1, 2, 3, 1, 2));
    }

    @Test
    void getName() {
        assertThat(underTest.getCommandId()).isEqualTo("fate");
    }

    @Test
    void getButtonMessage_modifier() {
        String res = underTest.createNewButtonMessage(new FateConfig(null, "with_modifier", AnswerFormatType.full))
                .getContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice and add the value of the button");
    }

    @Test
    void getButtonMessage_simple() {
        String res = underTest.createNewButtonMessage(new FateConfig(null, "simple", AnswerFormatType.full))
                .getContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice");
    }

    @Test
    void getButtonMessageWithState_modifier() {
        String res = underTest.createNewButtonMessageWithState(new FateConfig(null, "with_modifier", AnswerFormatType.full), new State<>("0", StateData.empty()))
                .orElseThrow().getContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice and add the value of the button");
    }

    @Test
    void getButtonMessageWithState_simple() {
        String res = underTest.createNewButtonMessageWithState(new FateConfig(null, "simple", AnswerFormatType.full), new State<>("0", StateData.empty()))
                .orElseThrow().getContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice");
    }

    @Test
    void matchingComponentCustomId_match_legacy() {
        assertThat(underTest.matchingComponentCustomId("fate\u00001;2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch_legacy() {
        assertThat(underTest.matchingComponentCustomId("fate")).isFalse();
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("fate2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("fate22")).isFalse();
    }

    @Test
    void getDiceResult_simple() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new FateConfig(null, "simple", AnswerFormatType.full), new State<>("roll", StateData.empty()))
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF ⇒ -1");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_minus1() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new FateConfig(null, "with_modifier", AnswerFormatType.full), new State<>("-1", StateData.empty()))
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF -1 ⇒ -2");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_plus1() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new FateConfig(null, "with_modifier", AnswerFormatType.full), new State<>("1", StateData.empty()))
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF +1 ⇒ 0");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_0() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new FateConfig(null, "with_modifier", AnswerFormatType.full), new State<>("0", StateData.empty()))
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF ⇒ -1");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("type");
    }

    @Test
    void getStateFromEvent_simple() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate\u0000roll,simple");

        State<StateData> res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new State<>("roll", StateData.empty()));
    }

    @Test
    void getStateFromEvent_modifier() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate\u00005\u0000with_modifier\u0000");

        State<StateData> res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new State<>("5", StateData.empty()));
    }

    @Test
    void getStateFromEvent_modifierNegativ() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate\u0000-3\u0000with_modifier\u0000");

        State<StateData> res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new State<>("-3", StateData.empty()));
    }

    @Test
    void getButtonLayoutWithState_simple() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new FateConfig(null, "simple", AnswerFormatType.full), new State<>("roll", StateData.empty()))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("Roll 4dF");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate\u001Eroll");
    }

    @Test
    void getButtonLayout_simple() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(new FateConfig(null, "simple", AnswerFormatType.full))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("Roll 4dF");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate\u001Eroll");
    }

    @Test
    void getButtonLayoutWithState_modifier() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new FateConfig(null, "with_modifier", AnswerFormatType.full), new State<>("2", StateData.empty()))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate-4",
                        "fate-3",
                        "fate-2",
                        "fate-1",
                        "fate0",
                        "fate1",
                        "fate2",
                        "fate3",
                        "fate4",
                        "fate5",
                        "fate6",
                        "fate7",
                        "fate8",
                        "fate9",
                        "fate10");
    }

    @Test
    void getButtonLayout_modifier() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(new FateConfig(null, "with_modifier", AnswerFormatType.full)).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate-4",
                        "fate-3",
                        "fate-2",
                        "fate-1",
                        "fate0",
                        "fate1",
                        "fate2",
                        "fate3",
                        "fate4",
                        "fate5",
                        "fate6",
                        "fate7",
                        "fate8",
                        "fate9",
                        "fate10");
    }

    @Test
    void getCurrentMessageContentChange() {
        assertThat(underTest.getCurrentMessageContentChange(new FateConfig(null, "with_modifier", AnswerFormatType.full), new State<>("2", StateData.empty()))).isEmpty();
    }

    @Test
    void getConfigFromEvent() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate\u0000roll\u0000simple\u0000");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new FateConfig(null, "simple", AnswerFormatType.full));
    }

    @Test
    void getConfigFromEvent_target() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate\u0000roll\u0000simple\u0000123");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new FateConfig(123L, "simple", AnswerFormatType.full));
    }

    @Test
    void getConfigFromEvent_legacy() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate\u0000roll\u0000simple");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new FateConfig(null, "simple", AnswerFormatType.full));
    }

    @Test
    void checkPersistence() {
        MessageDataDAO messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        UUID configUUID = UUID.randomUUID();
        FateConfig config = new FateConfig(123L, "with_modifier", AnswerFormatType.full);
        State<StateData> state = new State<>("5", StateData.empty());
        Optional<MessageDataDTO> toSave = underTest.createMessageDataForNewMessage(configUUID, 1L, channelId, messageId, config, state);
        messageDataDAO.saveMessageData(toSave.orElseThrow());

        MessageDataDTO loaded = messageDataDAO.getDataForMessage(channelId, messageId).orElseThrow();

        assertThat(toSave.orElseThrow()).isEqualTo(loaded);
        ConfigAndState<FateConfig, StateData> configAndState = underTest.deserializeAndUpdateState(loaded, "3");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(state.getData());
    }

    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "fate", "FateConfig", """
                ---
                answerTargetChannelId: 123
                type: "with_modifier"
                """, "None", null);


        ConfigAndState<FateConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new FateConfig(123L, "with_modifier", AnswerFormatType.full));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "fate", "FateConfig", """
                ---
                answerTargetChannelId: 123
                type: "with_modifier"
                answerFormatType: compact
                """, "None", null);


        ConfigAndState<FateConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new FateConfig(123L, "with_modifier", AnswerFormatType.compact));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

}