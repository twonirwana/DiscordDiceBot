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
        String res = underTest.getButtonMessage(new FateCommand.Config("with_modifier"))
                .getContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice and add the value of the button");
    }

    @Test
    void getButtonMessage_simple() {
        String res = underTest.getButtonMessage(new FateCommand.Config("simple"))
                .getContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice");
    }

    @Test
    void getButtonMessageWithState_modifier() {
        String res = underTest.getButtonMessageWithState(new FateCommand.State(0), new FateCommand.Config("with_modifier"))
                .get().getContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice and add the value of the button");
    }

    @Test
    void getButtonMessageWithState_simple() {
        String res = underTest.getButtonMessageWithState(new FateCommand.State(0), new FateCommand.Config("simple"))
                .get().getContent();

        assertThat(res).isEqualTo("Click a button to roll four fate dice");
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("fate,1;2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("fate")).isFalse();
    }

    @Test
    void getDiceResult_simple() {
        EmbedDefinition res = underTest.getAnswer(new FateCommand.State(null), new FateCommand.Config("simple")).get();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF = -1");
        assertThat(res.getDescription()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_minus1() {
        EmbedDefinition res = underTest.getAnswer(new FateCommand.State(-1), new FateCommand.Config("with_modifier")).get();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF -1 = -2");
        assertThat(res.getDescription()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_plus1() {
        EmbedDefinition res = underTest.getAnswer(new FateCommand.State(1), new FateCommand.Config("with_modifier")).get();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF +1 = 0");
        assertThat(res.getDescription()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_0() {
        EmbedDefinition res = underTest.getAnswer(new FateCommand.State(0), new FateCommand.Config("with_modifier")).get();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("4dF = -1");
        assertThat(res.getDescription()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("type");
    }

    @Test
    void getStateFromEvent_simple() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate,roll,simple");

        FateCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new FateCommand.State(null));
    }

    @Test
    void getStateFromEvent_modifier() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate,5,with_modifier");

        FateCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new FateCommand.State(5));
    }

    @Test
    void getStateFromEvent_modifierNegativ() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("fate,-3,with_modifier");

        FateCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new FateCommand.State(-3));
    }

    @Test
    void createButtonCustomId() {
        String res = underTest.createButtonCustomId("3", new FateCommand.Config("with_modifier"));

        assertThat(res).isEqualTo("fate,3,with_modifier");
    }

    @Test
    void getButtonLayoutWithState_simple() {
        List<ComponentRowDefinition> res = underTest.getButtonMessageWithState(new FateCommand.State(null), new FateCommand.Config("simple"))
                .get().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("Roll 4dF");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate,roll,simple");
    }

    @Test
    void getButtonLayout_simple() {
        List<ComponentRowDefinition> res = underTest.getButtonMessage(new FateCommand.Config("simple"))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("Roll 4dF");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate,roll,simple");
    }

    @Test
    void getButtonLayoutWithState_modifier() {
        List<ComponentRowDefinition> res = underTest.getButtonMessageWithState(new FateCommand.State(2), new FateCommand.Config("with_modifier"))
                .get().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate,-4,with_modifier",
                        "fate,-3,with_modifier",
                        "fate,-2,with_modifier",
                        "fate,-1,with_modifier",
                        "fate,0,with_modifier",
                        "fate,1,with_modifier",
                        "fate,2,with_modifier",
                        "fate,3,with_modifier",
                        "fate,4,with_modifier",
                        "fate,5,with_modifier",
                        "fate,6,with_modifier",
                        "fate,7,with_modifier",
                        "fate,8,with_modifier",
                        "fate,9,with_modifier",
                        "fate,10,with_modifier");
    }

    @Test
    void getButtonLayout_modifier() {
        List<ComponentRowDefinition> res = underTest.getButtonMessage(new FateCommand.Config("with_modifier")).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("fate,-4,with_modifier",
                        "fate,-3,with_modifier",
                        "fate,-2,with_modifier",
                        "fate,-1,with_modifier",
                        "fate,0,with_modifier",
                        "fate,1,with_modifier",
                        "fate,2,with_modifier",
                        "fate,3,with_modifier",
                        "fate,4,with_modifier",
                        "fate,5,with_modifier",
                        "fate,6,with_modifier",
                        "fate,7,with_modifier",
                        "fate,8,with_modifier",
                        "fate,9,with_modifier",
                        "fate,10,with_modifier");
    }

    @Test
    void editButtonMessage() {
        assertThat(underTest.getEditButtonMessage(new FateCommand.State(2), new FateCommand.Config("with_modifier"))).isEmpty();
    }
}