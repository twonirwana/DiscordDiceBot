package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.api.Answer;
import de.janno.discord.api.IButtonEventAdaptor;
import de.janno.discord.dice.DiceUtils;
import discord4j.core.object.component.LayoutComponent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HoldRerollCommandTest {

    HoldRerollCommand underTest;

    private static Stream<Arguments> generateValidateData() {
        return Stream.of(
                Arguments.of(new HoldRerollCommand.Config(6, ImmutableSet.of(7), ImmutableSet.of(), ImmutableSet.of()), "reroll set [7] contains a number bigger then the sides of the die 6"),
                Arguments.of(new HoldRerollCommand.Config(6, ImmutableSet.of(), ImmutableSet.of(7), ImmutableSet.of()), "success set [7] contains a number bigger then the sides of the die 6"),
                Arguments.of(new HoldRerollCommand.Config(6, ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of(7)), "failure set [7] contains a number bigger then the sides of the die 6"),
                Arguments.of(new HoldRerollCommand.Config(6, ImmutableSet.of(1, 2, 3, 4, 5, 6), ImmutableSet.of(), ImmutableSet.of()), "The reroll must not contain all numbers"),
                Arguments.of(new HoldRerollCommand.Config(6, ImmutableSet.of(), ImmutableSet.of(0), ImmutableSet.of(0)), null),
                Arguments.of(new HoldRerollCommand.Config(6, ImmutableSet.of(2, 3, 4), ImmutableSet.of(5, 6), ImmutableSet.of(1)), null)
        );
    }

    @BeforeEach
    void setup() {
        underTest = new HoldRerollCommand(new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
    }

    @Test
    void getName() {
        String res = underTest.getName();
        assertThat(res).isEqualTo("hold_reroll");
    }

    @Test
    void getStartOptions() {
        List<ApplicationCommandOptionData> res = underTest.getStartOptions();

        assertThat(res.stream().map(ApplicationCommandOptionData::name)).containsExactly("sides", "reroll_set", "success_set", "failure_set");
    }

    @Test
    void getDiceResult_withoutReroll() {
        Answer res = underTest.getAnswer(new HoldRerollCommand.State("finish", ImmutableList.of(1, 2, 3, 4, 5, 6), 0),
                new HoldRerollCommand.Config(
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1)));
        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("Success: 2 and Failure: 1");
        assertThat(res.getContent()).isEqualTo("[**1**,2,3,4,**5**,**6**]");
    }

    @Test
    void getDiceResult_withReroll() {
        Answer res = underTest.getAnswer(new HoldRerollCommand.State("finish", ImmutableList.of(1, 2, 3, 4, 5, 6), 2),
                new HoldRerollCommand.Config(
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1)));
        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("Success: 2, Failure: 1 and Rerolls: 2");
        assertThat(res.getContent()).isEqualTo("[**1**,2,3,4,**5**,**6**]");
    }

    @Test
    void getConfigFromEvent_roll3d6() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,3,EMPTY,6,2;3;4,5;6,1,0");

        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));
    }

    @Test
    void getStateFromEvent_roll3d6() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,3,EMPTY,6,2;3;4,5;6,1,0");

        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new HoldRerollCommand.State("3", ImmutableList.of(1, 1, 1), 0));
    }

    @Test
    void getConfigFromEvent_finish() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,finish,1;2;3;4;5;6,6,2;3;4,5;6,1,0");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));
    }

    @Test
    void getStateFromEvent_finish() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,finish,1;2;3;4;5;6,6,2;3;4,5;6,1,0");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new HoldRerollCommand.State("finish", ImmutableList.of(1, 2, 3, 4, 5, 6), 0));
    }

    @Test
    void getConfigFromEvent_clear() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,clear,1;2;3;4;5;6,6,2;3;4,5;6,1,0");
        assertThat(underTest.getConfigFromEvent(event))
                .isEqualTo(new HoldRerollCommand.Config(
                        6,
                        ImmutableSet.of(2, 3, 4),
                        ImmutableSet.of(5, 6),
                        ImmutableSet.of(1)));
    }

    @Test
    void getStateFromEvent_clear() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,clear,1;2;3;4;5;6,6,2;3;4,5;6,1,0");
        assertThat(underTest.getStateFromEvent(event))
                .isEqualTo(new HoldRerollCommand.State("clear", ImmutableList.of(), 0));
    }

    @Test
    void getConfigFromEvent_reroll() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,reroll,1;2;3;4;5;6,6,2;3;4,5;6,1,1");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));
    }

    @Test
    void getStateFromEvent_reroll() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,reroll,1;2;3;4;5;6,6,2;3;4,5;6,1,1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new HoldRerollCommand.State("reroll", ImmutableList.of(1, 1, 1, 1, 5, 6), 2));
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("hold_reroll,1;2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("hold_rerol")).isFalse();
    }

    @Test
    void getButtonMessageWithState_clear() {
        String res = underTest.getButtonMessageWithState(new HoldRerollCommand.State("clear", ImmutableList.of(1, 2, 3, 4, 5, 6), 2), new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @Test
    void getButtonMessageWithState_finish() {
        String res = underTest.getButtonMessageWithState(new HoldRerollCommand.State("finish", ImmutableList.of(1, 2, 3, 4, 5, 6), 2), new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @Test
    void getButtonMessageWithState_noRerollPossible() {
        String res = underTest.getButtonMessageWithState(new HoldRerollCommand.State("reroll", ImmutableList.of(1, 1, 1, 5, 5, 6), 2), new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @Test
    void getButtonMessageWithState_rerollPossible() {
        String res = underTest.getButtonMessageWithState(new HoldRerollCommand.State("reroll", ImmutableList.of(1, 2, 3, 4, 5, 6), 2), new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));

        assertThat(res).isEqualTo("[**1**,2,3,4,**5**,**6**] = 2 successes and 1 failures");
    }

    @Test
    void getButtonMessage() {
        String res = underTest.getButtonMessage(new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("generateValidateData")
    void validate(HoldRerollCommand.Config config, String expected) {
        assertThat(underTest.validate(config)).isEqualTo(expected);
    }

    @Test
    void getStateFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,finish,1;2;3;4;5;6,6,2;3;4,5;6,1,0");

        HoldRerollCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new HoldRerollCommand.State("finish", ImmutableList.of(1, 2, 3, 4, 5, 6), 0));
    }

    @Test
    void createButtonCustomId() {
        String res = underTest.createButtonCustomId("finish", new HoldRerollCommand.Config(6, ImmutableSet.of(2, 3, 4), ImmutableSet.of(5, 6), ImmutableSet.of(1)),
                new HoldRerollCommand.State("finish", ImmutableList.of(1, 1, 1, 1, 5, 6), 3));

        assertThat(res).isEqualTo("hold_reroll,finish,1;1;1;1;5;6,6,2;3;4,5;6,1,3");
    }

    @Test
    void getButtonLayoutWithState_reroll() {
        List<LayoutComponent> res = underTest.getButtonLayoutWithState(new HoldRerollCommand.State("reroll", ImmutableList.of(1, 2, 3, 4, 5, 6), 2), new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get())).containsExactly("Reroll", "Finish", "Clear");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
                .containsExactly("hold_reroll,reroll,1;2;3;4;5;6,6,2;3;4,5;6,1,2",
                        "hold_reroll,finish,1;2;3;4;5;6,6,2;3;4,5;6,1,2",
                        "hold_reroll,clear,1;2;3;4;5;6,6,2;3;4,5;6,1,2");
    }

    @Test
    void getButtonLayoutWithState_finish() {
        List<LayoutComponent> res = underTest.getButtonLayoutWithState(new HoldRerollCommand.State("finish", ImmutableList.of(1, 2, 3, 4, 5, 6), 2), new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get()))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
                .containsExactly("hold_reroll,1,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,2,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,3,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,4,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,5,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,6,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,7,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,8,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,9,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,10,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,11,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,12,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,13,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,14,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,15,EMPTY,6,2;3;4,5;6,1,0");
    }

    @Test
    void getButtonLayoutWithState_clear() {
        List<LayoutComponent> res = underTest.getButtonLayoutWithState(new HoldRerollCommand.State("clear", ImmutableList.of(1, 2, 3, 4, 5, 6), 2), new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get()))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
                .containsExactly("hold_reroll,1,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,2,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,3,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,4,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,5,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,6,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,7,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,8,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,9,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,10,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,11,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,12,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,13,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,14,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,15,EMPTY,6,2;3;4,5;6,1,0");
    }

    @Test
    void getButtonLayoutWithState_3() {
        List<LayoutComponent> res = underTest.getButtonLayoutWithState(new HoldRerollCommand.State("3", ImmutableList.of(1, 2, 3, 4, 5, 6), 2), new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get())).containsExactly("Reroll", "Finish", "Clear");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
                .containsExactly("hold_reroll,reroll,1;2;3;4;5;6,6,2;3;4,5;6,1,2",
                        "hold_reroll,finish,1;2;3;4;5;6,6,2;3;4,5;6,1,2",
                        "hold_reroll,clear,1;2;3;4;5;6,6,2;3;4,5;6,1,2");
    }

    @Test
    void getButtonLayoutWithState_3_finished() {
        List<LayoutComponent> res = underTest.getButtonLayoutWithState(new HoldRerollCommand.State("3", ImmutableList.of(1, 1, 1, 5, 5, 6), 2), new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get()))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
                .containsExactly("hold_reroll,1,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,2,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,3,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,4,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,5,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,6,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,7,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,8,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,9,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,10,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,11,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,12,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,13,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,14,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,15,EMPTY,6,2;3;4,5;6,1,0");
    }

    @Test
    void getButtonLayout() {
        List<LayoutComponent> res = underTest.getButtonLayout(new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)));

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get()))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
                .containsExactly("hold_reroll,1,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,2,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,3,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,4,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,5,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,6,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,7,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,8,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,9,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,10,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,11,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,12,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,13,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,14,EMPTY,6,2;3;4,5;6,1,0",
                        "hold_reroll,15,EMPTY,6,2;3;4,5;6,1,0");
    }

    @Test
    void editButtonMessage() {
        assertThat(underTest.getEditButtonMessage(new HoldRerollCommand.State("3", ImmutableList.of(1, 1, 1, 5, 5, 6), 2), new HoldRerollCommand.Config(
                6,
                ImmutableSet.of(2, 3, 4),
                ImmutableSet.of(5, 6),
                ImmutableSet.of(1)))).isNull();
    }
}