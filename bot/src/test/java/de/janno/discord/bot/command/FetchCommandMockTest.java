package de.janno.discord.bot.command;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@ExtendWith(SnapshotExtension.class)
class FetchCommandMockTest {

    private final UUID uuid0 = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private final UUID uuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private Expect expect;
    private FetchCommand underTest;
    private CustomDiceCommand customDiceCommand;
    private CustomParameterCommand customParameterCommand;
    private SumCustomSetCommand sumCustomSetCommand;
    private PersistenceManager persistenceManager;
    private SlashEventAdaptorMock fetchEvent;

    @BeforeEach
    void setup() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0));
        customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        underTest = new FetchCommand(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand);
        fetchEvent = new SlashEventAdaptorMock(List.of());
    }

    @AfterEach
    void cleanup() {
        io.avaje.config.Config.setProperty("command.delayMessageDataDeletionMs", "10");
        io.avaje.config.Config.setProperty("command.fetch.delayMs", "0");
    }


    @Test
    public void getCommandDefinition() {
        expect.toMatchSnapshot(underTest.getCommandDefinition());
    }

    @Test
    public void getId() {
        expect.toMatchSnapshot(underTest.getCommandId());
    }


    @Test
    void noOldMessage() {
        underTest.handleSlashCommandEvent(fetchEvent, () -> uuid0, Locale.ENGLISH).block();

        expect.toMatchSnapshot(fetchEvent.getSortedActions());
    }

    @Test
    void noOldMessage_fr() {
        underTest.handleSlashCommandEvent(fetchEvent, () -> uuid0, Locale.FRENCH).block();

        expect.toMatchSnapshot(fetchEvent.getSortedActions());
    }

    @Test
    void noOldMessage_pt_BR() {
        underTest.handleSlashCommandEvent(fetchEvent, () -> uuid0, Locale.of("pt", "BR")).block();

        expect.toMatchSnapshot(fetchEvent.getSortedActions());
    }

    @Test
    void noOldMessage_de() {
        underTest.handleSlashCommandEvent(fetchEvent, () -> uuid0, Locale.GERMAN).block();

        expect.toMatchSnapshot(fetchEvent.getSortedActions());
    }


    @Test
    void fetchOldCustomDiceMessage() throws InterruptedException {
        io.avaje.config.Config.setProperty("command.delayMessageDataDeletionMs", "1000");
        io.avaje.config.Config.setProperty("command.fetch.delayMs", "0");

        CustomDiceConfig otherConfig = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "att", "2d20", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        customDiceCommand.createMessageConfig(uuid1, fetchEvent.getGuildId(), fetchEvent.getChannelId(), otherConfig)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        customDiceCommand.createEmptyMessageData(uuid1, fetchEvent.getGuildId(), fetchEvent.getChannelId(), -3L);

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        customDiceCommand.createMessageConfig(uuid0, fetchEvent.getGuildId(), fetchEvent.getChannelId(), config)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        customDiceCommand.createEmptyMessageData(uuid0, fetchEvent.getGuildId(), fetchEvent.getChannelId(), -2L);
        MessageDeletionHelper.deleteMessageDataWithDelay(persistenceManager, fetchEvent.getChannelId(), -2).subscribe();

        customDiceCommand.createEmptyMessageData(uuid0, fetchEvent.getGuildId(), fetchEvent.getChannelId(), -1L);
        Thread.sleep(100);

        underTest.handleSlashCommandEvent(fetchEvent, () -> uuid0, Locale.ENGLISH).block();
        expect.toMatchSnapshot(fetchEvent.getActions());
    }

    @Test
    void fetchOldCustomDiceMessageNotOldEnough() throws InterruptedException {
        io.avaje.config.Config.setProperty("command.fetch.delayMs", "6000000");
        CustomDiceConfig otherConfig = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "att", "2d20", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        customDiceCommand.createMessageConfig(uuid1, fetchEvent.getGuildId(), fetchEvent.getChannelId(), otherConfig)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        customDiceCommand.createEmptyMessageData(uuid1, fetchEvent.getGuildId(), fetchEvent.getChannelId(), -3L);

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        customDiceCommand.createMessageConfig(uuid0, fetchEvent.getGuildId(), fetchEvent.getChannelId(), config)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        customDiceCommand.createEmptyMessageData(uuid0, fetchEvent.getGuildId(), fetchEvent.getChannelId(), -2L);
        MessageDeletionHelper.deleteMessageDataWithDelay(persistenceManager, fetchEvent.getChannelId(), -2).subscribe();

        customDiceCommand.createEmptyMessageData(uuid0, fetchEvent.getGuildId(), fetchEvent.getChannelId(), -1L);

        Thread.sleep(100);
        underTest.handleSlashCommandEvent(fetchEvent, () -> uuid0, Locale.ENGLISH).block();

        expect.toMatchSnapshot(fetchEvent.getActions());
    }

    @Test
    void fetchOldSumCustomSetMessage() throws InterruptedException {
        io.avaje.config.Config.setProperty("command.delayMessageDataDeletionMs", "1000");
        io.avaje.config.Config.setProperty("command.fetch.delayMs", "0");

        SumCustomSetConfig otherConfig = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Att", "+2d20", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+5", false, false, null)), true, true, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        sumCustomSetCommand.createMessageConfig(uuid1, fetchEvent.getGuildId(), fetchEvent.getChannelId(), otherConfig)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        sumCustomSetCommand.createEmptyMessageData(uuid1, fetchEvent.getGuildId(), fetchEvent.getChannelId(), -3L);

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, true, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        sumCustomSetCommand.createMessageConfig(uuid0, fetchEvent.getGuildId(), fetchEvent.getChannelId(), config)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        customDiceCommand.createEmptyMessageData(uuid0, fetchEvent.getGuildId(), fetchEvent.getChannelId(), -2L);
        MessageDeletionHelper.deleteMessageDataWithDelay(persistenceManager, fetchEvent.getChannelId(), -2).subscribe();

        sumCustomSetCommand.createEmptyMessageData(uuid0, fetchEvent.getGuildId(), fetchEvent.getChannelId(), -1L);

        Thread.sleep(100);
        underTest.handleSlashCommandEvent(fetchEvent, () -> uuid0, Locale.ENGLISH).block();

        expect.toMatchSnapshot(fetchEvent.getActions());
    }

    @Test
    void fetchOldCustomParameterMessage() throws InterruptedException {
        io.avaje.config.Config.setProperty("command.delayMessageDataDeletionMs", "1000");
        io.avaje.config.Config.setProperty("command.fetch.delayMs", "0");

        CustomParameterConfig otherConfig = new CustomParameterConfig(null, "{numberOfDice:3<=>6}d{sides:6/8/10/12}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        customParameterCommand.createMessageConfig(uuid1, fetchEvent.getGuildId(), fetchEvent.getChannelId(), otherConfig)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        customParameterCommand.createEmptyMessageData(uuid1, fetchEvent.getGuildId(), fetchEvent.getChannelId(), -3L);

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        customParameterCommand.createMessageConfig(uuid0, fetchEvent.getGuildId(), fetchEvent.getChannelId(), config)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        customParameterCommand.createEmptyMessageData(uuid0, fetchEvent.getGuildId(), fetchEvent.getChannelId(), -2L);
        MessageDeletionHelper.deleteMessageDataWithDelay(persistenceManager, fetchEvent.getChannelId(), -2).subscribe();

        customParameterCommand.createEmptyMessageData(uuid0, fetchEvent.getGuildId(), fetchEvent.getChannelId(), -1L);

        Thread.sleep(100);
        underTest.handleSlashCommandEvent(fetchEvent, () -> uuid0, Locale.ENGLISH).block();

        expect.toMatchSnapshot(fetchEvent.getActions());
    }

}