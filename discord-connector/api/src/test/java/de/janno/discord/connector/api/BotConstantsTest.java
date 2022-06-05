package de.janno.discord.connector.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BotConstantsTest {

    @Test
    void splitLegacy() {
        String[] res = "a,b".split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);
        assertThat(res).containsExactly("a", "b");
    }

    @Test
    void splitCurrent() {
        String[] res = "a\u0000b".split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);
        assertThat(res).containsExactly("a", "b");
    }
}