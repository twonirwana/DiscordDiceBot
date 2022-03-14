package de.janno.discord.bot.cache;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.cache.ButtonMessageCache;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ButtonWithConfigHashTest {

    @Test
    void testEquals_true() {
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash1 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test").hashCode());
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash2 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.equals(buttonWithConfigHash2)).isTrue();
    }

    @Test
    void testEquals_false_id() {
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash1 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test").hashCode());
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash2 = new ButtonMessageCache.ButtonWithConfigHash(2, ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.equals(buttonWithConfigHash2)).isFalse();
    }

    @Test
    void testEquals_false_configHash() {
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash1 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test").hashCode());
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash2 = new ButtonMessageCache.ButtonWithConfigHash(2, ImmutableList.of("test2").hashCode());
        assertThat(buttonWithConfigHash1.equals(buttonWithConfigHash2)).isFalse();
    }

    @Test
    void testHash_true() {
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash1 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test").hashCode());
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash2 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.hashCode()).isEqualTo(buttonWithConfigHash2.hashCode());
    }

    @Test
    void testHash_false_id() {
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash1 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test").hashCode());
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash2 = new ButtonMessageCache.ButtonWithConfigHash(2, ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.hashCode()).isNotEqualTo(buttonWithConfigHash2.hashCode());
    }

    @Test
    void testHash_false_configHash() {
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash1 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test").hashCode());
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash2 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test2").hashCode());
        assertThat(buttonWithConfigHash1.hashCode()).isNotEqualTo(buttonWithConfigHash2.hashCode());
    }

    @Test
    void compareEqual() {
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash1 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test").hashCode());
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash2 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.compareTo(buttonWithConfigHash2)).isEqualTo(0);
    }

    @Test
    void compareNotEqual_id() {
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash1 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test").hashCode());
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash2 = new ButtonMessageCache.ButtonWithConfigHash(2, ImmutableList.of("test").hashCode());
        assertThat(buttonWithConfigHash1.compareTo(buttonWithConfigHash2)).isEqualTo(-1);
    }

    @Test
    void compareNotEqual_hash() {
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash1 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test").hashCode());
        ButtonMessageCache.ButtonWithConfigHash buttonWithConfigHash2 = new ButtonMessageCache.ButtonWithConfigHash(1, ImmutableList.of("test2").hashCode());
        assertThat(buttonWithConfigHash1.compareTo(buttonWithConfigHash2)).isEqualTo(-1);
    }
}