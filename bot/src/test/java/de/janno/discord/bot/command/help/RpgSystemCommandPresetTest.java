package de.janno.discord.bot.command.help;

import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.channelConfig.AliasConfig;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class RpgSystemCommandPresetTest {


    @ParameterizedTest
    @EnumSource(RpgSystemCommandPreset.PresetId.class)
    void testCommandAndClassOfPreset(RpgSystemCommandPreset.PresetId presetId) {
        Config config = RpgSystemCommandPreset.createConfig(presetId, Locale.ENGLISH);
        switch (config) {
            case CustomDiceConfig ignored -> {
                assertThat(presetId.getCommandId()).isEqualTo("custom_dice");
                assertThat(presetId.getConfigClassType()).isEqualTo("CustomDiceConfig");
            }
            case CustomParameterConfig ignored -> {
                assertThat(presetId.getCommandId()).isEqualTo("custom_parameter");
                assertThat(presetId.getConfigClassType()).isEqualTo("CustomParameterConfig");
            }
            case SumCustomSetConfig ignored -> {
                assertThat(presetId.getCommandId()).isEqualTo("sum_custom_set");
                assertThat(presetId.getConfigClassType()).isEqualTo("SumCustomSetConfig");
            }
            case AliasConfig ignored -> {
                assertThat(presetId.getCommandId()).isEqualTo("channel_config");
                assertThat(presetId.getConfigClassType()).isEqualTo("AliasConfig");
            }
            default -> fail();
        }
    }

    @Test
        //primarily used to generate the documentation
    void allQuickstartNames() {
        String allQuickstarts = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .map(p -> RpgSystemCommandPreset.createConfig(p, Locale.ENGLISH))
                .filter(c -> !(c instanceof AliasConfig))
                .map(Config::getName)
                .map("|`%s`"::formatted)
                .sorted()
                .collect(Collectors.joining("\n"));

        assertThat(allQuickstarts).isEqualTo("""
                |`A Song of Ice and Fire`
                |`Blades in the Dark - Detail`
                |`Blades in the Dark without Dice Images`
                |`Blades in the Dark`
                |`Bluebeard's Bride`
                |`Call of Cthulhu 7th Edition`
                |`Candela Obscura v2`
                |`Candela Obscura`
                |`City of Mist`
                |`Cyberpunk Red`
                |`Daggerheart`
                |`Dice Calculator`
                |`Dungeon & Dragons 5e Calculator 2`
                |`Dungeon & Dragons 5e Calculator`
                |`Dungeon & Dragons 5e without Dice Images`
                |`Dungeon & Dragons 5e`
                |`Dungeon Crawl Classics`
                |`EZD6`
                |`Exalted 3ed`
                |`Fallout`
                |`Fate without Dice Images`
                |`Fate`
                |`Forbidden Lands`
                |`Ghostbusters: A Frightfully Cheerful Roleplaying Game First Edition & Spooktacular`
                |`Heroes of Cerulea`
                |`Hunter 5ed`
                |`Ironsworn`
                |`Kids on Brooms`
                |`OSR`
                |`Oathsworn`
                |`One-Roll Engine`
                |`Otherscape`
                |`Paranoia: Red Clearance Edition`
                |`Powered by the Apocalypse`
                |`Prowlers & Paragons Ultimate Edition`
                |`Public Access`
                |`Rebellion Unplugged`
                |`Risus The Anything RPG "Evens Up"`
                |`Rêve de Dragon`
                |`Salvage Union BDR-V1.0`
                |`Savage Worlds`
                |`Shadowdark`
                |`Shadowrun without Dice Images`
                |`Shadowrun`
                |`Star Wars - West End Games D6 Rules, 2nd Edition REUP`
                |`The Expanse`
                |`The Marvel Multiverse Role-Playing Game`
                |`The One Ring`
                |`Tiny D6`
                |`Traveller`
                |`Vampire 5ed`
                |`Warhammer Age of Sigmar: Soulbound`
                |`Year Zero Engine: Alien`
                |`nWod / Chronicles of Darkness`
                |`oWod / Storyteller System`
                |`🪙Coin Toss`""");
    }
}