package de.janno.discord.bot.command.channelConfig;

import de.janno.discord.bot.persistance.ChannelConfigDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AliasHelperTest {


    static Stream<Arguments> generateAliasData() {
        return Stream.of(
                Arguments.of(List.of(new Alias("base", "1d20")), List.of(new Alias("bonus", "20")), "base+bonus", "1d20+20"),
                Arguments.of(List.of(new Alias("Attack", "1d20")), List.of(new Alias("att", "Attack")), "att@roll", "1d20@roll"),
                Arguments.of(List.of(new Alias("att", "Attack"), new Alias("Attack", "2d20")), List.of(), "att@roll", "2d20@roll"),
                Arguments.of(List.of(new Alias("1d20", "1d20+1d20")), List.of(), "1d20@roll", "1d20+1d20@roll"),
                Arguments.of(List.of(new Alias("att", "1d20"), new Alias("att", "2d20")), List.of(), "att@roll", "1d20@roll"),
                Arguments.of(List.of(), List.of(new Alias("att", "1d20"), new Alias("att", "2d20")), "att@roll", "1d20@roll"),
                Arguments.of(List.of(new Alias("att", "1d20")), List.of(new Alias("att", "2d20")), "att@roll", "2d20@roll"),
                Arguments.of(List.of(new Alias("att", "1d20")), List.of(), "att@roll", "1d20@roll"),
                Arguments.of(List.of(), List.of(new Alias("att", "1d20")), "att@roll", "1d20@roll"),
                Arguments.of(List.of(new Alias("att", "1d20")), List.of(), "1d6@roll", "1d6@roll"),
                Arguments.of(List.of(), List.of(new Alias("att", "1d20")), "1d6@roll", "1d6@roll"),
                Arguments.of(List.of(), List.of(), "1d6@roll", "1d6@roll")
        );
    }

    @ParameterizedTest(name = "{index} channelAlias={0}, userChannelAlias={1}, expression={3} -> {4}")
    @MethodSource("generateAliasData")
    void applyAlias(List<Alias> channelAlias, List<Alias> userChannelAlias, String expression, String expected) {
        String res = AliasHelper.applyAliaseToExpression(channelAlias, userChannelAlias, expression);
        assertThat(res).contains(expected);
    }

    @Test
    void deserialization_alias() {

        String aliasString = """
                ---
                aliasList:
                - name: "att"
                  value: "d20+5"
                - name: "par"
                  value: "d20+3"
                - name: "dmg"
                  value: "3d6+4"
                """;

        ChannelConfigDTO savedData = new ChannelConfigDTO(UUID.fromString("00000000-0000-0000-0000-000000000000"), 1L, 2L, null, "r", "AliasConfig", aliasString);

        AliasConfig res = AliasHelper.deserializeAliasConfig(savedData);
        assertThat(res).isEqualTo(new AliasConfig(List.of(new Alias("att", "d20+5"), new Alias("par", "d20+3"), new Alias("dmg", "3d6+4"))));

    }
}
