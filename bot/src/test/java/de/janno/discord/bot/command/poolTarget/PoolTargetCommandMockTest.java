package de.janno.discord.bot.command.poolTarget;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class PoolTargetCommandMockTest {
    MessageDataDAO messageDataDAO;
    AtomicLong messageIdCounter;

    @BeforeEach
    void setup() {
        messageIdCounter = new AtomicLong(0);
        messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_full() {
        PoolTargetCommand underTest = new PoolTargetCommand(messageDataDAO, new DiceUtils(0L));
        PoolTargetConfig config = new PoolTargetConfig(null, 10, 15, Set.of(9, 10), Set.of(1, 2), "ask", AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<PoolTargetConfig, PoolTargetStateData> factory = new ButtonEventAdaptorMockFactory<>("pool_target", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("9");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("6");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("do_reroll");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on the target to roll 9d10 against it, with ask reroll:9,10 and botch:1,2, buttonValues=2,3,4,5,6,7,8,9,10,clear"
        );

        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Should 9s,10s in 9d10 against 6 be be rerolled?, buttonValues=do_reroll,no_reroll");

        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "createAnswer: title=9d10 ≥6 ⇒ 3, description=[**1**,**1**,**1**,3,3,5,5,**6**,**6**,**9**,**10**,**10**,**10**], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }

    @Test
    void roll_compact() {
        PoolTargetCommand underTest = new PoolTargetCommand(messageDataDAO, new DiceUtils(0L));
        PoolTargetConfig config = new PoolTargetConfig(null, 10, 15, Set.of(9, 10), Set.of(1, 2), "ask", AnswerFormatType.compact);
        ButtonEventAdaptorMockFactory<PoolTargetConfig, PoolTargetStateData> factory = new ButtonEventAdaptorMockFactory<>("pool_target", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("9");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("6");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("do_reroll");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on the target to roll 9d10 against it, with ask reroll:9,10 and botch:1,2, buttonValues=2,3,4,5,6,7,8,9,10,clear"
        );

        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Should 9s,10s in 9d10 against 6 be be rerolled?, buttonValues=do_reroll,no_reroll");

        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "createAnswer: title=null, description=__**9d10 ≥6 ⇒ 3**__  [**1**,**1**,**1**,3,3,5,5,**6**,**6**,**9**,**10**,**10**,**10**], fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }

    @Test
    void roll_minimal() {
        PoolTargetCommand underTest = new PoolTargetCommand(messageDataDAO, new DiceUtils(0L));
        PoolTargetConfig config = new PoolTargetConfig(null, 10, 15, Set.of(9, 10), Set.of(1, 2), "ask", AnswerFormatType.minimal);
        ButtonEventAdaptorMockFactory<PoolTargetConfig, PoolTargetStateData> factory = new ButtonEventAdaptorMockFactory<>("pool_target", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("9");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("6");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("do_reroll");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on the target to roll 9d10 against it, with ask reroll:9,10 and botch:1,2, buttonValues=2,3,4,5,6,7,8,9,10,clear"
        );

        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Should 9s,10s in 9d10 against 6 be be rerolled?, buttonValues=do_reroll,no_reroll");

        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "createAnswer: title=null, description=9d10 ≥6 ⇒ 3, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2, buttonValues=1,2,3,4,5,6,7,8,9,10,11,12,13,14,15",
                "deleteMessageById: 0");
    }


}
