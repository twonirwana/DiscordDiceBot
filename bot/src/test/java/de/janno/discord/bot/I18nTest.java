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

    @Test
    void testBrazilian() {
        String res = I18n.getMessage("base.option.help", Locale.of("pt", "BR"));
        assertThat(res).isEqualTo("ajuda");
    }

    @Test
    void testPortuguese() {
        String res = I18n.getMessage("base.option.help", Locale.of("pt"));
        //pt is currently not the fallback for pt_BR
        assertThat(res).isEqualTo("help");
    }

    @Test
    void testNewLine() {
        String res = I18n.getMessage("channel_config.save.reply", Locale.ENGLISH, "test");
        assertThat(res).isEqualTo("`test`\nSaved direct roll channel config");
    }

    @Test
    void testFrench() {
        String res = I18n.getMessage("base.option.help", Locale.FRENCH);
        assertThat(res).isEqualTo("aide");
    }

    @Test
    void testFrench_Apostrophe() {
        String res = I18n.getMessage("custom_parameter.description", Locale.FRENCH);
        assertThat(res).isEqualTo("Remplir les paramètres personnalisés d'une expression cubique donnée");
    }

    @Test
    void testFrenchPlaceholderAndApostrophe() {
        String res = I18n.getMessage("base.help.description", Locale.FRENCH, "custom_dice");
        assertThat(res).isEqualTo("Obtenir de l'aide pour /custom_dice");
    }
}