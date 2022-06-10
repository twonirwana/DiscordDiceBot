package de.janno.discord.bot.command;

import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FateCommandTest {

    FateCommand underTest;

    @BeforeEach
    void setup() {
        underTest = new FateCommand(new DiceUtils(1, 2, 3, 1, 2, 3, 1, 2));
    }

    @Test
    void getName() {
        assertThat(underTest.getName()).isEqualTo("fate");
    }

    @Test
    void getButtonMessage_modifier() {
        String res = underTest.createNewButtonMessage(new FateCommand.Config("with_modifier", null))
                .getContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice and add the value of the button");
    }

    @Test
    void getButtonMessage_simple() {
        String res = underTest.createNewButtonMessage(new FateCommand.Config("simple", null))
                .getContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice");
    }

    @Test
    void getButtonMessageWithState_modifier() {
        String res = underTest.createNewButtonMessageWithState(new FateCommand.State(0), new FateCommand.Config("with_modifier", null))
                .orElseThrow().getContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice and add the value of the button");
    }

    @Test
    void getButtonMessageWithState_simple() {
        String res = underTest.createNewButtonMessageWithState(new FateCommand.State(0), new FateCommand.Config("simple", null))
                .orElseThrow().getContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice");
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("fate\u00001;2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("fate")).isFalse();
    }

    @Test
    void getDiceResult_simple() {
        EmbedDefinition res = underTest.getAnswer(new FateCommand.State(null), new FateCommand.Config("simple", null)).orElseThrow();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF = -1");
        assertThat(res.getDescription()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_minus1() {
        EmbedDefinition res = underTest.getAnswer(new FateCommand.State(-1), new FateCommand.Config("with_modifier", null)).orElseThrow();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF -1 = -2");
        assertThat(res.getDescription()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_plus1() {
        EmbedDefinition res = underTest.getAnswer(new FateCommand.State(1), new FateCommand.Config("with_modifier", null)).orElseThrow();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF +1 = 0");
        assertThat(res.getDescription()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_0() {
        EmbedDefinition res = underTest.getAnswer(new FateCommand.State(0), new FateCommand.Config("with_modifier", null)).orElseThrow();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF = -1");
        assertThat(res.getDescription()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("type", "target_channel");
    }

    @Test
    void getStateFromEvent_simple() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate\u0000roll,simple");

        FateCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new FateCommand.State(null));
    }

    @Test
    void getStateFromEvent_modifier() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate\u00005\u0000with_modifier\u0000");

        FateCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new FateCommand.State(5));
    }

    @Test
    void getStateFromEvent_modifierNegativ() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate\u0000-3\u0000with_modifier\u0000");

        FateCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new FateCommand.State(-3));
    }

    @Test
    void createButtonCustomId() {
        String res = underTest.createButtonCustomId("3", new FateCommand.Config("with_modifier", null));

        assertThat(res).isEqualTo("fate\u00003\u0000with_modifier\u0000");
    }

    @Test
    void getButtonLayoutWithState_simple() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new FateCommand.State(null), new FateCommand.Config("simple", null))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("Roll 4dF");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate\u0000roll\u0000simple\u0000");
    }

    @Test
    void getButtonLayout_simple() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(new FateCommand.Config("simple", null))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("Roll 4dF");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate\u0000roll\u0000simple\u0000");
    }

    @Test
    void getButtonLayoutWithState_modifier() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new FateCommand.State(2), new FateCommand.Config("with_modifier", null))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate\u0000-4\u0000with_modifier\u0000",
                        "fate\u0000-3\u0000with_modifier\u0000",
                        "fate\u0000-2\u0000with_modifier\u0000",
                        "fate\u0000-1\u0000with_modifier\u0000",
                        "fate\u00000\u0000with_modifier\u0000",
                        "fate\u00001\u0000with_modifier\u0000",
                        "fate\u00002\u0000with_modifier\u0000",
                        "fate\u00003\u0000with_modifier\u0000",
                        "fate\u00004\u0000with_modifier\u0000",
                        "fate\u00005\u0000with_modifier\u0000",
                        "fate\u00006\u0000with_modifier\u0000",
                        "fate\u00007\u0000with_modifier\u0000",
                        "fate\u00008\u0000with_modifier\u0000",
                        "fate\u00009\u0000with_modifier\u0000",
                        "fate\u000010\u0000with_modifier\u0000");
    }

    @Test
    void getButtonLayout_modifier() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(new FateCommand.Config("with_modifier", null)).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate\u0000-4\u0000with_modifier\u0000",
                        "fate\u0000-3\u0000with_modifier\u0000",
                        "fate\u0000-2\u0000with_modifier\u0000",
                        "fate\u0000-1\u0000with_modifier\u0000",
                        "fate\u00000\u0000with_modifier\u0000",
                        "fate\u00001\u0000with_modifier\u0000",
                        "fate\u00002\u0000with_modifier\u0000",
                        "fate\u00003\u0000with_modifier\u0000",
                        "fate\u00004\u0000with_modifier\u0000",
                        "fate\u00005\u0000with_modifier\u0000",
                        "fate\u00006\u0000with_modifier\u0000",
                        "fate\u00007\u0000with_modifier\u0000",
                        "fate\u00008\u0000with_modifier\u0000",
                        "fate\u00009\u0000with_modifier\u0000",
                        "fate\u000010\u0000with_modifier\u0000");
    }

    @Test
    void getCurrentMessageContentChange() {
        assertThat(underTest.getCurrentMessageContentChange(new FateCommand.State(2), new FateCommand.Config("with_modifier", null))).isEmpty();
    }

    @Test
    void getConfigFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate\u0000roll\u0000simple\u0000");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new FateCommand.Config("simple", null));
    }

    @Test
    void getConfigFromEvent_target() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate\u0000roll\u0000simple\u0000123");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new FateCommand.Config("simple", 123L));
    }

    @Test
    void getConfigFromEvent_legacy() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate\u0000roll\u0000simple");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new FateCommand.Config("simple", null));
    }
}