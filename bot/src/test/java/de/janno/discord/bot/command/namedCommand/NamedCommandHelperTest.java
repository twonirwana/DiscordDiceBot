package de.janno.discord.bot.command.namedCommand;

import de.janno.discord.bot.command.help.RpgSystemCommandPreset;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NamedCommandHelperTest {

    @Test
    void getPresetId_idMatch() {
        Optional<RpgSystemCommandPreset.PresetId> res = NamedCommandHelper.getPresetId("DND5", Locale.ENGLISH);

        assertThat(res).contains(RpgSystemCommandPreset.PresetId.DND5);
    }

    @Test
    void getPresetId_nameMatch() {
        Optional<RpgSystemCommandPreset.PresetId> res = NamedCommandHelper.getPresetId("Dungeon & dragons 5e ", Locale.ENGLISH);

        assertThat(res).contains(RpgSystemCommandPreset.PresetId.DND5_IMAGE);
    }

    @Test
    void getPresetId_synonymeMatch() {
        Optional<RpgSystemCommandPreset.PresetId> res = NamedCommandHelper.getPresetId(" reve de Dragon", Locale.ENGLISH);

        assertThat(res).contains(RpgSystemCommandPreset.PresetId.REVE_DE_DRAGON);
    }

    @Test
    void getPresetId_nameStartsWith() {
        Optional<RpgSystemCommandPreset.PresetId> res = NamedCommandHelper.getPresetId(" oWod ", Locale.ENGLISH);

        assertThat(res).contains(RpgSystemCommandPreset.PresetId.OWOD);
    }

    @Test
    void getPresetId_noMatch() {
        Optional<RpgSystemCommandPreset.PresetId> res = NamedCommandHelper.getPresetId(" Opus Anima ", Locale.ENGLISH);

        assertThat(res).isEmpty();
    }

}