package de.janno.discord.bot.command.customParameter;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomParameterCommandMockTest {

    MessageDataDAO messageDataDAO;
    AtomicLong messageIdCounter;
    CustomParameterCommand underTest;

    @BeforeEach
    void setup() {
        messageIdCounter = new AtomicLong(0);
        messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new CustomParameterCommand(messageDataDAO);
    }

    @Test
    void roll() {
        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("1");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d1 ⇒ 1, 1, 1, 1, description=1, 1, 1, 1, fieldValues:, answerChannel:null",
                "createButtonMessage: content=*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "deleteMessage: 0");
    }

    @Test
    void clear() {
        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("clear");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10");
    }

    @Test
    void roll_pinned() {
        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("1");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d1 ⇒ 1, 1, 1, 1, description=1, 1, 1, 1, fieldValues:, answerChannel:null",
                "createButtonMessage: content=*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10");
    }

    @Test
    void roll_answerChannel() {
        CustomParameterConfig config = new CustomParameterConfig(2L, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("1");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d1 ⇒ 1, 1, 1, 1, description=1, 1, 1, 1, fieldValues:, answerChannel:2");
    }

    @Test
    void roll_pinnedTwice() {
        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("1");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("1");
        underTest.handleComponentInteractEvent(click4).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d1 ⇒ 1, 1, 1, 1, description=1, 1, 1, 1, fieldValues:, answerChannel:null",
                "createButtonMessage: content=*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10");
        assertThat(click3.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click4.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d1 ⇒ 1, 1, 1, 1, description=1, 1, 1, 1, fieldValues:, answerChannel:null",
                "createButtonMessage: content=*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "deleteMessage: 1");
    }

    @Test
    void roll_answerChannelTwice() {
        CustomParameterConfig config = new CustomParameterConfig(2L, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("1");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("1");
        underTest.handleComponentInteractEvent(click4).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d1 ⇒ 1, 1, 1, 1, description=1, 1, 1, 1, fieldValues:, answerChannel:2");
        assertThat(click3.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click4.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d1 ⇒ 1, 1, 1, 1, description=1, 1, 1, 1, fieldValues:, answerChannel:2");
    }
}
