package de.janno.discord.bot.command.directRoll;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

@ExtendWith(SnapshotExtension.class)
public class HiddenRollCommandTest {

    HiddenDirectRollCommand underTest;
    private Expect expect;

    static Stream<Arguments> generateAllLocaleData() {
        return I18n.allSupportedLanguage().stream()
                .map(Arguments::of);
    }

    @BeforeEach
    void setup() {
        underTest = new HiddenDirectRollCommand(mock(PersistenceManager.class), new CachingDiceEvaluator((minExcl, maxIncl) -> 1));
    }

    @ParameterizedTest(name = "{index} locale={0}")
    @MethodSource("generateAllLocaleData")
    void testHelp(Locale userLocale) {
        expect.scenario(userLocale.toString()).toMatchSnapshot(underTest.getHelpMessage(userLocale));
    }

    @Test
    public void getCommandDefinition() {
        expect.toMatchSnapshot(underTest.getCommandDefinition());
    }

    @Test
    public void getId() {
        expect.toMatchSnapshot(underTest.getCommandId());
    }
}
