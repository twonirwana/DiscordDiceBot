package de.janno.discord.bot.command.customParameter;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomParameterCommandMockTest {

    MessageDataDAO messageDataDAO;
    AtomicLong messageIdCounter;

    @BeforeEach
    void setup() {
        messageIdCounter = new AtomicLong(0);
        messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_full() {
        CustomParameterCommand underTest = new CustomParameterCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("6");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d6 ⇒ 2, 3, 1, 4, description=[2, 3, 1, 4], fieldValues:, answerChannel:null, type:EMBED",
                "deleteMessageById: 0",
                "createButtonMessage: content=*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10");
    }

    @Test
    void roll_compact() {
        CustomParameterCommand underTest = new CustomParameterCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("6");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=__**4d6 ⇒ 2, 3, 1, 4**__  [2, 3, 1, 4], fieldValues:, answerChannel:null, type:MESSAGE",
                "deleteMessageById: 0",
                "createButtonMessage: content=*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10");
    }

    @Test
    void roll_minimal() {
        CustomParameterCommand underTest = new CustomParameterCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.minimal);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("6");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=4d6 ⇒ 2, 3, 1, 4, fieldValues:, answerChannel:null, type:MESSAGE",
                "deleteMessageById: 0",
                "createButtonMessage: content=*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10");
    }

    @Test
    void clear() {
        CustomParameterCommand underTest = new CustomParameterCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("clear");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "editMessage: message:*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10");
    }

    @Test
    void roll_pinned() {
        CustomParameterCommand underTest = new CustomParameterCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("6");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "editMessage: message:*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d6 ⇒ 2, 3, 1, 4, description=[2, 3, 1, 4], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10");
    }

    @Test
    void roll_answerChannel() {
        CustomParameterCommand underTest = new CustomParameterCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);

        CustomParameterConfig config = new CustomParameterConfig(2L, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("6");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "editMessage: message:*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d6 ⇒ 2, 3, 1, 4, description=[2, 3, 1, 4], fieldValues:, answerChannel:2, type:EMBED"
        );
    }

    @Test
    void roll_pinnedTwice() {
        CustomParameterCommand underTest = new CustomParameterCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("6");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("6");
        underTest.handleComponentInteractEvent(click4).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "editMessage: message:*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d6 ⇒ 2, 3, 1, 4, description=[2, 3, 1, 4], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10");
        assertThat(click3.getActions()).containsExactly(
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click4.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d6 ⇒ 1, 1, 6, 3, description=[1, 1, 6, 3], fieldValues:, answerChannel:null, type:EMBED",
                "deleteMessageById: 1",
                "createButtonMessage: content=*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10");
    }

    @Test
    void roll_answerChannelTwice() {
        CustomParameterCommand underTest = new CustomParameterCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);

        CustomParameterConfig config = new CustomParameterConfig(2L, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("6");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("6");
        underTest.handleComponentInteractEvent(click4).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click2.getActions()).containsExactly(
                "editMessage: message:*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d6 ⇒ 2, 3, 1, 4, description=[2, 3, 1, 4], fieldValues:, answerChannel:2, type:EMBED"
        );
        assertThat(click3.getActions()).containsExactly(
                "editMessage: message:invokingUser∶4d*{sides}*: Please select value for *{sides}*, buttonValues=1,4,6,8,10,12,20,100,clear");
        assertThat(click4.getActions()).containsExactly(
                "editMessage: message:*{numberOfDice}*d*{sides}*: Please select value for *{numberOfDice}*, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d6 ⇒ 1, 1, 6, 3, description=[1, 1, 6, 3], fieldValues:, answerChannel:2, type:EMBED"
        );
    }
}
