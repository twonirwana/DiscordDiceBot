package de.janno.discord.command;

import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CountSuccessesCommandTest {

    CountSuccessesCommand underTest;

    @BeforeEach
    void setup() {
        underTest = new CountSuccessesCommand(new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
    }

    @Test
    void getCommandDescription() {
        assertThat(underTest.getCommandDescription()).isEqualTo("Configure buttons for dice, with the same side, that counts successes against a target number");
    }

    @Test
    void getName() {
        assertThat(underTest.getName()).isEqualTo("count_successes");
    }


    @Test
    void getButtonMessage_noGlitch() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "no_glitch", 15);
        assertThat(underTest.getButtonMessage(null, config)).isEqualTo("Click to roll the dice against 6");
    }


    @Test
    void getButtonMessage_halfDiceOne() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "half_dice_one", 15);

        assertThat(underTest.getButtonMessage(null, config)).isEqualTo("Click to roll the dice against 6 and check for more then half of dice 1s");
    }

    @Test
    void getButtonMessage_countOnes() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "count_ones", 15);

        assertThat(underTest.getButtonMessage(null, config)).isEqualTo("Click to roll the dice against 6 and count the 1s");
    }

    @Test
    void getButtonMessage_subtractOnes() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "subtract_ones", 15);

        assertThat(underTest.getButtonMessage(null, config)).isEqualTo("Click to roll the dice against 6 minus 1s");
    }

    @Test
    void getConfigFromEvent_legacyOnlyTwo() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,1,6,6");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesCommand.Config(6, 6, "no_glitch", 15));
    }

    @Test
    void getConfigFromEvent_legacyOnlyThree() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,1,6,6,no_glitch");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesCommand.Config(6, 6, "no_glitch", 15));
    }

    @Test
    void getConfigFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,1,6,6,no_glitch,15");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesCommand.Config(6, 6, "no_glitch", 15));
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("count_successes,x")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("count_successe")).isFalse();
    }

    @Test
    void rollDice() {
        List<DiceResult> results = underTest.getDiceResult(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "no_glitch", 15));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResultTitle()).isEqualTo("6d6 = 1");
        assertThat(results.get(0).getResultDetails()).isEqualTo("[1,1,1,1,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_halfDiceOne_glitch() {
        List<DiceResult> results = underTest.getDiceResult(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "half_dice_one", 15));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResultTitle()).isEqualTo("6d6 = 1 - Glitch!");
        assertThat(results.get(0).getResultDetails()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1 and more then half of all dice show 1s");
    }

    @Test
    void rollDice_halfDiceOne_noGlitch() {
        List<DiceResult> results = underTest.getDiceResult(new CountSuccessesCommand.State(8), new CountSuccessesCommand.Config(6, 6, "half_dice_one", 15));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResultTitle()).isEqualTo("8d6 = 3");
        assertThat(results.get(0).getResultDetails()).isEqualTo("[1,1,1,1,5,**6**,**6**,**6**] ≥6 = 3");
    }

    @Test
    void rollDice_countOnes() {
        List<DiceResult> results = underTest.getDiceResult(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "count_ones", 15));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResultTitle()).isEqualTo("6d6 = 1 successes and 4 ones");
        assertThat(results.get(0).getResultDetails()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_subtractOnes() {
        List<DiceResult> results = underTest.getDiceResult(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "subtract_ones", 15));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResultTitle()).isEqualTo("6d6 = -3");
        assertThat(results.get(0).getResultDetails()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 -1s = -3");
    }

    @Test
    void getStartOptions() {
        List<ApplicationCommandOptionData> res = underTest.getStartOptions();

        assertThat(res.stream().map(ApplicationCommandOptionData::name)).containsExactly("dice_sides", "target_number", "glitch", "max_dice");
    }


    @Test
    void getStateFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,4");

        CountSuccessesCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new CountSuccessesCommand.State(4));
    }

    @Test
    void createButtonCustomId() {
        String res = underTest.createButtonCustomId("10", new CountSuccessesCommand.Config(6, 4, "half_dice_one", 12));

        assertThat(res).isEqualTo("count_successes,10,6,4,half_dice_one,12");
    }
  /*  @Test
    void handleComponentInteractEvent() {
        GatewayDiscordClient gatewayDiscordClientMock = mock(GatewayDiscordClient.class);
        TextChannel textChannel = new TextChannel(gatewayDiscordClientMock, ChannelData.builder()
                .id(1)
                .type(1)
                .build());
        Mockito.when(gatewayDiscordClientMock.getChannelById(any()))
                .thenReturn(Mono.just(textChannel));
        //Mono.when(discord4JAdapterMock.)
        Mono<Void> res = underTest.handleComponentInteractEvent(TestUtils.createEventWithCustomId(gatewayDiscordClientMock, "count_successes",
                "1", "6", "6", "6", "no_glitch", "15"));
        StepVerifier.create(res)

                .verifyComplete();
    }*/
}