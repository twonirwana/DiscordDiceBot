package de.janno.discord.bot.command.directRoll;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.mock;

@ExtendWith(SnapshotExtension.class)
public class HiddenRollCommandTest {

    HiddenDirectRollCommand underTest;
    private Expect expect;


    @BeforeEach
    void setup() {
        underTest = new HiddenDirectRollCommand(mock(PersistenceManager.class), new CachingDiceEvaluator((minExcl, maxIncl) -> 1, 1000, 0));
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
