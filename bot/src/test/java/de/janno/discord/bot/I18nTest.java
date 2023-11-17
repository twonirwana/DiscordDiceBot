package de.janno.discord.bot;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class I18nTest {

    @Test
    void testFallbackToDefault_English() {
        String res = I18n.getMessage("base.option.help", Locale.ENGLISH);
        assertThat(res).isEqualTo("help");
    }

    @Test
    void testFallbackToDefault_Japan() {
        String res = I18n.getMessage("base.option.help", Locale.JAPAN);
        assertThat(res).isEqualTo("help");
    }

    @Test
    void testGermany() {
        String res = I18n.getMessage("base.option.help", Locale.GERMAN);
        assertThat(res).isEqualTo("hilfe");
    }

}