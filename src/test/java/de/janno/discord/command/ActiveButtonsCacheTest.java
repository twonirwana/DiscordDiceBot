package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
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
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(2), ImmutableList.of("test", "test2"));
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(3), ImmutableList.of("test", "test2"));

        assertThat(underTest.getCache().asMap())
                .hasSize(1)
                .containsEntry(Snowflake.of(1),
                        ImmutableSet.of(
                                new ButtonWithConfigHash(Snowflake.of(2), ImmutableList.of("test", "test2").hashCode()),
                                new ButtonWithConfigHash(Snowflake.of(3), ImmutableList.of("test", "test2").hashCode()))
                );

    }

    @Test
    void removeButtonFromChannel() {
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(2), ImmutableList.of("test", "test2"));
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(3), ImmutableList.of("test", "test2"));
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(4), ImmutableList.of("test2", "test"));


        underTest.removeButtonFromChannel(Snowflake.of(1), Snowflake.of(2), ImmutableList.of("test", "test2"));

        assertThat(underTest.getCache().asMap())
                .hasSize(1)
                .containsEntry(Snowflake.of(1), ImmutableSet.of(
                        new ButtonWithConfigHash(Snowflake.of(3), ImmutableList.of("test", "test2").hashCode()),
                        new ButtonWithConfigHash(Snowflake.of(4), ImmutableList.of("test2", "test").hashCode())
                ));
    }

    @Test
    void getAllWithoutOneAndRemoveThem() {
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(2), ImmutableList.of("test", "test2"));
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(3), ImmutableList.of("test", "test2"));
        underTest.addChannelWithButton(Snowflake.of(1), Snowflake.of(4), ImmutableList.of("test2", "test"));


        underTest.getAllWithoutOneAndRemoveThem(Snowflake.of(1), Snowflake.of(3), ImmutableList.of("test", "test2"));


        assertThat(underTest.getCache().asMap())
                .hasSize(1)
                .containsEntry(Snowflake.of(1), ImmutableSet.of(
                        new ButtonWithConfigHash(Snowflake.of(3), ImmutableList.of("test", "test2").hashCode()),
                        new ButtonWithConfigHash(Snowflake.of(4), ImmutableList.of("test2", "test").hashCode())
                ));
    }
}