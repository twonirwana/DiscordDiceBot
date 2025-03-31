package de.janno.discord.bot.command.reroll;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.command.IncrementingUUIDSupplier;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import io.avaje.config.Config;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

@ExtendWith(SnapshotExtension.class)
public class RerollAnswerHandlerMockTest {
    PersistenceManager persistenceManager;
    Expect expect;

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
    void customDice_reroll() {
        Supplier<UUID> testingUUIDSupplier = IncrementingUUIDSupplier.create();
        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)), testingUUIDSupplier);
        RerollAnswerHandler rerollAnswerHandler = new RerollAnswerHandler(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(2)), testingUUIDSupplier);
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "3d6+5d!4+3d[a/b/c/d]", false, false, null)), AnswerFormatType.full, AnswerInteractionType.reroll, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", customDiceCommand, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        customDiceCommand.handleComponentInteractEvent(buttonEvent).block();

        expect.scenario("roll").toMatchSnapshot(buttonEvent.getSortedActions());

        EmbedOrMessageDefinition answer = buttonEvent.getSendMessages().getFirst();

        String dieIdButton = answer.getComponentRowDefinitions().getFirst().getComponentDefinitions().getFirst().getId();
        ButtonEventAdaptorMock answerButtonMessage1 = new ButtonEventAdaptorMock(dieIdButton, 2);
        rerollAnswerHandler.handleComponentInteractEvent(answerButtonMessage1).block();
        expect.scenario("select").toMatchSnapshot(answerButtonMessage1.getSortedActions());

        String secondDieIdButton = answer.getComponentRowDefinitions().getFirst().getComponentDefinitions().getLast().getId();
        ButtonEventAdaptorMock answerButtonMessage2 = new ButtonEventAdaptorMock(secondDieIdButton, 2);
        rerollAnswerHandler.handleComponentInteractEvent(answerButtonMessage2).block();
        expect.scenario("secondSelect").toMatchSnapshot(answerButtonMessage2.getSortedActions());

        ButtonEventAdaptorMock answerButtonMessage3 = new ButtonEventAdaptorMock(dieIdButton, 2);
        rerollAnswerHandler.handleComponentInteractEvent(answerButtonMessage3).block();
        expect.scenario("unselect").toMatchSnapshot(answerButtonMessage3.getSortedActions());

        ButtonEventAdaptorMock answerButtonMessage4 = new ButtonEventAdaptorMock(dieIdButton, 2);
        rerollAnswerHandler.handleComponentInteractEvent(answerButtonMessage4).block();
        expect.scenario("reselect").toMatchSnapshot(answerButtonMessage4.getSortedActions());

        String rollIdButton = answer.getComponentRowDefinitions().getLast().getComponentDefinitions().getFirst().getId();
        ButtonEventAdaptorMock answerButtonMessage5 = new ButtonEventAdaptorMock(rollIdButton, 2);
        rerollAnswerHandler.handleComponentInteractEvent(answerButtonMessage5).block();
        expect.scenario("reroll").toMatchSnapshot(answerButtonMessage5.getSortedActions());

        String finishIdButton = answerButtonMessage5.getSendMessages().getFirst().getComponentRowDefinitions().getLast().getComponentDefinitions().getLast().getId();
        ButtonEventAdaptorMock answerButtonMessage6 = new ButtonEventAdaptorMock(finishIdButton, 2);
        rerollAnswerHandler.handleComponentInteractEvent(answerButtonMessage6).block();
        expect.scenario("finish").toMatchSnapshot(answerButtonMessage6.getSortedActions());

    }
}
