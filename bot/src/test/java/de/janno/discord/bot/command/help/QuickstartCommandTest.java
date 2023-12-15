package de.janno.discord.bot.command.help;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
class QuickstartCommandTest {

    QuickstartCommand underTest;
    private Expect expect;

    @BeforeEach
    void setup() {
        PersistenceManager persistenceManager = Mockito.mock(PersistenceManager.class);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0);
        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand);
        underTest = new QuickstartCommand(rpgSystemCommandPreset);
    }

    @Test
    void getCommandId() {
        assertThat(underTest.getCommandId()).isEqualTo("quickstart");
    }

    @ParameterizedTest(name = "{index} config={0}")
    @EnumSource(value = RpgSystemCommandPreset.PresetId.class)
    void handleSlashCommandEvent(RpgSystemCommandPreset.PresetId presetId) {
        SlashEventAdaptorMock slashEventAdaptor = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("system")
                .stringValue(presetId.name())
                .build()));

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH);
        StepVerifier.create(res).verifyComplete();
        expect.scenario(presetId.name()).toMatchSnapshot(slashEventAdaptor.getSortedActions());
    }

    @Test
    void getPresetId_idMatch() {
        Optional<RpgSystemCommandPreset.PresetId> res = QuickstartCommand.getPresetId("DND5", Locale.ENGLISH);

        assertThat(res).contains(RpgSystemCommandPreset.PresetId.DND5);
    }

    @Test
    void getPresetId_nameMatch() {
        Optional<RpgSystemCommandPreset.PresetId> res = QuickstartCommand.getPresetId("Dungeon & dragons 5e ", Locale.ENGLISH);

        assertThat(res).contains(RpgSystemCommandPreset.PresetId.DND5);
    }

    @Test
    void getPresetId_synonymeMatch() {
        Optional<RpgSystemCommandPreset.PresetId> res = QuickstartCommand.getPresetId(" reve de Dragon", Locale.ENGLISH);

        assertThat(res).contains(RpgSystemCommandPreset.PresetId.REVE_DE_DRAGON);
    }

    @Test
    void getPresetId_nameStartsWith() {
        Optional<RpgSystemCommandPreset.PresetId> res = QuickstartCommand.getPresetId(" oWod ", Locale.ENGLISH);

        assertThat(res).contains(RpgSystemCommandPreset.PresetId.OWOD);
    }

    @Test
    void getPresetId_noMatch() {
        Optional<RpgSystemCommandPreset.PresetId> res = QuickstartCommand.getPresetId(" Opus Anima ", Locale.ENGLISH);

        assertThat(res).isEmpty();
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
    void getAutoCompleteAnswer_filter() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("system", "du", List.of()), Locale.ENGLISH);
        assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly("Dungeon & Dragons 5e",
                "Dungeon & Dragons 5e Calculator",
                "Dungeon & Dragons 5e with Dice Images",
                "Dungeon Crawl Classics");
        assertThat(res.stream().map(AutoCompleteAnswer::getValue)).containsExactly("DND5", "DND5_CALC", "DND5_IMAGE", "DUNGEON_CRAWL_CLASSICS");
    }

    @Test
    void getAutoCompleteAnswer_filterGerman() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("system", "du", List.of()), Locale.GERMAN);
        assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly("Dungeon & Dragons 5e",
                "Dungeon & Dragons 5e Kalkulator",
                "Dungeon & Dragons 5e mit Würfelbildern",
                "Dungeon Crawl Classics",
                "nWod / Chronicles of Darkness",
                "oWod / Storyteller System");
        assertThat(res.stream().map(AutoCompleteAnswer::getValue)).containsExactly("DND5", "DND5_CALC", "DND5_IMAGE", "DUNGEON_CRAWL_CLASSICS", "NWOD", "OWOD");
    }

    @Test
    void getAutoCompleteAnswer_filterBrazil() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("system", "vampire", List.of()), Locale.of("PT", "br"));
        assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly("Vampiro 5ed", "nWod / Crônicas das Trevas", "oWod / Sistema Storyteller");
        assertThat(res.stream().map(AutoCompleteAnswer::getValue)).containsExactly("VAMPIRE_5ED", "NWOD", "OWOD");
    }

    @Test
    void getAutoCompleteAnswer_All() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("system", "", List.of()), Locale.ENGLISH);
        assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly("A Song of Ice and Fire",
                "Blades in the Dark",
                "Bluebeard's Bride",
                "Call of Cthulhu 7th Edition",
                "Candela Obscura",
                "City of Mist",
                "Coin Toss",
                "Cyberpunk Red",
                "Dice Calculator",
                "Dungeon & Dragons 5e",
                "Dungeon & Dragons 5e Calculator",
                "Dungeon & Dragons 5e with Dice Images",
                "Dungeon Crawl Classics",
                "Exalted 3ed",
                "Fate",
                "Fate with Dice Images",
                "Hunter 5ed",
                "Kids on Brooms",
                "OSR",
                "One-Roll Engine",
                "Paranoia: Red Clearance Edition",
                "Prowlers & Paragons Ultimate Edition",
                "Public Access",
                "Risus The Anything RPG \"Evens Up\"",
                "Rêve de Dragon",
                "Savage Worlds",
                "Shadowrun",
                "Shadowrun with Dice Images",
                "The Expanse",
                "Tiny D6",
                "Traveller",
                "Vampire 5ed",
                "nWod / Chronicles of Darkness",
                "oWod / Storyteller System");
        assertThat(res.stream().map(AutoCompleteAnswer::getValue)).containsExactly("ASOIAF",
                "BLADES_IN_THE_DARK",
                "BLUEBEARD_BRIDE",
                "CALL_OF_CTHULHU_7ED",
                "CANDELA_OBSCURA",
                "CITY_OF_MIST",
                "COIN",
                "CYBERPUNK_RED",
                "DICE_CALCULATOR",
                "DND5",
                "DND5_CALC",
                "DND5_IMAGE",
                "DUNGEON_CRAWL_CLASSICS",
                "EXALTED_3ED",
                "FATE",
                "FATE_IMAGE",
                "HUNTER_5ED",
                "KIDS_ON_BROOMS",
                "OSR",
                "ONE_ROLL_ENGINE",
                "PARANOIA",
                "PROWLERS_PARAGONS",
                "PUBLIC_ACCESS",
                "RISUS",
                "REVE_DE_DRAGON",
                "SAVAGE_WORLDS",
                "SHADOWRUN",
                "SHADOWRUN_IMAGE",
                "EXPANSE",
                "TINY_D6",
                "TRAVELLER",
                "VAMPIRE_5ED",
                "NWOD",
                "OWOD");
    }
}