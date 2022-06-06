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
                .containsExactly("custom_dice\u00001d4\u0000",
                        "custom_dice\u00001d6\u0000",
                        "custom_dice\u00001d8\u0000",
                        "custom_dice\u00001d10\u0000",
                        "custom_dice\u00001d12\u0000",
                        "custom_dice\u00001d20\u0000",
                        "custom_dice\u00001d100\u0000",
                        "custom_dice\u00002d20k1\u0000",
                        "custom_dice\u00002d20L1\u0000",
                        "custom_dice\u00002d4\u0000",
                        "custom_dice\u00002d6\u0000",
                        "custom_dice\u00002d8\u0000",
                        "custom_dice\u00002d10\u0000",
                        "custom_dice\u00002d12\u0000",
                        "custom_dice\u00002d20\u0000");
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
                .containsExactly("count_successes\u00001\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u00002\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u00003\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u00004\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u00005\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u00006\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u00007\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u00008\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u00009\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u000010\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u000011\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u000012\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u000013\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u000014\u000010\u00008\u0000no_glitch\u000015\u0000",
                        "count_successes\u000015\u000010\u00008\u0000no_glitch\u000015\u0000");
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
                .containsExactly("pool_target\u00001\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00002\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00003\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00004\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00005\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00006\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00007\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00008\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00009\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000010\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000011\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000012\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000013\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000014\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000015\u000010\u000015\u000010\u00001\u0000ask\u0000EMPTY\u0000EMPTY\u0000");
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
                .containsExactly("count_successes\u00001\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u00002\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u00003\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u00004\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u00005\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u00006\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u00007\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u00008\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u00009\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u000010\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u000011\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u000012\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u000013\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u000014\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u000015\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u000016\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u000017\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u000018\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u000019\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000",
                        "count_successes\u000020\u00006\u00005\u0000glitch:half_dice_one\u000020\u0000");
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
                .isEqualTo("""
                        Welcome to the Button Dice Bot,
                        use one of the example buttons below to start one of the RPG dice systems or use the slash command to configure your own custom dice system (see https://github.com/twonirwana/DiscordDiceBot for details or the slash command `/help`).\s
                        You can also use the slash command `/r` to directly roll dice with.
                        For help or feature request come to the support discord server: https://discord.gg/e43BsqKpFr""");
        assertThat(res.getComponentRowDefinitions()
                .stream()
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("welcome\u0000fate",
                        "welcome\u0000dnd5",
                        "welcome\u0000nWoD",
                        "welcome\u0000oWoD",
                        "welcome\u0000shadowrun",
                        "welcome\u0000coin");
        assertThat(res.getComponentRowDefinitions()
                .stream()
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("Fate", "D&D5e", "nWoD", "oWoD", "Shadowrun","Coin Toss");

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