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
        underTest = new QuickstartCommand(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand, channelConfigCommand);
    }

    @Test
    void getCommandId() {
        assertThat(underTest.getCommandId()).isEqualTo("quickstart");
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