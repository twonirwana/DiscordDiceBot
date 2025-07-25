package de.janno.discord.bot.command.channelConfig;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.directRoll.DirectRollCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.OptionValue;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
public class ChannelConfigMockTest {

    PersistenceManager persistenceManager;
    Expect expect;

    @BeforeEach
    void setup() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }


    @Test
    void saveConfigDelete_default() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save_direct_roll_config")
                .option(CommandInteractionOption.builder()
                        .name("always_sum_result")
                        .booleanValue(false)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("answer_format")
                        .stringValue("minimal")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("result_image")
                        .stringValue("polyhedral_3d_red_and_white")
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("delete_direct_roll_config")
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent3, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("slashEvent1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("slashEvent2").toMatchSnapshot(slashEvent2.getSortedActions());
        expect.scenario("slashEvent3").toMatchSnapshot(slashEvent3.getSortedActions());
    }

    @Test
    void saveConfigDelete_otherChannel() {
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("save_direct_roll_config")
                .option(CommandInteractionOption.builder()
                        .name("always_sum_result")
                        .booleanValue(false)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("answer_format")
                        .stringValue("minimal")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("result_image")
                        .stringValue("polyhedral_3d_red_and_white")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("channel")
                        .channelIdValue(4L)
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
                .build()));
        directRollCommand.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("delete_direct_roll_config")
                .option(CommandInteractionOption.builder()
                        .name("channel")
                        .channelIdValue(4L)
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent3, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("slashEvent1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("slashEvent2").toMatchSnapshot(slashEvent2.getSortedActions());
        expect.scenario("slashEvent3").toMatchSnapshot(slashEvent3.getSortedActions());
    }

    @Test
    void autoCompleteNoScope() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);
        List<AutoCompleteAnswer> res = channelConfig.getAutoCompleteAnswer(new AutoCompleteRequest("name", null, List.of()), Locale.ENGLISH, 1L, 1L, 0L);

        expect.toMatchSnapshot(res);
    }

    @Test
    void aliasMultiLine() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEventAdaptorMock = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20_'\\n'_1d6").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));

        channelConfig.handleSlashCommandEvent(slashEventAdaptorMock, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        Optional<ChannelConfigDTO> res = persistenceManager.getUserChannelConfig(SlashEventAdaptorMock.CHANNEL_ID, slashEventAdaptorMock.userId, AliasHelper.USER_ALIAS_CONFIG_TYPE_ID);
        assertThat(res).isPresent();
        expect.toMatchSnapshot(res.get());
    }

    @Test
    void autoCompleteMatchingUserAliasScope() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        channelConfig.handleSlashCommandEvent(new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build())), () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        channelConfig.handleSlashCommandEvent(new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att2").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("3d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build())), () -> UUID.fromString("00000000-0000-0000-0000-000000000001"), Locale.ENGLISH).block();

        channelConfig.handleSlashCommandEvent(new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("Block").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("3d8+4=").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build())), () -> UUID.fromString("00000000-0000-0000-0000-000000000002"), Locale.ENGLISH).block();


        List<AutoCompleteAnswer> res = channelConfig.getAutoCompleteAnswer(new AutoCompleteRequest("name", "a", List.of(new OptionValue("scope", "current_user_in_this_channel"))), Locale.ENGLISH, 1L, 0L, 0L);

        expect.toMatchSnapshot(res);
    }

    @Test
    void autoCompleteMatchingChannelAliasScope() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        channelConfig.handleSlashCommandEvent(new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build())), () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        channelConfig.handleSlashCommandEvent(new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att2").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("3d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build())), () -> UUID.fromString("00000000-0000-0000-0000-000000000001"), Locale.ENGLISH).block();

        channelConfig.handleSlashCommandEvent(new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("Block").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("3d8+4=").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build())), () -> UUID.fromString("00000000-0000-0000-0000-000000000002"), Locale.ENGLISH).block();


        List<AutoCompleteAnswer> res = channelConfig.getAutoCompleteAnswer(new AutoCompleteRequest("name", "a", List.of(new OptionValue("scope", "all_users_in_this_channel"))), Locale.ENGLISH, 1L, 0L, 0L);

        expect.toMatchSnapshot(res);
    }

    @Test
    void channelAlias_createListDelete() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("delete")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent3, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent4 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent4, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("slashEvent1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("slashEvent2").toMatchSnapshot(slashEvent2.getSortedActions());
        expect.scenario("slashEvent3").toMatchSnapshot(slashEvent3.getSortedActions());
        expect.scenario("slashEvent4").toMatchSnapshot(slashEvent4.getSortedActions());
    }

    @Test
    void userChannelAlias_createListDelete() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("delete")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent3, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent4 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent4, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("slashEvent1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("slashEvent2").toMatchSnapshot(slashEvent2.getSortedActions());
        expect.scenario("slashEvent3").toMatchSnapshot(slashEvent3.getSortedActions());
        expect.scenario("slashEvent4").toMatchSnapshot(slashEvent4.getSortedActions());
    }

    @Test
    void channelAlias_createCreateDeleteAll() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("dmg").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("3d6").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("delete_all")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent3, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent4 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent4, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("slashEvent1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("slashEvent2").toMatchSnapshot(slashEvent2.getSortedActions());
        expect.scenario("slashEvent3").toMatchSnapshot(slashEvent3.getSortedActions());
        expect.scenario("slashEvent4").toMatchSnapshot(slashEvent4.getSortedActions());
    }

    @Test
    void channelAlias_createCreateDeleteAll_otherChannel() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .option(CommandInteractionOption.builder().name("channel").channelIdValue(4L).build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("dmg").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("3d6").build())
                        .option(CommandInteractionOption.builder().name("channel").channelIdValue(4L).build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder().name("list").build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .option(CommandInteractionOption.builder().name("channel").channelIdValue(4L).build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent3, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SlashEventAdaptorMock slashEvent4 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("delete_all")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .option(CommandInteractionOption.builder()
                        .name("channel")
                        .channelIdValue(4L)
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent4, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent5 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder().name("list").build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .option(CommandInteractionOption.builder().name("channel").channelIdValue(4L).build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent5, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("slashEvent1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("slashEvent2").toMatchSnapshot(slashEvent2.getSortedActions());
        expect.scenario("slashEvent3").toMatchSnapshot(slashEvent3.getSortedActions());
        expect.scenario("slashEvent4").toMatchSnapshot(slashEvent4.getSortedActions());
        expect.scenario("slashEvent5").toMatchSnapshot(slashEvent5.getSortedActions());
    }

    @Test
    void userChannelAlias_createCreateDeleteAll() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("dmg").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("3d6").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("delete_all")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent3, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent4 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent4, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("slashEvent1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("slashEvent2").toMatchSnapshot(slashEvent2.getSortedActions());
        expect.scenario("slashEvent3").toMatchSnapshot(slashEvent3.getSortedActions());
        expect.scenario("slashEvent4").toMatchSnapshot(slashEvent4.getSortedActions());
    }

    @Test
    void userChannelAlias_2UserCreateAlias() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()), 1L);
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+1").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()), 2L);
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000001"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent3 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()), 1L);
        channelConfig.handleSlashCommandEvent(slashEvent3, () -> UUID.fromString("00000000-0000-0000-0000-000000000002"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent4 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()), 2L);
        channelConfig.handleSlashCommandEvent(slashEvent4, () -> UUID.fromString("00000000-0000-0000-0000-000000000003"), Locale.ENGLISH).block();

        expect.scenario("slashEvent1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("slashEvent2").toMatchSnapshot(slashEvent2.getSortedActions());
        expect.scenario("slashEvent3").toMatchSnapshot(slashEvent3.getSortedActions());
        expect.scenario("slashEvent4").toMatchSnapshot(slashEvent4.getSortedActions());
    }

    @Test
    void channelAlias_multiCreateList() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("multi_save")
                        .option(CommandInteractionOption.builder()
                                .name("aliases")
                                .stringValue("att:2d20;dmg:2d6+3=")
                                .build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        expect.scenario("slashEvent1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("slashEvent2").toMatchSnapshot(slashEvent2.getSortedActions());
    }

    @Test
    void channelAlias_multiCreateList_wrongFormat() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("multi_save")
                        .option(CommandInteractionOption.builder()
                                .name("aliases")
                                .stringValue("att:2d20;dmg:")
                                .build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.toMatchSnapshot(slashEvent1.getSortedActions());
    }


    @Test
    void channelUserAlias_multiCreateList() {
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("multi_save")
                        .option(CommandInteractionOption.builder()
                                .name("aliases")
                                .stringValue("att:2d20;dmg:2d6+3=")
                                .build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        SlashEventAdaptorMock slashEvent2 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("list")
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent2, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        expect.scenario("slashEvent1").toMatchSnapshot(slashEvent1.getSortedActions());
        expect.scenario("slashEvent2").toMatchSnapshot(slashEvent2.getSortedActions());
    }
}
