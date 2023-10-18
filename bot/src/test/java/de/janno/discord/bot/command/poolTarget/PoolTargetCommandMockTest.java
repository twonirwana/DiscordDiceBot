package de.janno.discord.bot.command.poolTarget;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.AnswerFormatType;
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

public class PoolTargetCommandMockTest {
    PersistenceManager persistenceManager;

    @BeforeEach
    void setup() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_full() {
        PoolTargetCommand underTest = new PoolTargetCommand(persistenceManager, new DiceUtils(0L));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        PoolTargetConfig config = new PoolTargetConfig(null, 10, 15, Set.of(9, 10), Set.of(1, 2), "ask", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<PoolTargetConfig, PoolTargetStateData> factory = new ButtonEventAdaptorMockFactory<>("pool_target", underTest, config, persistenceManager, false);

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
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=9d10 ≥6 ⇒ 3, descriptionOrContent=[**1**,**1**,**1**,3,3,5,5,**6**,**6**,**9**,**10**,**10**,**10**], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d10, id=pool_target100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d10, id=pool_target200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d10, id=pool_target300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d10, id=pool_target400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d10, id=pool_target500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d10, id=pool_target600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d10, id=pool_target700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d10, id=pool_target800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d10, id=pool_target900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d10, id=pool_target1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d10, id=pool_target1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d10, id=pool_target1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d10, id=pool_target1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d10, id=pool_target1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d10, id=pool_target1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_compact() {
        PoolTargetCommand underTest = new PoolTargetCommand(persistenceManager, new DiceUtils(0L));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        PoolTargetConfig config = new PoolTargetConfig(null, 10, 15, Set.of(9, 10), Set.of(1, 2), "ask", AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<PoolTargetConfig, PoolTargetStateData> factory = new ButtonEventAdaptorMockFactory<>("pool_target", underTest, config, persistenceManager, false);

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
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**9d10 ≥6 ⇒ 3**__  [**1**,**1**,**1**,3,3,5,5,**6**,**6**,**9**,**10**,**10**,**10**], fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d10, id=pool_target100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d10, id=pool_target200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d10, id=pool_target300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d10, id=pool_target400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d10, id=pool_target500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d10, id=pool_target600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d10, id=pool_target700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d10, id=pool_target800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d10, id=pool_target900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d10, id=pool_target1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d10, id=pool_target1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d10, id=pool_target1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d10, id=pool_target1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d10, id=pool_target1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d10, id=pool_target1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_minimal() {
        PoolTargetCommand underTest = new PoolTargetCommand(persistenceManager, new DiceUtils(0L));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        PoolTargetConfig config = new PoolTargetConfig(null, 10, 15, Set.of(9, 10), Set.of(1, 2), "ask", AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<PoolTargetConfig, PoolTargetStateData> factory = new ButtonEventAdaptorMockFactory<>("pool_target", underTest, config, persistenceManager, false);

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
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=9d10 ≥6 ⇒ 3, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d10, id=pool_target100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d10, id=pool_target200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d10, id=pool_target300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4d10, id=pool_target400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5d10, id=pool_target500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6d10, id=pool_target600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7d10, id=pool_target700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8d10, id=pool_target800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9d10, id=pool_target900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10d10, id=pool_target1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11d10, id=pool_target1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12d10, id=pool_target1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13d10, id=pool_target1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14d10, id=pool_target1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15d10, id=pool_target1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }


}
