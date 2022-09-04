package de.janno.discord.bot.command.countSuccesses;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class CountSuccessCommandMockTest {
    MessageDataDAO messageDataDAO;
    AtomicLong messageIdCounter;
    CountSuccessesCommand underTest;

    @BeforeEach
    void setup() {
        messageIdCounter = new AtomicLong(0);
        messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new CountSuccessesCommand(messageDataDAO, new DiceUtils(1, 1, 5, 6));
    }

    @Test
    void roll_noGlitch() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "no_glitch", 15);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d6 = 2, description=[1,1,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click to roll the dice against 4, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessage: 0");
    }


    @Test
    void roll_subtractOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "subtract_ones", 15);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d6 = 0, description=[**1**,**1**,**5**,**6**] ≥4 -1s = 0, fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click to roll the dice against 4 minus 1s, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessage: 0");
    }

    @Test
    void roll_halfOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "half_dice_one", 15);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d6 = 2, description=[1,1,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click to roll the dice against 4 and check for more then half of dice 1s, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessage: 0");
    }

    @Test
    void roll_countOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "count_ones", 15);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d6 = 2 successes and 2 ones, description=[**1**,**1**,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click to roll the dice against 4 and count the 1s, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessage: 0");
    }

    @Test
    void roll_pinned() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "no_glitch", 15);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, true);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click to roll the dice against 4, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "createAnswer: title=4d6 = 2, description=[1,1,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click to roll the dice against 4, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15");
    }

    @Test
    void roll_answerChannel() {
        CountSuccessesConfig config = new CountSuccessesConfig(2L, 6, 4, "no_glitch", 15);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click to roll the dice against 4, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "createAnswer: title=4d6 = 2, description=[1,1,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:2");
    }
}
