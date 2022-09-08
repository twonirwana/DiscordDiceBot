package de.janno.discord.bot.command.sumCustomSet;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class SumCustomSetCommandMockTest {

    MessageDataDAO messageDataDAO;
    AtomicLong messageIdCounter;
    SumCustomSetCommand underTest;

    @BeforeEach
    void setup() {
        messageIdCounter = new AtomicLong(0);
        messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new SumCustomSetCommand(messageDataDAO);
    }

    @Test
    void roll() {
        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "+1", "1"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2", "2")));
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge", "editMessage: message:invokingUser∶ 1, buttonValues=");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge", "editMessage: message:invokingUser∶ 1+2, buttonValues=");
        assertThat(click3.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=",
                "createAnswer: title=1+2 = 3, description=[1, 2], fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "deleteMessage: 0");
    }

    @Test
    void clear() {
        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "+1", "1"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2", "2")));
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("clear");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge", "editMessage: message:invokingUser∶ 1, buttonValues=");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=");
    }

    @Test
    void backBack() {
        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "+1", "1"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2", "2")));
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("back");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("back");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge", "editMessage: message:invokingUser∶ 1, buttonValues=");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=");
    }

    @Test
    void roll_pinned() {
        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "+1", "1"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2", "2")));
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶ 1, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶ 1+2, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click3.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=1+2 = 3, description=[1, 2], fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back");
    }

    @Test
    void roll_answerChannel() {
        SumCustomSetConfig config = new SumCustomSetConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "+1", "1"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2", "2")));
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶ 1, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶ 1+2, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click3.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=1+2 = 3, description=[1, 2], fieldValues:, answerChannel:2");
    }

    @Test
    void roll_pinnedTwice() {
        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "+1", "1"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2", "2")));
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click4).block();
        ButtonEventAdaptorMock click5 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click5).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶ 1, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶ 1+2, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click3.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=1+2 = 3, description=[1, 2], fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click4.getActions()).containsExactly(
                "acknowledge", "editMessage: message:invokingUser∶ 1, buttonValues=");
        assertThat(click5.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=",
                "createAnswer: title=1 = 1, description=[1], fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "deleteMessage: 1");
    }

    @Test
    void roll_answerChannelTwice() {
        SumCustomSetConfig config = new SumCustomSetConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "+1", "1"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2", "2")));
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click4).block();
        ButtonEventAdaptorMock click5 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click5).block();

        assertThat(click1.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶ 1, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶ 1+2, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click3.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=1+2 = 3, description=[1, 2], fieldValues:, answerChannel:2");
        assertThat(click4.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:invokingUser∶ 1, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click5.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=1 = 1, description=[1], fieldValues:, answerChannel:2");
    }
}
