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
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("system", "du", List.of()), Locale.ENGLISH, 1L, 1L, 0L);
        expect.toMatchSnapshot(res);
    }

    @Test
    void getAutoCompleteAnswer_filterGerman() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("system", "du", List.of()), Locale.GERMAN, 1L, 1L, 0L);
        expect.toMatchSnapshot(res);
    }

    @Test
    void getAutoCompleteAnswer_filterBrazil() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("system", "vampire", List.of()), Locale.of("PT", "br"), 1L, 1L, 0L);
        expect.toMatchSnapshot(res);
    }

    @Test
    void getAutoCompleteAnswer_all() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("system", "", List.of()), Locale.ENGLISH, 1L, 1L, 0L);
        expect.toMatchSnapshot(res);
    }
}