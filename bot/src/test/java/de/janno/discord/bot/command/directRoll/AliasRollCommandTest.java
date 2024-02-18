package de.janno.discord.bot.command.directRoll;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.channelConfig.Alias;
import de.janno.discord.bot.command.channelConfig.AliasConfig;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static de.janno.discord.bot.command.channelConfig.AliasHelper.CHANNEL_ALIAS_CONFIG_TYPE_ID;
import static de.janno.discord.bot.command.channelConfig.AliasHelper.USER_ALIAS_CONFIG_TYPE_ID;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)

public class AliasRollCommandTest {
    private PersistenceManager persistenceManager;
    private Expect expect;
    private AliasRollCommand underTest;

    static Stream<Arguments> generateAllLocaleData() {
        return I18n.allSupportedLanguage().stream()
                .map(Arguments::of);
    }

    @BeforeEach
    void setup() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new AliasRollCommand(persistenceManager, new CachingDiceEvaluator((minExcl, maxIncl) -> 1, 1000, 0));
    }

    @ParameterizedTest(name = "{index} locale={0}")
    @MethodSource("generateAllLocaleData")
    void testHelp(Locale userLocale) {
        expect.scenario(userLocale.toString()).toMatchSnapshot(underTest.getHelpMessage(userLocale));
    }

    @Test
    public void getCommandDefinition() {
        expect.toMatchSnapshot(underTest.getCommandDefinition());
    }

    @Test
    public void getId() {
        assertThat(underTest.getCommandId()).isEqualTo("a");
    }

    @Test
    void autoCompleteNoAlias() {

        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("alias_or_expression", null, List.of()), Locale.ENGLISH, 1L, 1L);

        assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly("No alias configured in this channel, add them with `/channel_config alias save`");
        assertThat(res.stream().map(AutoCompleteAnswer::getValue)).containsExactly("help");
    }

    @Test
    void autoCompleteValid() {
        createAlias();

        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("alias_or_expression", null, List.of()), Locale.ENGLISH, 1L, 1L);

        assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly("a", "b", "c");
        assertThat(res.stream().map(AutoCompleteAnswer::getValue)).containsExactly("a", "b", "c");
    }

    @Test
    void autoCompleteValid_noUserAlias() {
        createAlias();

        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("alias_or_expression", null, List.of()), Locale.ENGLISH, 1L, 3L);

        assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly("b", "c");
        assertThat(res.stream().map(AutoCompleteAnswer::getValue)).containsExactly("b", "c");
    }

    @Test
    void autoCompleteFilter() {
        createAlias();

        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("alias_or_expression", "b", List.of()), Locale.ENGLISH, 1L, 1L);

        assertThat(res.stream().map(AutoCompleteAnswer::getName)).containsExactly("b");
        assertThat(res.stream().map(AutoCompleteAnswer::getValue)).containsExactly("b");
    }

    @Test
    void autoCompleteFilterNotFound() {
        createAlias();

        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("alias_or_expression", "bc", List.of()), Locale.ENGLISH, 1L, 1L);

        assertThat(res).isEmpty();
    }

    private void createAlias() {
        persistenceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(),
                1L,
                1L,
                1L,
                "channel_config",
                USER_ALIAS_CONFIG_TYPE_ID,
                Mapper.serializedObject(new AliasConfig(List.of(new Alias("a", "1d6"), new Alias("b", "2d6"))))));

        persistenceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(),
                1L,
                1L,
                null,
                "channel_config",
                CHANNEL_ALIAS_CONFIG_TYPE_ID,
                Mapper.serializedObject(new AliasConfig(List.of(new Alias("b", "1d8"), new Alias("c", "2d8"))))));

        persistenceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(),
                1L,
                2L,
                1L,
                "channel_config",
                USER_ALIAS_CONFIG_TYPE_ID,
                Mapper.serializedObject(new AliasConfig(List.of(new Alias("z", "1d10"), new Alias("a", "1d10"), new Alias("b", "1d10"))))));

        persistenceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(),
                1L,
                2L,
                null,
                "channel_config",
                CHANNEL_ALIAS_CONFIG_TYPE_ID,
                Mapper.serializedObject(new AliasConfig(List.of(new Alias("z", "1d10"), new Alias("b", "1d10"), new Alias("c", "1d10"))))));

        persistenceManager.saveChannelConfig(new ChannelConfigDTO(UUID.randomUUID(),
                1L,
                2L,
                2L,
                "channel_config",
                USER_ALIAS_CONFIG_TYPE_ID,
                Mapper.serializedObject(new AliasConfig(List.of(new Alias("y", "1d12"), new Alias("a", "1d12"), new Alias("b", "1d12"))))));
    }

}
