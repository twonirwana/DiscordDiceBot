package de.janno.discord.bot.command.fate;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class FateCommandMockTest {
    MessageDataDAO messageDataDAO;
    AtomicLong messageIdCounter;

    @BeforeEach
    void setup() {
        messageIdCounter = new AtomicLong(0);
        messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_simple_full() {
        FateCommand underTest = new FateCommand(messageDataDAO, new DiceUtils(0L));
        FateConfig config = new FateConfig(null, "simple", AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<FateConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("fate", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4dF ⇒ -1, description=[▢,＋,−,−], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click a button to roll four fate dice, buttonValues=roll",
                "deleteMessage: 0");
    }

    @Test
    void roll_simple_compact() {
        FateCommand underTest = new FateCommand(messageDataDAO, new DiceUtils(0L));
        FateConfig config = new FateConfig(null, "simple", AnswerFormatType.compact);
        ButtonEventAdaptorMockFactory<FateConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("fate", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=__**4dF ⇒ -1**__  [▢,＋,−,−], fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click a button to roll four fate dice, buttonValues=roll",
                "deleteMessage: 0");
    }

    @Test
    void roll_simple_minimal() {
        FateCommand underTest = new FateCommand(messageDataDAO, new DiceUtils(0L));
        FateConfig config = new FateConfig(null, "simple", AnswerFormatType.minimal);
        ButtonEventAdaptorMockFactory<FateConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("fate", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=4dF ⇒ -1, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click a button to roll four fate dice, buttonValues=roll",
                "deleteMessage: 0");
    }

    @Test
    void roll_modifier_full() {
        FateCommand underTest = new FateCommand(messageDataDAO, new DiceUtils(0L));
        FateConfig config = new FateConfig(null, "with_modifier", AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<FateConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("fate", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4dF +3 ⇒ 2, description=[▢,＋,−,−], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click a button to roll four fate dice and add the value of the button, buttonValues=-4,-3,-2,-1,0,1,2,3,4,5,6,7,8,9,10",
                "deleteMessage: 0");
    }

    @Test
    void roll_modifier_compact() {
        FateCommand underTest = new FateCommand(messageDataDAO, new DiceUtils(0L));
        FateConfig config = new FateConfig(null, "with_modifier", AnswerFormatType.compact);
        ButtonEventAdaptorMockFactory<FateConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("fate", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=__**4dF +3 ⇒ 2**__  [▢,＋,−,−], fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click a button to roll four fate dice and add the value of the button, buttonValues=-4,-3,-2,-1,0,1,2,3,4,5,6,7,8,9,10",
                "deleteMessage: 0");
    }

    @Test
    void roll_modifier_minimal() {
        FateCommand underTest = new FateCommand(messageDataDAO, new DiceUtils(0L));
        FateConfig config = new FateConfig(null, "with_modifier", AnswerFormatType.minimal);
        ButtonEventAdaptorMockFactory<FateConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("fate", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=4dF +3 ⇒ 2, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click a button to roll four fate dice and add the value of the button, buttonValues=-4,-3,-2,-1,0,1,2,3,4,5,6,7,8,9,10",
                "deleteMessage: 0");
    }
}
