package de.janno.discord.bot.command.fate;

import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
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

class FateCommandTest {

    FateCommand underTest;


    @BeforeEach
    void setup() {
        underTest = new FateCommand(mock(PersistenceManager.class), new DiceUtils(1, 2, 3, 1, 2, 3, 1, 2));
    }

    @Test
    void getName() {
        assertThat(underTest.getCommandId()).isEqualTo("fate");
    }

    @Test
    void getButtonMessage_modifier() {
        String res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new FateConfig(null, "with_modifier", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")))
                .getDescriptionOrContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice and add the value of the button");
    }

    @Test
    void getButtonMessage_simple() {
        String res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new FateConfig(null, "simple", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")))
                .getDescriptionOrContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice");
    }

    @Test
    void getButtonMessageWithState_modifier() {
        String res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new FateConfig(null, "with_modifier", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("0", StateData.empty()), 1, 2)
                .orElseThrow().getDescriptionOrContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice and add the value of the button");
    }

    @Test
    void getButtonMessageWithState_simple() {
        String res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new FateConfig(null, "simple", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("0", StateData.empty()), 1, 2)
                .orElseThrow().getDescriptionOrContent();

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
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new FateConfig(null, "simple", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("roll", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF ⇒ -1");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_minus1() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new FateConfig(null, "with_modifier", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("-1", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF -1 ⇒ -2");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_plus1() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new FateConfig(null, "with_modifier", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("1", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF +1 ⇒ 0");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_0() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new FateConfig(null, "with_modifier", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("0", StateData.empty()), 0L, 0L)
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
    void getButtonLayoutWithState_simple() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new FateConfig(null, "simple", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("roll", StateData.empty()), 1, 2)
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("Roll 4dF");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate\u001Eroll00000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayout_simple() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new FateConfig(null, "simple", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("Roll 4dF");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate\u001Eroll00000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayoutWithState_modifier() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new FateConfig(null, "with_modifier", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("2", StateData.empty()), 1, 2)
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate-400000000-0000-0000-0000-000000000000",
                        "fate-300000000-0000-0000-0000-000000000000",
                        "fate-200000000-0000-0000-0000-000000000000",
                        "fate-100000000-0000-0000-0000-000000000000",
                        "fate000000000-0000-0000-0000-000000000000",
                        "fate100000000-0000-0000-0000-000000000000",
                        "fate200000000-0000-0000-0000-000000000000",
                        "fate300000000-0000-0000-0000-000000000000",
                        "fate400000000-0000-0000-0000-000000000000",
                        "fate500000000-0000-0000-0000-000000000000",
                        "fate600000000-0000-0000-0000-000000000000",
                        "fate700000000-0000-0000-0000-000000000000",
                        "fate800000000-0000-0000-0000-000000000000",
                        "fate900000000-0000-0000-0000-000000000000",
                        "fate1000000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayout_modifier() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new FateConfig(null, "with_modifier", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"))).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate-400000000-0000-0000-0000-000000000000",
                        "fate-300000000-0000-0000-0000-000000000000",
                        "fate-200000000-0000-0000-0000-000000000000",
                        "fate-100000000-0000-0000-0000-000000000000",
                        "fate000000000-0000-0000-0000-000000000000",
                        "fate100000000-0000-0000-0000-000000000000",
                        "fate200000000-0000-0000-0000-000000000000",
                        "fate300000000-0000-0000-0000-000000000000",
                        "fate400000000-0000-0000-0000-000000000000",
                        "fate500000000-0000-0000-0000-000000000000",
                        "fate600000000-0000-0000-0000-000000000000",
                        "fate700000000-0000-0000-0000-000000000000",
                        "fate800000000-0000-0000-0000-000000000000",
                        "fate900000000-0000-0000-0000-000000000000",
                        "fate1000000000-0000-0000-0000-000000000000");
    }

    @Test
    void getCurrentMessageContentChange() {
        assertThat(underTest.getCurrentMessageContentChange(new FateConfig(null, "with_modifier", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>("2", StateData.empty()))).isEmpty();
    }

    @Test
    void checkPersistence() {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.fromString("00000000-0000-0000-0000-000000000000"), null, null);
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        FateConfig config = new FateConfig(123L, "with_modifier", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, 2L, config);
        assertThat(toSave).isPresent();

        persistenceManager.saveMessageConfig(toSave.get());
        MessageConfigDTO loaded = persistenceManager.getMessageConfig(configUUID).orElseThrow();

        assertThat(toSave.get()).isEqualTo(loaded);
        ConfigAndState<FateConfig, StateData> configAndState = underTest.deserializeAndUpdateState(loaded, "3");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState()).isEqualTo(new State<>("3", StateData.empty()));
    }

    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "fate", "FateConfig", """
                ---
                answerTargetChannelId: 123
                type: "with_modifier"
                """);


        ConfigAndState<FateConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new FateConfig(123L, "with_modifier", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "fate", "FateConfig", """
                ---
                answerTargetChannelId: 123
                type: "with_modifier"
                answerFormatType: compact
                """);


        ConfigAndState<FateConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new FateConfig(123L, "with_modifier", AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none")));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

}