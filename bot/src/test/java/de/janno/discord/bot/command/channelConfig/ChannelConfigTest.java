package de.janno.discord.bot.command.channelConfig;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.persistance.PersistenceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.mock;

@ExtendWith(SnapshotExtension.class)
public class ChannelConfigTest {
    ChannelConfigCommand underTest;
    Expect expect;


    @BeforeEach
    void setup() {
        underTest = new ChannelConfigCommand(mock(PersistenceManager.class));
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
