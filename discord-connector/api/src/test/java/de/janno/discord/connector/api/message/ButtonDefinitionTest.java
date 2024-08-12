package de.janno.discord.connector.api.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ButtonDefinitionTest {

    @Test
    void testIdLength_builder() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ButtonDefinition.builder()
                .label("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .id("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").build());

        assertThat(exception.getMessage()).isEqualTo("ID 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' is to long");

    }

    @Test
    void testIdEmpty() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ButtonDefinition.builder()
                .id("").label("test").build());

        assertThat(exception.getMessage()).isEqualTo("id is empty");

    }

    @Test
    void testLabelLength_builder() {
        ButtonDefinition underTest = ButtonDefinition.builder()
                .label("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .id("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").build();

        assertThat(underTest.getLabel()).isEqualTo("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb...");
    }

    @Test
    void testLabelOrEmoji_fail() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ButtonDefinition.builder()
                .label("")
                .emoji(null)
                .id("a").build());

        assertThat(exception.getMessage()).isEqualTo("label and emoji are empty");
    }

    @Test
    void testLabelOrEmoji_label() {
        ButtonDefinition underTest = ButtonDefinition.builder()
                .label("test")
                .emoji(null)
                .id("a").build();

        assertThat(underTest.toString()).isEqualTo("ButtonDefinition(label=test, id=a, style=PRIMARY, disabled=false, emoji=null)");
    }

    @Test
    void testLabelOrEmoji_emoji() {
        ButtonDefinition underTest = ButtonDefinition.builder()
                .label("")
                .emoji("\uD83D\uDE00")
                .id("a").build();

        assertThat(underTest).isEqualTo(new ButtonDefinition("", "a", ButtonDefinition.Style.PRIMARY, false, "\uD83D\uDE00"));
    }

    @Test
    void testLabelOrEmoji_discordEmoji() {
        ButtonDefinition underTest = ButtonDefinition.builder()
                .label("")
                .emoji("<:calculator:1>")
                .id("a").build();

        assertThat(underTest.toString()).isEqualTo("ButtonDefinition(label=, id=a, style=PRIMARY, disabled=false, emoji=<:calculator:1>)");
    }

    @Test
    void testEmoji_invalidUnicode() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ButtonDefinition.builder()
                .label("")
                .emoji("test")
                .id("a").build());

        assertThat(exception.getMessage()).isEqualTo("invalid emoji: test");
    }
    @Test
    void testEmoji_invalidDiscordEmoji() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ButtonDefinition.builder()
                .label("")
                .emoji("<:calculator:1> a")
                .id("a").build());

        assertThat(exception.getMessage()).isEqualTo("invalid emoji: <:calculator:1> a");
    }

}