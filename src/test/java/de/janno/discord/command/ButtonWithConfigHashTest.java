package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import discord4j.common.util.Snowflake;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ButtonWithConfigHashTest {

    @Test
    void testEquals_true() {
        ButtonWithConfigHash buttonWithConfigHash1 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ButtonWithConfigHash buttonWithConfigHash2 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.equals(buttonWithConfigHash2)).isTrue();
    }

    @Test
    void testEquals_false_id() {
        ButtonWithConfigHash buttonWithConfigHash1 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ButtonWithConfigHash buttonWithConfigHash2 = new ButtonWithConfigHash(Snowflake.of(2), ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.equals(buttonWithConfigHash2)).isFalse();
    }

    @Test
    void testEquals_false_configHash() {
        ButtonWithConfigHash buttonWithConfigHash1 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ButtonWithConfigHash buttonWithConfigHash2 = new ButtonWithConfigHash(Snowflake.of(2), ImmutableList.of("test2").hashCode());
        assertThat(buttonWithConfigHash1.equals(buttonWithConfigHash2)).isFalse();
    }

    @Test
    void testHash_true() {
        ButtonWithConfigHash buttonWithConfigHash1 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ButtonWithConfigHash buttonWithConfigHash2 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.hashCode()).isEqualTo(buttonWithConfigHash2.hashCode());
    }

    @Test
    void testHash_false_id() {
        ButtonWithConfigHash buttonWithConfigHash1 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ButtonWithConfigHash buttonWithConfigHash2 = new ButtonWithConfigHash(Snowflake.of(2), ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.hashCode()).isNotEqualTo(buttonWithConfigHash2.hashCode());
    }

    @Test
    void testHash_false_configHash() {
        ButtonWithConfigHash buttonWithConfigHash1 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ButtonWithConfigHash buttonWithConfigHash2 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test2").hashCode());
        assertThat(buttonWithConfigHash1.hashCode()).isNotEqualTo(buttonWithConfigHash2.hashCode());
    }

    @Test
    void compareEqual() {
        ButtonWithConfigHash buttonWithConfigHash1 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ButtonWithConfigHash buttonWithConfigHash2 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.compareTo(buttonWithConfigHash2)).isEqualTo(0);
    }

    @Test
    void compareNotEqual_id() {
        ButtonWithConfigHash buttonWithConfigHash1 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ButtonWithConfigHash buttonWithConfigHash2 = new ButtonWithConfigHash(Snowflake.of(2), ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.compareTo(buttonWithConfigHash2)).isEqualTo(-1);
    }

    @Test
    void compareNotEqual_hash() {
        ButtonWithConfigHash buttonWithConfigHash1 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test").hashCode());
        ButtonWithConfigHash buttonWithConfigHash2 = new ButtonWithConfigHash(Snowflake.of(1), ImmutableList.of("test2").hashCode());
        assertThat(buttonWithConfigHash1.compareTo(buttonWithConfigHash2)).isEqualTo(-1);
    }
}