package de.janno.discord.bot.command.sumCustomSet;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
public class SumCustomSetCommandMockTest {

    PersistenceManager persistenceManager;
    Expect expect;

    @BeforeEach
    void setup() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_fullExpression() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();


        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("click3").toMatchSnapshot(click3.getSortedActions());
    }

    @Test
    void roll_full() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, true, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();


        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("click3").toMatchSnapshot(click3.getSortedActions());
    }

    @Test
    void directRoll() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, true, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", true, false, null)), true, true, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click2).block();


        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_disabled() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, true, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "2+", true, false, null)), true, true, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click1).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
    }

    @Test
    void directRoll_disabled() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "*1d6", false, true, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "2*", true, false, null)), true, true, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click1).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
    }

    @Test
    void slash_start_multiLine() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("d[a\\nb\\nc,\\nd,e\\n];1d20@\\nAttack\\nDown\\n;3d10,3d10,3d10")
                        .build())
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent.getSortedActions());

        Optional<ButtonEventAdaptorMock> buttonEventAdaptorMock = slashEvent.getFirstButtonEventMockOfLastButtonMessage();
        assertThat(buttonEventAdaptorMock).isPresent();
        underTest.handleComponentInteractEvent(buttonEventAdaptorMock.get()).block();
        expect.scenario("event2").toMatchSnapshot(buttonEventAdaptorMock.get().getSortedActions());
    }

    @Test
    void slash_start_directRoll() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("+1d10@!d10;+1@!;+3;+d6@d6")
                        .build())
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent.getSortedActions());

        Optional<ButtonEventAdaptorMock> buttonEventAdaptorMock = slashEvent.getFirstButtonEventMockOfLastButtonMessage();
        assertThat(buttonEventAdaptorMock).isPresent();
        underTest.handleComponentInteractEvent(buttonEventAdaptorMock.get()).block();
        expect.scenario("event2").toMatchSnapshot(buttonEventAdaptorMock.get().getSortedActions());
    }


    @Test
    void roll_fullGerman() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.GERMAN, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("click3").toMatchSnapshot(click3.getSortedActions());
    }

    @Test
    void roll_full_ptBR() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.of("pt", "BR"), null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("click3").toMatchSnapshot(click3.getSortedActions());
    }

    @Test
    void roll_compact() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, false, false, null, null, AnswerFormatType.compact, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("click3").toMatchSnapshot(click3.getSortedActions());
    }

    @Test
    void roll_minimal() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, false, false, null, null, AnswerFormatType.minimal, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("click3").toMatchSnapshot(click3.getSortedActions());
    }

    @Test
    void roll_locked() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, false, false, null, null, AnswerFormatType.minimal, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button", "user2");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click4).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("click3").toMatchSnapshot(click3.getSortedActions());
        expect.scenario("click4").toMatchSnapshot(click4.getSortedActions());
    }

    @Test
    void clear() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("clear");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void backBack() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("back");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("back");
        underTest.handleComponentInteractEvent(click3).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_pinned() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("click3").toMatchSnapshot(click3.getSortedActions());
    }

    @Test
    void roll_answerChannel() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("click3").toMatchSnapshot(click3.getSortedActions());
    }

    @Test
    void roll_pinnedTwice() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, true);

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

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("click3").toMatchSnapshot(click3.getSortedActions());
        expect.scenario("click4").toMatchSnapshot(click4.getSortedActions());
        expect.scenario("click5").toMatchSnapshot(click5.getSortedActions());
    }

    @Test
    void roll_answerChannelTwice() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        SumCustomSetConfig config = new SumCustomSetConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6", false, false, null),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2", false, false, null)), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);

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

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("click3").toMatchSnapshot(click3.getSortedActions());
        expect.scenario("click4").toMatchSnapshot(click4.getSortedActions());
        expect.scenario("click5").toMatchSnapshot(click5.getSortedActions());
    }

    @Test
    void channelAlias() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "att", false, false, null)), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void userChannelAlias() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "att", false, false, null)), true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateDataV2> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_set", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
    }
}
