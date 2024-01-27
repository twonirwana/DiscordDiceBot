package de.janno.discord.bot.command;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static shadow.org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
class CallCommandMockTest {

    private final UUID uuid0 = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private final UUID uuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private Expect expect;
    private CallCommand underTest;
    private CustomDiceCommand customDiceCommand;
    private CustomParameterCommand customParameterCommand;
    private SumCustomSetCommand sumCustomSetCommand;
    private PersistenceManager persistenceManager;
    private SlashEventAdaptorMock callEvent;

    @BeforeEach
    void setup() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0);
        customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        underTest = new CallCommand(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand);
        callEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("call")
                .build()));
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
        underTest.handleSlashCommandEvent(callEvent, () -> uuid0, Locale.ENGLISH).block();

        assertThat(callEvent.getActions()).containsExactlyInAnyOrder(
                "reply: No old button message found"
        );
    }

    @Test
    void noOldMessage_fr() {
        underTest.handleSlashCommandEvent(callEvent, () -> uuid0, Locale.FRENCH).block();

        assertThat(callEvent.getActions()).containsExactlyInAnyOrder(
                "reply: Aucun ancien message de bouton trouvé"
        );
    }

    @Test
    void noOldMessage_pt_BR() {
        underTest.handleSlashCommandEvent(callEvent, () -> uuid0, Locale.of("pt", "BR")).block();

        assertThat(callEvent.getActions()).containsExactlyInAnyOrder(
                "reply: Nenhuma mensagem antiga do botão foi encontrada"
        );
    }

    @Test
    void noOldMessage_de() {
        underTest.handleSlashCommandEvent(callEvent, () -> uuid0, Locale.GERMAN).block();

        assertThat(callEvent.getActions()).containsExactlyInAnyOrder(
                "reply: Es wurde keine alte Würfel-Nachricht gefunden"
        );
    }


    @Test
    void callOldCustomDiceMessage() {
        CustomDiceConfig otherConfig = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "att", "2d20")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        customDiceCommand.createMessageConfig(uuid1, callEvent.getGuildId(), callEvent.getChannelId(), otherConfig)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        customDiceCommand.createEmptyMessageData(uuid1, callEvent.getGuildId(), callEvent.getChannelId(), -3L);

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        customDiceCommand.createMessageConfig(uuid0, callEvent.getGuildId(), callEvent.getChannelId(), config)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        customDiceCommand.createEmptyMessageData(uuid0, callEvent.getGuildId(), callEvent.getChannelId(), -2L);
        customDiceCommand.createEmptyMessageData(uuid0, callEvent.getGuildId(), callEvent.getChannelId(), -1L);
        underTest.handleSlashCommandEvent(callEvent, () -> uuid0, Locale.ENGLISH).block();

        expect.toMatchSnapshot(callEvent.getActions());
    }

    @Test
    void callOldSumCustomSetMessage() {
        SumCustomSetConfig otherConfig = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Att", "+2d20"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+5")), DiceParserSystem.DICE_EVALUATOR, true, true, false, null, null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        sumCustomSetCommand.createMessageConfig(uuid1, callEvent.getGuildId(), callEvent.getChannelId(), otherConfig)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        sumCustomSetCommand.createEmptyMessageData(uuid1, callEvent.getGuildId(), callEvent.getChannelId(), -3L);

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, true, false, null, null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        sumCustomSetCommand.createMessageConfig(uuid0, callEvent.getGuildId(), callEvent.getChannelId(), config)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        sumCustomSetCommand.createEmptyMessageData(uuid0, callEvent.getGuildId(), callEvent.getChannelId(), -2L);
        sumCustomSetCommand.createEmptyMessageData(uuid0, callEvent.getGuildId(), callEvent.getChannelId(), -1L);
        underTest.handleSlashCommandEvent(callEvent, () -> uuid0, Locale.ENGLISH).block();

        expect.toMatchSnapshot(callEvent.getActions());
    }

    @Test
    void callOldCustomParameterMessage() {
        CustomParameterConfig otherConfig = new CustomParameterConfig(null, "{numberOfDice:3<=>6}d{sides:6/8/10/12}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        customParameterCommand.createMessageConfig(uuid1, callEvent.getGuildId(), callEvent.getChannelId(), otherConfig)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        customParameterCommand.createEmptyMessageData(uuid1, callEvent.getGuildId(), callEvent.getChannelId(), -3L);

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        customParameterCommand.createMessageConfig(uuid0, callEvent.getGuildId(), callEvent.getChannelId(), config)
                .ifPresent(m -> persistenceManager.saveMessageConfig(m));
        customParameterCommand.createEmptyMessageData(uuid0, callEvent.getGuildId(), callEvent.getChannelId(), -2L);
        customParameterCommand.createEmptyMessageData(uuid0, callEvent.getGuildId(), callEvent.getChannelId(), -1L);
        underTest.handleSlashCommandEvent(callEvent, () -> uuid0, Locale.ENGLISH).block();

        expect.toMatchSnapshot(callEvent.getActions());
    }

}