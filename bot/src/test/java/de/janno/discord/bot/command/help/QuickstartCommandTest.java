package de.janno.discord.bot.command.help;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
class QuickstartCommandTest {

    QuickstartCommand underTest;

    Expect expect;

    @BeforeEach
    void setup() {
        PersistenceManager persistenceManager = Mockito.mock(PersistenceManager.class);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0));
        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        ChannelConfigCommand channelConfigCommand = new ChannelConfigCommand(persistenceManager);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand, channelConfigCommand);
        underTest = new QuickstartCommand(rpgSystemCommandPreset);
    }

    @Test
    void getCommandId() {
        assertThat(underTest.getCommandId()).isEqualTo("quickstart");
    }

    @Test
    void getPresetId_idMatch() {
        Optional<RpgSystemCommandPreset.PresetId> res = QuickstartCommand.getPresetId("DND5", Locale.ENGLISH);

        assertThat(res).contains(RpgSystemCommandPreset.PresetId.DND5);
    }

    @Test
    void getPresetId_nameMatch() {
        Optional<RpgSystemCommandPreset.PresetId> res = QuickstartCommand.getPresetId("Dungeon & dragons 5e ", Locale.ENGLISH);

        assertThat(res).contains(RpgSystemCommandPreset.PresetId.DND5_IMAGE);
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
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("system", "du", List.of()), Locale.ENGLISH, 1L, 1L);
        SoftAssertions.assertSoftly(a -> {
            a.assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly(
                    "Dungeon & Dragons 5e",
                    "Dungeon & Dragons 5e Calculator",
                    "Dungeon & Dragons 5e Calculator 2",
                    "Dungeon & Dragons 5e without Dice Images",
                    "Dungeon Crawl Classics",
                    "Powered by the Apocalypse");
            a.assertThat(res.stream().map(AutoCompleteAnswer::getValue)).containsExactly(
                    "DND5_IMAGE",
                    "DND5_CALC",
                    "DND5_CALC2",
                    "DND5",
                    "DUNGEON_CRAWL_CLASSICS",
                    "PBTA");
        });

    }

    @Test
    void getAutoCompleteAnswer_filterGerman() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("system", "du", List.of()), Locale.GERMAN, 1L, 1L);
        SoftAssertions.assertSoftly(a -> {
            a.assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly(
                    "Dungeon & Dragons 5e",
                    "Dungeon & Dragons 5e Calculator 2",
                    "Dungeon & Dragons 5e Kalkulator",
                    "Dungeon & Dragons 5e ohne Würfelbildern",
                    "Dungeon Crawl Classics",
                    "Powered by the Apocalypse",
                    "nWod / Chronicles of Darkness",
                    "oWod / Storyteller System");
            a.assertThat(res.stream().map(AutoCompleteAnswer::getValue)).containsExactly(
                    "DND5_IMAGE",
                    "DND5_CALC2",
                    "DND5_CALC",
                    "DND5",
                    "DUNGEON_CRAWL_CLASSICS",
                    "PBTA",
                    "NWOD",
                    "OWOD");
        });
    }

    @Test
    void getAutoCompleteAnswer_filterBrazil() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("system", "vampire", List.of()), Locale.of("PT", "br"), 1L, 1L);
        assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly("Vampiro 5ed", "nWod / Crônicas das Trevas", "oWod / Sistema Storyteller");
        assertThat(res.stream().map(AutoCompleteAnswer::getValue)).containsExactly("VAMPIRE_5ED", "NWOD", "OWOD");
    }

    @Test
    void getAutoCompleteAnswer_All() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("system", "", List.of()), Locale.ENGLISH, 1L, 1L);
        SoftAssertions.assertSoftly(a -> {
            a.assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly(
                    "A Song of Ice and Fire",
                    "Blades in the Dark",
                    "Blades in the Dark - Detail",
                    "Blades in the Dark without Dice Images",
                    "Bluebeard's Bride",
                    "Call of Cthulhu 7th Edition",
                    "Candela Obscura",
                    "City of Mist",
                    "Coin Toss",
                    "Cyberpunk Red",
                    "Dice Calculator",
                    "Dungeon & Dragons 5e",
                    "Dungeon & Dragons 5e Calculator",
                    "Dungeon & Dragons 5e Calculator 2",
                    "Dungeon & Dragons 5e without Dice Images",
                    "Dungeon Crawl Classics",
                    "EZD6",
                    "Exalted 3ed",
                    "Fate",
                    "Fate Alias",
                    "Fate without Dice Images",
                    "Heroes of Cerulea",
                    "Hunter 5ed",
                    "Ironsworn",
                    "Kids on Brooms",
                    "OSR",
                    "Oathsworn",
                    "One-Roll Engine",
                    "Paranoia: Red Clearance Edition",
                    "Powered by the Apocalypse",
                    "Prowlers & Paragons Ultimate Edition",
                    "Public Access",
                    "Rebellion Unplugged",
                    "Risus The Anything RPG \"Evens Up\"",
                    "Rêve de Dragon",
                    "Savage Worlds",
                    "Shadowrun",
                    "Shadowrun without Dice Images",
                    "Star Wars - West End Games D6 Rules, 2nd Edition REUP",
                    "The Expanse",
                    "The Marvel Multiverse Role-Playing Game",
                    "The One Ring",
                    "Tiny D6",
                    "Traveller",
                    "Vampire 5ed",
                    "Year Zero Engine: Alien",
                    "nWod / Chronicles of Darkness",
                    "oWod / Storyteller System");
            a.assertThat(res.stream().map(AutoCompleteAnswer::getValue)).containsExactly(
                    "ASOIAF",
                    "BLADES_IN_THE_DARK_IMAGE",
                    "BLADES_IN_THE_DARK_DETAIL",
                    "BLADES_IN_THE_DARK",
                    "BLUEBEARD_BRIDE",
                    "CALL_OF_CTHULHU_7ED",
                    "CANDELA_OBSCURA",
                    "CITY_OF_MIST",
                    "COIN",
                    "CYBERPUNK_RED",
                    "DICE_CALCULATOR",
                    "DND5_IMAGE",
                    "DND5_CALC",
                    "DND5_CALC2",
                    "DND5",
                    "DUNGEON_CRAWL_CLASSICS",
                    "EZD6",
                    "EXALTED_3ED",
                    "FATE_IMAGE",
                    "FATE_ALIAS",
                    "FATE",
                    "HEROES_OF_CERULEA",
                    "HUNTER_5ED",
                    "IRONSWORN",
                    "KIDS_ON_BROOMS",
                    "OSR",
                    "OATHSWORN",
                    "ONE_ROLL_ENGINE",
                    "PARANOIA",
                    "PBTA",
                    "PROWLERS_PARAGONS",
                    "PUBLIC_ACCESS",
                    "REBELLION_UNPLUGGED",
                    "RISUS",
                    "REVE_DE_DRAGON",
                    "SAVAGE_WORLDS",
                    "SHADOWRUN_IMAGE",
                    "SHADOWRUN",
                    "STAR_WARS_D6",
                    "EXPANSE",
                    "MARVEL",
                    "THE_ONE_RING",
                    "TINY_D6",
                    "TRAVELLER",
                    "VAMPIRE_5ED",
                    "ALIEN",
                    "NWOD",
                    "OWOD");
        });
    }
}