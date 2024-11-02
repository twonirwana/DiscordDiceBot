package de.janno.discord.bot.command.help;

import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.channelConfig.AliasConfig;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class RpgSystemCommandPresetTest {


    @ParameterizedTest
    @EnumSource(RpgSystemCommandPreset.PresetId.class)
    void testCommandAndClassOfPreset(RpgSystemCommandPreset.PresetId presetId) {
        Config config = RpgSystemCommandPreset.createConfig(presetId, Locale.ENGLISH);
        if (config instanceof CustomDiceConfig) {
            assertThat(presetId.getCommandId()).isEqualTo("custom_dice");
            assertThat(presetId.getConfigClassType()).isEqualTo("CustomDiceConfig");
        } else if (config instanceof CustomParameterConfig) {
            assertThat(presetId.getCommandId()).isEqualTo("custom_parameter");
            assertThat(presetId.getConfigClassType()).isEqualTo("CustomParameterConfig");
        } else if (config instanceof SumCustomSetConfig) {
            assertThat(presetId.getCommandId()).isEqualTo("sum_custom_set");
            assertThat(presetId.getConfigClassType()).isEqualTo("SumCustomSetConfig");
        } else if (config instanceof AliasConfig) {
            assertThat(presetId.getCommandId()).isEqualTo("channel_config");
            assertThat(presetId.getConfigClassType()).isEqualTo("AliasConfig");
        } else {
            fail();
        }
    }
}