package de.janno.discord.connector.cache;

import com.google.common.collect.ImmutableSet;
import de.janno.discord.connector.cache.ButtonMessageCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ButtonMessageCacheTest {

    ButtonMessageCache underTest;

    @BeforeEach
    void setup() {
        underTest = new ButtonMessageCache("test");
    }

    @Test
    void addChannelWithButton() {
        underTest.addChannelWithButton(1, 2, 1);
        underTest.addChannelWithButton(1, 3, 1);

        assertThat(underTest.getCacheContent())
                .hasSize(1)
                .containsEntry(1L,
                        ImmutableSet.of(
                                new ButtonMessageCache.ButtonWithConfigHash(2, 1),
                                new ButtonMessageCache.ButtonWithConfigHash(3, 1))
                );

    }

    @Test
    void removeButtonFromChannel() {
        underTest.addChannelWithButton(1, 2, 1);
        underTest.addChannelWithButton(1, 3, 1);
        underTest.addChannelWithButton(1, 4, 2);


        underTest.removeButtonFromChannel(1, 2, 1);

        assertThat(underTest.getCacheContent())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(
                        new ButtonMessageCache.ButtonWithConfigHash(3, 1),
                        new ButtonMessageCache.ButtonWithConfigHash(4, 2)
                ));
    }

    @Test
    void getAllWithoutOneAndRemoveThem() {
        underTest.addChannelWithButton(1, 2, 1);
        underTest.addChannelWithButton(1, 3, 1);
        underTest.addChannelWithButton(1, 4, 2);


        underTest.getAllWithoutOneAndRemoveThem(1, 3, 1);


        assertThat(underTest.getCacheContent())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(
                        new ButtonMessageCache.ButtonWithConfigHash(3, 1),
                        new ButtonMessageCache.ButtonWithConfigHash(4, 2)
                ));
    }
}