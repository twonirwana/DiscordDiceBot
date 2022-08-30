package de.janno.discord.bot.command;

import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WelcomeCommandTest {

    final WelcomeCommand underTest = new WelcomeCommand(mock(MessageDataDAO.class));

    @Test
    public void getButtonMessageWithState_fate() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(null, new State<>("fate", new EmptyData()));
        assertThat(res.map(MessageDefinition::getContent))
                .contains("Click a button to roll four fate dice and add the value of the button");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
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
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10");

    }

    @Test
    public void getButtonMessageWithState_dnd5() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(null, new State<>("dnd5", new EmptyData()));
        assertThat(res.map(MessageDefinition::getContent))
                .contains("Click on a button to roll the dice");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("custom_dice1_button",
                        "custom_dice2_button",
                        "custom_dice3_button",
                        "custom_dice4_button",
                        "custom_dice5_button",
                        "custom_dice6_button",
                        "custom_dice7_button",
                        "custom_dice8_button",
                        "custom_dice9_button",
                        "custom_dice10_button",
                        "custom_dice11_button",
                        "custom_dice12_button",
                        "custom_dice13_button",
                        "custom_dice14_button",
                        "custom_dice15_button");
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
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(null, new State<>("nWoD", new EmptyData()));

        assertThat(res.map(MessageDefinition::getContent))
                .contains("Click to roll the dice against 8");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("count_successes1",
                        "count_successes2",
                        "count_successes3",
                        "count_successes4",
                        "count_successes5",
                        "count_successes6",
                        "count_successes7",
                        "count_successes8",
                        "count_successes9",
                        "count_successes10",
                        "count_successes11",
                        "count_successes12",
                        "count_successes13",
                        "count_successes14",
                        "count_successes15");
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
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(null, new State<>("oWoD", new EmptyData()));
        assertThat(res.map(MessageDefinition::getContent))
                .contains("Click on the buttons to roll dice, with ask reroll:10 and botch:1");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("pool_target1",
                        "pool_target2",
                        "pool_target3",
                        "pool_target4",
                        "pool_target5",
                        "pool_target6",
                        "pool_target7",
                        "pool_target8",
                        "pool_target9",
                        "pool_target10",
                        "pool_target11",
                        "pool_target12",
                        "pool_target13",
                        "pool_target14",
                        "pool_target15");
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
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(null, new State<>("shadowrun", new EmptyData()));
        assertThat(res.map(MessageDefinition::getContent))
                .contains("Click to roll the dice against 5");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("count_successes1",
                        "count_successes2",
                        "count_successes3",
                        "count_successes4",
                        "count_successes5",
                        "count_successes6",
                        "count_successes7",
                        "count_successes8",
                        "count_successes9",
                        "count_successes10",
                        "count_successes11",
                        "count_successes12",
                        "count_successes13",
                        "count_successes14",
                        "count_successes15",
                        "count_successes16",
                        "count_successes17",
                        "count_successes18",
                        "count_successes19",
                        "count_successes20");
        assertThat(res.map(MessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6", "16d6", "17d6", "18d6", "19d6", "20d6");

    }

    @Test
    public void getButtonMessageWithState_other() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(null, new State<>("-", new EmptyData()));
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
                .containsExactly("welcomefate",
                        "welcomednd5",
                        "welcomenWoD",
                        "welcomeoWoD",
                        "welcomeshadowrun",
                        "welcomecoin");
        assertThat(res.getComponentRowDefinitions()
                .stream()
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("Fate", "D&D5e", "nWoD", "oWoD", "Shadowrun", "Coin Toss 🪙");

    }

    @Test
    public void shouldKeepExistingButtonMessage() {
        assertThat(underTest.shouldKeepExistingButtonMessage(mock(ButtonEventAdaptor.class))).isTrue();
    }

    @Test
    public void getAnswer() {
        assertThat(underTest.getAnswer(null, null)).isEmpty();
    }

    @Test
    public void getStateFromEvent() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("welcome,fate");

        State<EmptyData> res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new State<>("fate", new EmptyData()));
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
        assertThat(underTest.getCommandId()).isEqualTo("welcome");
    }
}