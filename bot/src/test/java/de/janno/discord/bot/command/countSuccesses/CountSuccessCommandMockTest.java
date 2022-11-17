package de.janno.discord.bot.command.countSuccesses;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
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
    void roll_noGlitch_full() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d6 ⇒ 2, description=[1,1,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click to roll the dice against 4, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }


    @Test
    void roll_subtractOnes_full() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "subtract_ones", 15, 1, Set.of(), Set.of(1), AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d6 ⇒ 0, description=[**1**,**1**,**5**,**6**] ≥4 = 0, remove success for: [1], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click to roll the dice against 4, remove success for: [1] minus 1s, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }

    @Test
    void roll_halfOnes_full() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d6 ⇒ 2, description=[1,1,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click to roll the dice against 4 and check for more then half of dice 1s, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }

    @Test
    void roll_countOnes_full() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d6 ⇒ 2 successes and 2 ones, description=[**1**,**1**,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click to roll the dice against 4 and count the 1s, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }

    @Test
    void roll_noGlitch_compact() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.compact);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=__**4d6 ⇒ 2**__  [1,1,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click to roll the dice against 4, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }


    @Test
    void roll_subtractOnes_compact() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "subtract_ones", 15, 1, Set.of(), Set.of(1), AnswerFormatType.compact);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=__**4d6 ⇒ 0**__  [**1**,**1**,**5**,**6**] ≥4 = 0, remove success for: [1], fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click to roll the dice against 4, remove success for: [1] minus 1s, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }

    @Test
    void roll_halfOnes_compact() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.compact);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=__**4d6 ⇒ 2**__  [1,1,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click to roll the dice against 4 and check for more then half of dice 1s, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }

    @Test
    void roll_countOnes_compact() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.compact);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=__**4d6 ⇒ 2 successes and 2 ones**__  [**1**,**1**,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click to roll the dice against 4 and count the 1s, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }

    @Test
    void roll_noGlitch_minimal() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.minimal);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=4d6 ⇒ 2, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click to roll the dice against 4, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }


    @Test
    void roll_subtractOnes_minimal() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "subtract_ones", 15, 1, Set.of(), Set.of(1), AnswerFormatType.minimal);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=4d6 ⇒ 0, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click to roll the dice against 4, remove success for: [1] minus 1s, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }

    @Test
    void roll_halfOnes_minimal() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.minimal);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=4d6 ⇒ 2, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click to roll the dice against 4 and check for more then half of dice 1s, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }

    @Test
    void roll_countOnes_minimal() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.minimal);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=4d6 ⇒ 2 successes and 2 ones, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click to roll the dice against 4 and count the 1s, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }

    @Test
    void roll_pinned() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 4, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, true);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:Click to roll the dice against 4, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "createAnswer: title=4d6 ⇒ 2, description=[1,1,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click to roll the dice against 4, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15");
    }

    @Test
    void roll_answerChannel() {
        CountSuccessesConfig config = new CountSuccessesConfig(2L, 6, 4, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CountSuccessesConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("count_successes", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("4");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:Click to roll the dice against 4, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "createAnswer: title=4d6 ⇒ 2, description=[1,1,**5**,**6**] ≥4 = 2, fieldValues:, answerChannel:2, type:EMBED"
        );
    }
}
