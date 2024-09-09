package de.janno.discord.bot.command.customParameter;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.AnswerFormatType;
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
public class CustomParameterCommandMockTest {

    PersistenceManager persistenceManager;
    Expect expect;

    @BeforeEach
    void setup() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_full() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_pathPool() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "/custom_parameter start expression: val('$n',{numberOfDice:1<=>10}),{type:''!1!@pool/''!2!@sum} + '$n'd10{target!1!:>=4c@4/>=6c@6/>=8c@8}{modifier!2!:+1=@+1/+2=@+2/+3=@+3}",
                AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id5");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id1");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("id2");
        underTest.handleComponentInteractEvent(click3).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("event3").toMatchSnapshot(click3.getSortedActions());
    }

    @Test
    void roll_pathSum() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "/custom_parameter start expression: val('$n',{numberOfDice:1<=>10}),{type:''!1!@pool/''!2!@sum} + '$n'd10{target!1!:>=4c@4/>=6c@6/>=8c@8}{modifier!2!:+1=@+1/+2=@+2/+3=@+3}",
                AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id5");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id2");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("id2");
        underTest.handleComponentInteractEvent(click3).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("event3").toMatchSnapshot(click3.getSortedActions());
    }

    @Test
    void roll_direct() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{n}d{s:4/6/10/20@!20}+{modi:1/2/3}=", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id1");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_ignore_direct() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{n}d{s:4/6/10/20@!20}+{modi:1/2/3}=", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id1");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("id1");
        underTest.handleComponentInteractEvent(click3).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("event3").toMatchSnapshot(click3.getSortedActions());
    }

    @Test
    void slash_start_multiLine() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("expression")
                        .stringValue("d[a\\nb\\nc,\\nd,e\\n]+{bonus:0@None/3@Small\\nBonus/5@\\nBig\\nBonus\\n}")
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
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(
                (min, max) -> max)
        );

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("expression")
                        .stringValue("{n:1d20@!d20/1d6/2d8}+{b:1<=>5}=")
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
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.GERMAN);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_full_ptBR() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.of("pt", "BR"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_textLegacy() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{dice:d4/d['-','0','1']@Fate}+{bonus:1<=>10}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("d['-','0','1']");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_textLegacy2() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{bonus:-5<=>5}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("2");
        underTest.handleComponentInteractEvent(click1).block();

        expect.toMatchSnapshot(click1.getSortedActions());
    }

    @Test
    void roll_text() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{dice:d4/d['-','0','1']@Fate}+{bonus:1<=>10}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id2");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_full_withLabel() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}@Roll", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_withoutExpression() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_withoutExpression_withLabel() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}@Roll", AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_compact() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.compact, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_minimal() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.minimal, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void lockedToUser_block() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.minimal, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4", "user1");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3", "user2");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("id3", "user1");
        underTest.handleComponentInteractEvent(click3).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("event3").toMatchSnapshot(click3.getSortedActions());
    }

    @Test
    void clear() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("clear");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_pinned() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_answerChannel() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(2L, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void roll_pinnedTwice() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click4).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("event3").toMatchSnapshot(click3.getSortedActions());
        expect.scenario("event4").toMatchSnapshot(click4.getSortedActions());
    }

    @Test
    void roll_answerChannelTwice() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomParameterConfig config = new CustomParameterConfig(2L, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click4).block();

        expect.scenario("event1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(click2.getSortedActions());
        expect.scenario("event3").toMatchSnapshot(click3.getSortedActions());
        expect.scenario("event4").toMatchSnapshot(click4.getSortedActions());
    }

    @Test
    void channelAlias() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}+att", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);

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


        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
    }

    @Test
    void userChannelAlias() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}+att", AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);

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


        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        expect.scenario("click1").toMatchSnapshot(click1.getSortedActions());
        expect.scenario("click2").toMatchSnapshot(click2.getSortedActions());
    }
}
