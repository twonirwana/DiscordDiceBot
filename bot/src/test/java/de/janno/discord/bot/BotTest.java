package de.janno.discord.bot;

import de.janno.discord.bot.persistance.PersistenceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

class BotTest {

    PersistenceManager persistenceManager;

    @BeforeEach
    void setUp() {
        persistenceManager = Mockito.mock(PersistenceManager.class);
    }

    @Test
    void markAlreadyLeavedGuildsAsDeleted_empty() {
        Bot.markAlreadyLeavedGuildsAsDeleted(persistenceManager, Set.of());

        verifyNoInteractions(persistenceManager);
    }

    @Test
    void markAlreadyLeavedGuildsAsDeleted_toManyGuildsInStartup() {
        Bot.markAlreadyLeavedGuildsAsDeleted(persistenceManager, Set.of(1L));

        verify(persistenceManager, times(0)).markDeleteAllForGuild(any());
    }

    @Test
    void markAlreadyLeavedGuildsAsDeleted_ratioToBig() {
        when(persistenceManager.getAllGuildIds()).thenReturn(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));

        Bot.markAlreadyLeavedGuildsAsDeleted(persistenceManager, Set.of(3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));

        verify(persistenceManager, times(0)).markDeleteAllForGuild(any());
    }

    @Test
    void markAlreadyLeavedGuildsAsDeleted_delete() {
        when(persistenceManager.getAllGuildIds()).thenReturn(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));

        Bot.markAlreadyLeavedGuildsAsDeleted(persistenceManager, Set.of(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));

        verify(persistenceManager, times(1)).markDeleteAllForGuild(List.of(1L));
    }
}