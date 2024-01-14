package de.janno.discord.bot;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class I18nTest {

    @Test
    void testFallbackToDefault_English() {
        String res = I18n.getMessage("base.help.description", Locale.ENGLISH);
        assertThat(res).isEqualTo("Get help for /{0}");
    }

    @Test
    void testFallbackToKey() {
        String res = I18n.getMessage("path.property.name", Locale.ENGLISH);
        assertThat(res).isEqualTo("name");
    }

    @Test
    void testFallbackToKeyWithoutDot() {
        String res = I18n.getMessage("property", Locale.ENGLISH);
        assertThat(res).isEqualTo("property");
    }

    @Test
    void testFallbackToDefault_Japan() {
        String res = I18n.getMessage("base.help.description", Locale.JAPAN);
        assertThat(res).isEqualTo("Get help for /{0}");
    }

    @Test
    void testGermany() {
        String res = I18n.getMessage("base.help.description", Locale.GERMAN);
        assertThat(res).isEqualTo("Hilfe für /{0}");
    }

    @Test
    void testBrazilian() {
        String res = I18n.getMessage("base.help.description", Locale.of("pt", "BR"));
        assertThat(res).isEqualTo("Consiga ajuda para /{0}");
    }

    @Test
    void testPortuguese() {
        String res = I18n.getMessage("base.help.description", Locale.of("pt"));
        //pt is currently not the fallback for pt_BR
        assertThat(res).isEqualTo("Get help for /{0}");
    }

    @Test
    void testNewLine() {
        String res = I18n.getMessage("channel_config.save.reply", Locale.ENGLISH, "test");
        assertThat(res).isEqualTo("`test`\nSaved direct roll channel config");
    }

    @Test
    void testFrench() {
        String res = I18n.getMessage("base.help.description", Locale.FRENCH);
        assertThat(res).isEqualTo("Obtenir de l''aide pour /{0}");
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