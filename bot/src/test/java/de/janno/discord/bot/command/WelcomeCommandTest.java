package de.janno.discord.bot.command;

import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WelcomeCommandTest {

    WelcomeCommand underTest = new WelcomeCommand();

    @Test
    public void getButtonMessageWithState_fate() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new WelcomeCommand.State("fate"), null);
        assertThat(res.map(MessageDefinition::getContent))
                .contains("Click a button to roll four fate dice and add the value of the button");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
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
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10");

    }

    @Test
    public void getButtonMessageWithState_dnd5() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new WelcomeCommand.State("dnd5"), null);
        assertThat(res.map(MessageDefinition::getContent))
                .contains("Click on a button to roll the dice");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("custom_dice,1d4",
                        "custom_dice,1d6",
                        "custom_dice,1d8",
                        "custom_dice,1d10",
                        "custom_dice,1d12",
                        "custom_dice,1d20",
                        "custom_dice,1d100",
                        "custom_dice,2d20k1",
                        "custom_dice,2d20L1",
                        "custom_dice,2d4",
                        "custom_dice,2d6",
                        "custom_dice,2d8",
                        "custom_dice,2d10",
                        "custom_dice,2d12",
                        "custom_dice,2d20");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("D4",
                        "D6",
                        "D8",
                        "D10",
                        "D12",
                        "D20",
                        "D100",
                        "D20 Advantage",
                        "D20 Disadvantage",
                        "2D4",
                        "2D6",
                        "2D8",
                        "2D10",
                        "2D12",
                        "2D20");
    }

    @Test
    public void getButtonMessageWithState_nWoD() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new WelcomeCommand.State("nWoD"), null);

        assertThat(res.map(MessageDefinition::getContent))
                .contains("Click to roll the dice against 8");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("count_successes,1,10,8,no_glitch,15",
                        "count_successes,2,10,8,no_glitch,15",
                        "count_successes,3,10,8,no_glitch,15",
                        "count_successes,4,10,8,no_glitch,15",
                        "count_successes,5,10,8,no_glitch,15",
                        "count_successes,6,10,8,no_glitch,15",
                        "count_successes,7,10,8,no_glitch,15",
                        "count_successes,8,10,8,no_glitch,15",
                        "count_successes,9,10,8,no_glitch,15",
                        "count_successes,10,10,8,no_glitch,15",
                        "count_successes,11,10,8,no_glitch,15",
                        "count_successes,12,10,8,no_glitch,15",
                        "count_successes,13,10,8,no_glitch,15",
                        "count_successes,14,10,8,no_glitch,15",
                        "count_successes,15,10,8,no_glitch,15");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10",
                        "11d10", "12d10", "13d10", "14d10", "15d10");
    }

    @Test
    public void getButtonMessageWithState_oWoD() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new WelcomeCommand.State("oWoD"), null);
        assertThat(res.map(MessageDefinition::getContent))
                .contains("Click on the buttons to roll dice, with ask reroll:10 and botch:1");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("pool_target,1,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,2,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,3,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,4,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,5,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,6,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,7,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,8,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,9,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,10,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,11,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,12,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,13,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,14,10,15,10,1,ask,EMPTY,EMPTY",
                        "pool_target,15,10,15,10,1,ask,EMPTY,EMPTY");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10",
                        "11d10", "12d10", "13d10", "14d10", "15d10");

    }

    @Test
    public void getButtonMessageWithState_Shadowrun() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new WelcomeCommand.State("shadowrun"), null);
        assertThat(res.map(MessageDefinition::getContent))
                .contains("Click to roll the dice against 5");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("count_successes,1,6,5,glitch:half_dice_one,20",
                        "count_successes,2,6,5,glitch:half_dice_one,20",
                        "count_successes,3,6,5,glitch:half_dice_one,20",
                        "count_successes,4,6,5,glitch:half_dice_one,20",
                        "count_successes,5,6,5,glitch:half_dice_one,20",
                        "count_successes,6,6,5,glitch:half_dice_one,20",
                        "count_successes,7,6,5,glitch:half_dice_one,20",
                        "count_successes,8,6,5,glitch:half_dice_one,20",
                        "count_successes,9,6,5,glitch:half_dice_one,20",
                        "count_successes,10,6,5,glitch:half_dice_one,20",
                        "count_successes,11,6,5,glitch:half_dice_one,20",
                        "count_successes,12,6,5,glitch:half_dice_one,20",
                        "count_successes,13,6,5,glitch:half_dice_one,20",
                        "count_successes,14,6,5,glitch:half_dice_one,20",
                        "count_successes,15,6,5,glitch:half_dice_one,20",
                        "count_successes,16,6,5,glitch:half_dice_one,20",
                        "count_successes,17,6,5,glitch:half_dice_one,20",
                        "count_successes,18,6,5,glitch:half_dice_one,20",
                        "count_successes,19,6,5,glitch:half_dice_one,20",
                        "count_successes,20,6,5,glitch:half_dice_one,20");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6", "16d6", "17d6", "18d6", "19d6", "20d6");

    }

    @Test
    public void getButtonMessageWithState_other() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new WelcomeCommand.State("-"), null);
        assertThat(res)
                .isEmpty();

    }

    @Test
    public void getWelcomeMessage() {
        MessageDefinition res = underTest.getWelcomeMessage();
        assertThat(res.getContent())
                .isEqualTo("Welcome to the Button Dice Bot,\n" +
                        "use one of the example buttons below to start one of the RPG dice systems or use the slash command to configure your own custom dice system (see https://github.com/twonirwana/DiscordDiceBot for details or the slash command `/help`). \n" +
                        "You can also use the slash command `/r` to directly roll dice with.\n" +
                        "For help or feature request come to the support discord server: https://discord.gg/e43BsqKpFr");
        assertThat(res.getComponentRowDefinitions()
                .stream()
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("welcome,fate",
                        "welcome,dnd5",
                        "welcome,nWoD",
                        "welcome,oWoD",
                        "welcome,shadowrun");
        assertThat(res.getComponentRowDefinitions()
                .stream()
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("Fate", "D&D5e", "nWoD", "oWoD", "Shadowrun");

    }

    @Test
    public void shouldKeepExistingButtonMessage() {
        assertThat(underTest.shouldKeepExistingButtonMessage(null)).isTrue();
    }

    @Test
    public void getAnswer() {
        assertThat(underTest.getAnswer(null, null)).isEmpty();
    }

    @Test
    public void getStateFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("welcome,fate");

        WelcomeCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new WelcomeCommand.State("fate"));
    }

    @Test
    public void matchingComponentCustomId() {
        boolean res = underTest.matchingComponentCustomId("welcome,fate");

        assertThat(res).isTrue();
    }

    @Test
    public void matchingComponentCustomId_notMatch() {
        boolean res = underTest.matchingComponentCustomId("welcome2,fate");

        assertThat(res).isFalse();
    }

    @Test
    void getName() {
        assertThat(underTest.getName()).isEqualTo("welcome");
    }
}