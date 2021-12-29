package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HoldRerollCommandTest {
    HoldRerollCommand underTest = new HoldRerollCommand(new DiceUtils(new ArrayDeque<>(ImmutableList.of(1, 1, 1, 1, 5, 6, 6, 6))));

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
        List<DiceResult> res = underTest.getDiceResult("finish",
                ImmutableList.of("1;2;3;4;5;6",
                        "6",
                        "2;3;4",
                        "5;6",
                        "1",
                        "0"));
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("Success: 2 and Failure: 1");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[**1**,2,3,4,**5**,**6**]");
    }

    @Test
    void getDiceResult_withReroll() {
        List<DiceResult> res = underTest.getDiceResult("finish",
                ImmutableList.of("1;2;3;4;5;6",
                        "6",
                        "2;3;4",
                        "5;6",
                        "1",
                        "2"));
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("Success: 2, Failure: 1 and Rerolls: 2");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[**1**,2,3,4,**5**,**6**]");
    }

    @Test
    void getConfigFromEvent_roll3d6() {
        assertThat(underTest.getConfigFromEvent(TestUtils.createEventWithCustomId(mock(GatewayDiscordClient.class), "hold_reroll",
                "",
                "3",
                "EMPTY",
                "6",
                "2;3;4",
                "5;6",
                "1",
                "0"))).containsExactly("1;1;1", "6", "2;3;4", "5;6", "1", "0");
    }

    @Test
    void getConfigFromEvent_finish() {
        assertThat(underTest.getConfigFromEvent(TestUtils.createEventWithCustomId(mock(GatewayDiscordClient.class), "hold_reroll",
                "",
                "finish",
                "1;2;3;4;5;6",
                "6",
                "2;3;4",
                "5;6",
                "1",
                "0"))).containsExactly("1;2;3;4;5;6", "6", "2;3;4", "5;6", "1", "0");
    }

    @Test
    void getConfigFromEvent_clear() {
        assertThat(underTest.getConfigFromEvent(TestUtils.createEventWithCustomId(mock(GatewayDiscordClient.class), "hold_reroll",
                "",
                "clear",
                "1;2;3;4;5;6",
                "6",
                "2;3;4",
                "5;6",
                "1",
                "1"))).containsExactly("EMPTY", "6", "2;3;4", "5;6", "1", "0");
    }

    @Test
    void getConfigFromEvent_reroll() {
        assertThat(underTest.getConfigFromEvent(TestUtils.createEventWithCustomId(mock(GatewayDiscordClient.class), "hold_reroll",
                "",
                "reroll",
                "1;2;3;4;5;6",
                "6",
                "2;3;4",
                "5;6",
                "1",
                "1"))).containsExactly("1;1;1;1;5;6", "6", "2;3;4", "5;6", "1", "2");
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
    void getButtonMessage_clear(){
        String res = underTest.getButtonMessage("clear", ImmutableList.of("1;2;3;4;5;6", "6", "2;3;4", "5;6", "1", "2"));

        assertThat(res).isEqualTo("Click on the buttons to roll dice");
    }

    @Test
    void getButtonMessage_finish(){
        String res = underTest.getButtonMessage("finish", ImmutableList.of("1;2;3;4;5;6", "6", "2;3;4", "5;6", "1", "2"));

        assertThat(res).isEqualTo("Click on the buttons to roll dice");
    }

    @Test
    void getButtonMessage_noRerollPossible(){
        String res = underTest.getButtonMessage("reroll", ImmutableList.of("1;1;1;5;5;6", "6", "2;3;4", "5;6", "1", "2"));

        assertThat(res).isEqualTo("Click on the buttons to roll dice");
    }

    @Test
    void getButtonMessage_rerollPossible(){
        String res = underTest.getButtonMessage("reroll", ImmutableList.of("1;2;3;4;5;6", "6", "2;3;4", "5;6", "1", "2"));

        assertThat(res).isEqualTo("[**1**,2,3,4,**5**,**6**] = 2 successes and 1 failures");
    }
}