package de.janno.discord.bot.command.countSuccesses;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CountSuccessCommandMockTest {
    PersistenceManager persistenceManager;
    CountSuccessesCommand underTest;

    @BeforeEach
    void setup() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new CountSuccessesCommand(persistenceManager, new DiceUtils(1, 1, 5, 6));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
    }

    @Test
    void roll_noGlitch_full() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 2, descriptionOrContent=[1,1,**5**,**6**] ≥4 = 2, fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)",
                "deleteMessageById: 0");
    }


    @Test
    void roll_subtractOnes_full() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "subtract_ones", 15, 1, Set.of(), Set.of(1), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 0, descriptionOrContent=[**1**,**1**,**5**,**6**] ≥4 = 0, remove success for: [1], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4, remove success for: [1] minus 1s, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_halfOnes_full() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 2, descriptionOrContent=[1,1,**5**,**6**] ≥4 = 2, fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4 and check for more then half of dice 1s, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_countOnes_full() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 2 successes and 2 ones, descriptionOrContent=[**1**,**1**,**5**,**6**] ≥4 = 2, fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4 and count the 1s, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_noGlitch_compact() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**4d6 ⇒ 2**__  [1,1,**5**,**6**] ≥4 = 2, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }


    @Test
    void roll_subtractOnes_compact() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "subtract_ones", 15, 1, Set.of(), Set.of(1), AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**4d6 ⇒ 0**__  [**1**,**1**,**5**,**6**] ≥4 = 0, remove success for: [1], fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4, remove success for: [1] minus 1s, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)");
    }

    @Test
    void roll_halfOnes_compact() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**4d6 ⇒ 2**__  [1,1,**5**,**6**] ≥4 = 2, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4 and check for more then half of dice 1s, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_countOnes_compact() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**4d6 ⇒ 2 successes and 2 ones**__  [**1**,**1**,**5**,**6**] ≥4 = 2, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4 and count the 1s, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_noGlitch_minimal() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=4d6 ⇒ 2, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }


    @Test
    void roll_subtractOnes_minimal() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "subtract_ones", 15, 1, Set.of(), Set.of(1), AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=4d6 ⇒ 0, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4, remove success for: [1] minus 1s, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)");
    }

    @Test
    void roll_halfOnes_minimal() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=4d6 ⇒ 2, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4 and check for more then half of dice 1s, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_countOnes_minimal() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=4d6 ⇒ 2 successes and 2 ones, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4 and count the 1s, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_pinned() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, true);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click to roll the dice against 4, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 2, descriptionOrContent=[1,1,**5**,**6**] ≥4 = 2, fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click to roll the dice against 4, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=count_successes100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=count_successes200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d6, id=count_successes300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d6, id=count_successes400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d6, id=count_successes500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d6, id=count_successes600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d6, id=count_successes700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d6, id=count_successes800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d6, id=count_successes900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d6, id=count_successes1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d6, id=count_successes1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d6, id=count_successes1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d6, id=count_successes1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d6, id=count_successes1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d6, id=count_successes1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_answerChannel() {
        CountSuccessesConfig config = new CountSuccessesConfig(2L, 6, 4, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click to roll the dice against 4, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 2, descriptionOrContent=[1,1,**5**,**6**] ≥4 = 2, fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: 2"
        );
    }
}
