package de.janno.discord.cache;

import com.google.common.collect.ImmutableSet;
import discord4j.common.util.Snowflake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveButtonsCacheTest {

    ActiveButtonsCache underTest;

    @BeforeEach
    void setup() {
        underTest = new ActiveButtonsCache("test");
    }

    @Test
    void addChannelWithButton() {
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(2), 1);
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(3), 1);

        assertThat(underTest.getCache().asMap())
                .hasSize(1)
                .containsEntry(Snowflake.of(1),
                        ImmutableSet.of(
                                new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(2), 1),
                                new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(3), 1))
                );

    }

    @Test
    void removeButtonFromChannel() {
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(2), 1);
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(3), 1);
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(4), 2);


        underTest.removeButtonFromChannel(Snowflake.of(1), Snowflake.of(2), 1);

        assertThat(underTest.getCache().asMap())
                .hasSize(1)
                .containsEntry(Snowflake.of(1), ImmutableSet.of(
                        new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(3), 1),
                        new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(4), 2)
                ));
    }

    @Test
    void getAllWithoutOneAndRemoveThem() {
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(2), 1);
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(3), 1);
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(4), 2);


        underTest.getAllWithoutOneAndRemoveThem(Snowflake.of(1), Snowflake.of(3), 1);


        assertThat(underTest.getCache().asMap())
                .hasSize(1)
                .containsEntry(Snowflake.of(1), ImmutableSet.of(
                        new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(3), 1),
                        new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(4), 2)
                ));
    }
}