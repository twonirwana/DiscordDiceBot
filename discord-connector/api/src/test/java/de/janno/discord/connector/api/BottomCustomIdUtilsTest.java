package de.janno.discord.connector.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BottomCustomIdUtilsTest {

    @Test
    void splitLegacy() {
        String[] res = "a,b".split(BottomCustomIdUtils.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
        assertThat(res).containsExactly("a", "b");
    }

    @Test
    void splitCurrent() {
        String[] res = "a\u0000b".split(BottomCustomIdUtils.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
        assertThat(res).containsExactly("a", "b");
    }

    @Test
    void createButtonCustomId() {
        String res = BottomCustomIdUtils.createButtonCustomId("testCommand", "testValue");

        assertThat(res).isEqualTo("testCommand\u001EtestValue");
    }

    @Test
    void isLegacyCustomId_true() {
        assertThat(BottomCustomIdUtils.isLegacyCustomId("a\u0000b")).isTrue();
    }

    @Test
    void isLegacyCustomId_false() {
        assertThat(BottomCustomIdUtils.isLegacyCustomId("a\u001eb")).isFalse();

    }

    @Test
    void getButtonValueFromCustomId() {
        String res = BottomCustomIdUtils.getButtonValueFromCustomId("a\u001eb");

        assertThat(res).isEqualTo("b");
    }

    @Test
    void getButtonValueFromLegacyCustomId() {
        String res = BottomCustomIdUtils.getButtonValueFromLegacyCustomId("a,b");

        assertThat(res).isEqualTo("b");
    }

    @Test
    void getButtonValueFromLegacyCustomIdV2() {
        String res = BottomCustomIdUtils.getButtonValueFromLegacyCustomId("a\u0000b");

        assertThat(res).isEqualTo("b");
    }

    @Test
    void getCommandNameFromCustomIdWithPersistence() {
        String res = BottomCustomIdUtils.getCommandNameFromCustomIdWithPersistence("a\u001eb");

        assertThat(res).isEqualTo("a");
    }
}