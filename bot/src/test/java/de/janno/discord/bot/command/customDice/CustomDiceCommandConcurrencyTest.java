package de.janno.discord.bot.command.customDice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import io.avaje.config.Config;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static shadow.org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CustomDiceCommandConcurrencyTest {
    PersistenceManager persistenceManager;

    @BeforeEach
    void setup() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @AfterEach
    void cleanUp() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
        Config.setProperty("diceEvaluator.cacheSize", "0");
    }


    @Test
    void roll_diceEvaluator_full() throws InterruptedException {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");
        CountDownLatch finishLine = new CountDownLatch(2);
        CountDownLatch latch = new CountDownLatch(1);


        Runnable task = () -> {
            try {
                latch.await();
                underTest.handleComponentInteractEvent(buttonEvent)
                        .block();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finishLine.countDown();
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);

        t1.start();
        t2.start();

        latch.countDown();
        finishLine.await();
        underTest.handleComponentInteractEvent(buttonEvent)
                .block();
        assertThat(buttonEvent.getSortedActions()).isEqualTo(List.of(
                "acknowledge",
                "acknowledge",
                "acknowledge",
                "deleteMessageById: 0",
                "deleteMessageById: 0",
                "deleteMessageById: 2",
                "getMessagesState: [2]",
                "sendMessage: EmbedOrMessageDefinition(title=Dmg ⇒ 3, descriptionOrContent=1d6: [3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED, userReference=true, sendToOtherChannelId=null)",
                "sendMessage: EmbedOrMessageDefinition(title=Dmg ⇒ 4, descriptionOrContent=1d6: [4], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED, userReference=true, sendToOtherChannelId=null)",
                "sendMessage: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(componentDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false, emoji=null)])], hasImage=false, type=MESSAGE, userReference=false, sendToOtherChannelId=null)",
                "sendMessage: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(componentDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false, emoji=null)])], hasImage=false, type=MESSAGE, userReference=false, sendToOtherChannelId=null)"
        ));
    }


}
