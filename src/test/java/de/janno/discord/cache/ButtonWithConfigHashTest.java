package de.janno.discord.cache;

import com.google.common.collect.ImmutableList;
import de.janno.discord.cache.ActiveButtonsCache;
import discord4j.common.util.Snowflake;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ButtonWithConfigHashTest {

    @Test
    void testEquals_true() {
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash1 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash2 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.equals(buttonWithConfigHash2)).isTrue();
    }

    @Test
    void testEquals_false_id() {
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash1 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash2 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(2), ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.equals(buttonWithConfigHash2)).isFalse();
    }

    @Test
    void testEquals_false_configHash() {
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash1 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash2 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(2), ImmutableList.of("test2").hashCode());
        assertThat(buttonWithConfigHash1.equals(buttonWithConfigHash2)).isFalse();
    }

    @Test
    void testHash_true() {
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash1 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash2 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.hashCode()).isEqualTo(buttonWithConfigHash2.hashCode());
    }

    @Test
    void testHash_false_id() {
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash1 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash2 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(2), ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.hashCode()).isNotEqualTo(buttonWithConfigHash2.hashCode());
    }

    @Test
    void testHash_false_configHash() {
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash1 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash2 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test2").hashCode());
        assertThat(buttonWithConfigHash1.hashCode()).isNotEqualTo(buttonWithConfigHash2.hashCode());
    }

    @Test
    void compareEqual() {
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash1 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash2 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.compareTo(buttonWithConfigHash2)).isEqualTo(0);
    }

    @Test
    void compareNotEqual_id() {
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash1 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash2 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(2), ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.compareTo(buttonWithConfigHash2)).isEqualTo(-1);
    }

    @Test
    void compareNotEqual_hash() {
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash1 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ActiveButtonsCache.ButtonWithConfigHash buttonWithConfigHash2 = new ActiveButtonsCache.ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test2").hashCode());
        assertThat(buttonWithConfigHash1.compareTo(buttonWithConfigHash2)).isEqualTo(-1);
    }
}