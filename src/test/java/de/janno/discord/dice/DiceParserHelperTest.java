package de.janno.discord.dice;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DiceParserHelperTest {

    @Test
    void validateDiceExpressions() {
        assertThat(DiceParserHelper.validateDiceExpressions(ImmutableList.of("1d4/"), "test"))
                .isEqualTo("The following dice expression are invalid: 1d4/. Use test to get more information on how to use the command.");
    }
}