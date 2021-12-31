package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayDeque;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SumDiceSetCommandTest {
    SumDiceSetCommand underTest;

    static Stream<Arguments> generateEditMessageData() {
        return Stream.of(
                Arguments.of(ImmutableList.of(), "+1d4", "1d4"),
                Arguments.of(ImmutableList.of(), "+1d6", "1d6"),
                Arguments.of(ImmutableList.of(), "+1d8", "1d8"),
                Arguments.of(ImmutableList.of(), "+1d10", "1d10"),
                Arguments.of(ImmutableList.of(), "+1d12", "1d12"),
                Arguments.of(ImmutableList.of(), "+1d20", "1d20"),
                Arguments.of(ImmutableList.of(), "+1", "1"),
                Arguments.of(ImmutableList.of(), "+5", "5"),
                Arguments.of(ImmutableList.of(), "+10", "10"),

                Arguments.of(ImmutableList.of("1d4"), "-1d4", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("1d6"), "-1d6", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("1d8"), "-1d8", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("1d10"), "-1d10", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("1d12"), "-1d12", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("1d20"), "-1d20", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("+1"), "-1", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("-1d4"), "+1d4", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("-1d6"), "+1d6", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("-1d8"), "+1d8", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("-1d10"), "+1d10", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("-1d12"), "+1d12", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("-1d20"), "+1d20", "Click on the buttons to add dice to the set"),
                Arguments.of(ImmutableList.of("-1"), "+1", "Click on the buttons to add dice to the set"),

                Arguments.of(ImmutableList.of(), "-1d4", "-1d4"),
                Arguments.of(ImmutableList.of(), "-1d6", "-1d6"),
                Arguments.of(ImmutableList.of(), "-1d8", "-1d8"),
                Arguments.of(ImmutableList.of(), "-1d10", "-1d10"),
                Arguments.of(ImmutableList.of(), "-1d12", "-1d12"),
                Arguments.of(ImmutableList.of(), "-1d20", "-1d20"),
                Arguments.of(ImmutableList.of(), "-1", "-1"),
                Arguments.of(ImmutableList.of(), "-5", "-5"),

                Arguments.of(ImmutableList.of("+10"), "-5", "5"),
                Arguments.of(ImmutableList.of("+2"), "-5", "-3"),
                Arguments.of(ImmutableList.of("-2"), "+5", "3"),
                Arguments.of(ImmutableList.of("-10"), "+5", "-5"),

                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "+1d4", "2d4 +1d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "+1d6", "1d4 +2d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "+1d8", "1d4 +1d6 +2d8 +1d10 +1d12 +1d20"),
                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "+1d10", "1d4 +1d6 +1d8 +2d10 +1d12 +1d20"),
                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "+1d12", "1d4 +1d6 +1d8 +1d10 +2d12 +1d20"),
                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "+1d20", "1d4 +1d6 +1d8 +1d10 +1d12 +2d20"),
                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "+1", "1d4 +1d6 +1d8 +1d10 +1d12 +1d20 +1"),

                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "-1d4", "1d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "-1d6", "1d4 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "-1d8", "1d4 +1d6 +1d10 +1d12 +1d20"),
                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "-1d10", "1d4 +1d6 +1d8 +1d12 +1d20"),
                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "-1d12", "1d4 +1d6 +1d8 +1d10 +1d20"),
                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "-1d20", "1d4 +1d6 +1d8 +1d10 +1d12"),
                Arguments.of(ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"), "-1", "1d4 +1d6 +1d8 +1d10 +1d12 +1d20 -1"),

                Arguments.of(ImmutableList.of("100d4", "100d6", "100d8", "100d10", "100d12", "100d20"), "+1d4", "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(ImmutableList.of("100d4", "100d6", "100d8", "100d10", "100d12", "100d20"), "+1d6", "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(ImmutableList.of("100d4", "100d6", "100d8", "100d10", "100d12", "100d20"), "+1d8", "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(ImmutableList.of("100d4", "100d6", "100d8", "100d10", "100d12", "100d20"), "+1d10", "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(ImmutableList.of("100d4", "100d6", "100d8", "100d10", "100d12", "100d20"), "+1d12", "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(ImmutableList.of("100d4", "100d6", "100d8", "100d10", "100d12", "100d20"), "+1d20", "100d4 +100d6 +100d8 +100d10 +100d12 +100d20")
        );
    }

    @BeforeEach
    void setup() {
        underTest = new SumDiceSetCommand(new DiceUtils(new ArrayDeque<>(ImmutableList.of(1, 1, 1, 1, 5, 6, 6, 6))));
    }

    @Test
    void editMessage_clear() {
        String res = underTest.editMessage("clear", ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"));
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void editMessage_roll() {
        String res = underTest.editMessage("roll", ImmutableList.of("1d4", "1d6", "1d8", "1d10", "1d12", "1d20"));
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void editMessage_x2() {
        String res = underTest.editMessage("x2", ImmutableList.of("1d4", "2d6", "3d8", "4d10", "5d12", "+10"));
        assertThat(res).isEqualTo("2d4 +4d6 +6d8 +8d10 +10d12 +20");
    }

    @Test
    void editMessageNegativeModifier_x2() {
        String res = underTest.editMessage("x2", ImmutableList.of("-1d4", "-2d6", "-3d8", "-4d10", "5d12", "-10"));
        assertThat(res).isEqualTo("-2d4 -4d6 -6d8 -8d10 +10d12 -20");
    }

    @Test
    void editMessage_limit() {
        String res = underTest.editMessage("x2", ImmutableList.of("51d4"));
        assertThat(res).isEqualTo("100d4");
    }


    @ParameterizedTest(name = "{index} config={0}, buttonId={1} -> {2}")
    @MethodSource("generateEditMessageData")
    void editMessage(List<String> config, String buttonId, String expected) {
        String res = underTest.editMessage(buttonId, config);
        assertThat(res).isEqualTo(expected);
    }

    @Test
    void getName() {
        assertThat(underTest.getName()).isEqualTo("sum_dice_set");
    }

    @Test
    void getConfigFromEvent_1d6() {
        assertThat(underTest.getConfigFromEvent(TestUtils.createEventWithCustomId(mock(GatewayDiscordClient.class), "sum_dice_set",
                "1d6", "+1d21"))).containsExactly("+1d6");
    }

    @Test
    void getConfigFromEvent_1d4_2d6_3d8_4d12_5d20() {
        assertThat(underTest.getConfigFromEvent(TestUtils.createEventWithCustomId(mock(GatewayDiscordClient.class), "sum_dice_set",
                "1d4 +2d6 +3d8 +4d12 +5d20", "+1d21"))).containsExactly("+1d4", "+2d6", "+3d8", "+4d12", "+5d20");
    }

    @Test
    void getConfigFromEvent_legacy() {
        assertThat(underTest.getConfigFromEvent(TestUtils.createEventWithCustomId(mock(GatewayDiscordClient.class), "sum_dice_set",
                "1d4 + 2d6", "+1d21"))).containsExactly("+1d4", "+2d6");
    }

    @Test
    void getConfigFromEvent_empty() {
        assertThat(underTest.getConfigFromEvent(TestUtils.createEventWithCustomId(mock(GatewayDiscordClient.class), "sum_dice_set",
                "Click on the buttons to add dice to the set", "+1d21"))).isEmpty();
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_set,x")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_se")).isFalse();
    }

    @Test
    void createNewMessage_roll_true() {
        boolean res = underTest.createAnswerMessage("roll", ImmutableList.of("1d6"));
        assertThat(res).isTrue();
    }

    @Test
    void createNewMessage_rollNoConfig_false() {
        boolean res = underTest.createAnswerMessage("roll", ImmutableList.of());
        assertThat(res).isFalse();
    }

    @Test
    void createNewMessage_modifyMessage_false() {
        boolean res = underTest.createAnswerMessage("+1d6", ImmutableList.of("1d6"));
        assertThat(res).isFalse();
    }

    @Test
    void copyButtonMessageToTheEnd_roll_true() {
        boolean res = underTest.copyButtonMessageToTheEnd("roll", ImmutableList.of("1d6"));
        assertThat(res).isTrue();
    }

    @Test
    void copyButtonMessageToTheEnd_rollNoConfig_false() {
        boolean res = underTest.copyButtonMessageToTheEnd("roll", ImmutableList.of());
        assertThat(res).isFalse();
    }

    @Test
    void copyButtonMessageToTheEnd_modifyMessage_false() {
        boolean res = underTest.copyButtonMessageToTheEnd("+1d6", ImmutableList.of("1d6"));
        assertThat(res).isFalse();
    }

    @Test
    void getButtonMessage_empty() {
        String res = underTest.getButtonMessage(null, ImmutableList.of());
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void getButtonMessage_1d6() {
        String res = underTest.getButtonMessage(null, ImmutableList.of("1d6"));
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void getConfigValuesFromStartOptions() {
        List<String> res = underTest.getConfigValuesFromStartOptions(null);
        assertThat(res).isEmpty();
    }


    @Test
    void rollDice_1d4plus1d6plus10() {
        List<DiceResult> res = underTest.getDiceResult("roll", ImmutableList.of("+1d4", "+1d6", "+10"));

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("1d4 +1d6 +10 = 12");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[1, 1, 10]");
    }

    @Test
    void rollDice_minus1d4plus1d6minux10() {
        List<DiceResult> res = underTest.getDiceResult("roll", ImmutableList.of("-1d4", "+1d6", "-10"));

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("-1d4 +1d6 -10 = -10");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[-1, 1, -10]");
    }
    @Test
    void getStartOptions() {
        List<ApplicationCommandOptionData> res = underTest.getStartOptions();

        assertThat(res).isEmpty();
    }
}