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
                Arguments.of(List.of(new Alias("base", "1d20", Alias.Type.Replace)), List.of(new Alias("bonus", "20", Alias.Type.Replace)), "base+bonus", "1d20+20"),
                Arguments.of(List.of(new Alias("Attack", "1d20", Alias.Type.Replace)), List.of(new Alias("att", "Attack", Alias.Type.Replace)), "att@roll", "1d20@roll"),
                Arguments.of(List.of(new Alias("att", "Attack", Alias.Type.Replace), new Alias("Attack", "2d20", Alias.Type.Replace)), List.of(), "att@roll", "2d20@roll"),
                Arguments.of(List.of(new Alias("1d20", "1d20+1d20", Alias.Type.Replace)), List.of(), "1d20@roll", "1d20+1d20@roll"),
                Arguments.of(List.of(new Alias("att", "1d20", Alias.Type.Replace), new Alias("att", "2d20", Alias.Type.Replace)), List.of(), "att@roll", "1d20@roll"),
                Arguments.of(List.of(), List.of(new Alias("att", "1d20", Alias.Type.Replace), new Alias("att", "2d20", Alias.Type.Replace)), "att@roll", "1d20@roll"),
                Arguments.of(List.of(new Alias("att", "1d20", Alias.Type.Replace)), List.of(new Alias("att", "2d20", Alias.Type.Replace)), "att@roll", "2d20@roll"),
                Arguments.of(List.of(new Alias("att", "1d20", Alias.Type.Replace)), List.of(), "att@roll", "1d20@roll"),
                Arguments.of(List.of(), List.of(new Alias("att", "1d20", Alias.Type.Replace)), "att@roll", "1d20@roll"),
                Arguments.of(List.of(new Alias("att", "1d20", Alias.Type.Replace)), List.of(), "1d6@roll", "1d6@roll"),
                Arguments.of(List.of(), List.of(new Alias("att", "1d20", Alias.Type.Replace)), "1d6@roll", "1d6@roll"),
                Arguments.of(List.of(), List.of(new Alias("att", "1d20", Alias.Type.Regex)), "1d6@roll", "1d6@roll"),
                Arguments.of(List.of(), List.of(new Alias("att", "1d20", Alias.Type.Regex)), "att@roll", "1d20@roll"),
                Arguments.of(List.of(), List.of(new Alias("(\\d+)att", "$1d20", Alias.Type.Regex)), "3att@roll", "3d20@roll"),
                Arguments.of(List.of(), List.of(new Alias("\\", "$1d20", Alias.Type.Regex)), "3d20@roll", "3d20@roll"), //invalid regex is ignored
                Arguments.of(List.of(), List.of(new Alias("(\\d+)att", "$1d20+$2", Alias.Type.Regex)), "3att@roll", "3att@roll"),
                Arguments.of(List.of(new Alias("(?<first>\\w+), (?<second>\\w+)", "${second}, ${first}", Alias.Type.Regex)), List.of(), "1d6, 2d8", "2d8, 1d6"),
                Arguments.of(List.of(new Alias("(?<first>\\w+), (?<second>\\w+)", "${second} - ${first}", Alias.Type.Regex)), List.of(new Alias("(?<first>\\w+), (?<second>\\w+)", "${second} = ${first}", Alias.Type.Regex)), "1d6, 2d8", "2d8 = 1d6"),
                Arguments.of(List.of(), List.of(new Alias("(?<first>\\w+), (?<second>\\w+)", "${second}, ${first}", Alias.Type.Regex)), "1d6, 2d8", "2d8, 1d6"),
                Arguments.of(List.of(), List.of(new Alias("(?<first>\\w+), (?<second>\\w+)", "${second}, ${first}, ${third}", Alias.Type.Regex)), "1d6, 2d8", "1d6, 2d8"),
                Arguments.of(List.of(), List.of(new Alias("(?<numberOfDice>\\d+)r(?<target>\\d+)", "val('roll',${numberOfDice}d10) ('roll'>=${target}c)-('roll'==1c)=@:${numberOfDice}d10 vs ${target}", Alias.Type.Regex)), "6r7", "val('roll',6d10) ('roll'>=7c)-('roll'==1c)=@:6d10 vs 7"),
                Arguments.of(List.of(), List.of(new Alias("(?<numberOfDice>\\d+)re(?<target>\\d+)", "val('roll',${numberOfDice}d!10) ('roll'>=${target}c)-('roll'==1c)=@:${numberOfDice}d10 vs ${target} with reroll on 10", Alias.Type.Regex)),"7re8", "val('roll',7d!10) ('roll'>=8c)-('roll'==1c)=@:7d10 vs 8 with reroll on 10"),
                Arguments.of(List.of(), List.of(new Alias("(?<numberOfDice>\\d+)sr", "val('roll',${numberOfDice}d6) concat('roll'>4c, if('roll'==1c >? 'roll'c/2,' - Glitch!'))@${numberOfDice}d6", Alias.Type.Regex)), "8sr", "val('roll',8d6) concat('roll'>4c, if('roll'==1c >? 'roll'c/2,' - Glitch!'))@8d6"),
                Arguments.of(List.of(), List.of(new Alias("sw(?<sides>\\d+)", "1d!!${sides} + 1d!!6 k1@d${sides} Wildcard", Alias.Type.Regex)), "sw12", "1d!!12 + 1d!!6 k1@d12 Wildcard"),
                Arguments.of(List.of(), List.of(new Alias("(?<numberOfDice>\\d+)b", "val('diceRoll',  if(${numberOfDice}=?0,2d6L1, ${numberOfDice}d6) val('sixes','diceRoll'==6c) val('partials','diceRoll'>3<6c)  if('sixes'>?1,'Critical Success - You do it with increased effect.', 'sixes'=?1,'Success - You do it.','partials' >? 0,'Partial Success - You do it but suffer severe harm, a serious complication or have reduced effect.','Failure - You suffer severe harm, a serious complication occurs, or you lose this opportunity for action.'@${numberOfDice} Dice", Alias.Type.Regex)), "6b", "val('diceRoll',  if(6=?0,2d6L1, 6d6) val('sixes','diceRoll'==6c) val('partials','diceRoll'>3<6c)  if('sixes'>?1,'Critical Success - You do it with increased effect.', 'sixes'=?1,'Success - You do it.','partials' >? 0,'Partial Success - You do it but suffer severe harm, a serious complication or have reduced effect.','Failure - You suffer severe harm, a serious complication occurs, or you lose this opportunity for action.'@6 Dice"),

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
    void deserialization_alias_legcacy() {

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

        ChannelConfigDTO savedData = new ChannelConfigDTO(UUID.fromString("00000000-0000-0000-0000-000000000000"), 1L, 2L, null, "r", "AliasConfig", aliasString, null);

        AliasConfig res = AliasHelper.deserializeAliasConfig(savedData);
        assertThat(res).isEqualTo(new AliasConfig(List.of(new Alias("att", "d20+5", Alias.Type.Replace), new Alias("par", "d20+3", Alias.Type.Replace), new Alias("dmg", "3d6+4", Alias.Type.Replace)), null));

    }

    @Test
    void deserialization_alias_legcacy2() {

        String aliasString = """
                ---
                aliasList:
                - name: "att"
                  value: "d20+5"
                  type: Replace
                - name: "par"
                  value: "d20+3"
                  type: Regex
                - name: "dmg"
                  value: "3d6+4"
                  type: Regex
                """;

        ChannelConfigDTO savedData = new ChannelConfigDTO(UUID.fromString("00000000-0000-0000-0000-000000000000"), 1L, 2L, null, "r", "AliasConfig", aliasString, null);

        AliasConfig res = AliasHelper.deserializeAliasConfig(savedData);
        assertThat(res)
                .isEqualTo(new AliasConfig(List.of(new Alias("att", "d20+5", Alias.Type.Replace), new Alias("par", "d20+3", Alias.Type.Regex), new Alias("dmg", "3d6+4", Alias.Type.Regex)), null));

    }

    @Test
    void deserialization_alias() {

        String aliasString = """
                ---
                aliasList:
                - name: "att"
                  value: "d20+5"
                  type: Replace
                - name: "par"
                  value: "d20+3"
                  type: Regex
                - name: "dmg"
                  value: "3d6+4"
                  type: Regex
                name: alias name
                """;

        ChannelConfigDTO savedData = new ChannelConfigDTO(UUID.fromString("00000000-0000-0000-0000-000000000000"), 1L, 2L, null, "r", "AliasConfig", aliasString, "alias name");

        AliasConfig res = AliasHelper.deserializeAliasConfig(savedData);
        assertThat(res)
                .isEqualTo(new AliasConfig(List.of(new Alias("att", "d20+5", Alias.Type.Replace), new Alias("par", "d20+3", Alias.Type.Regex), new Alias("dmg", "3d6+4", Alias.Type.Regex)), "alias name"));

    }
}
