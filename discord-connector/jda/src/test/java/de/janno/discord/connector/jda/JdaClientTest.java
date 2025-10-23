package de.janno.discord.connector.jda;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import io.avaje.config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.ChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.IThreadContainerUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import net.dv8tion.jda.api.managers.ApplicationManager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JdaClientTest {

    @Test
    void onChannelCreateHandler_privateThread() {
        ChannelCreateEvent channelCreateEvent = mock(ChannelCreateEvent.class);
        List<ChildrenChannelCreationEvent> allChildrenChannelCreationEvents = new ArrayList<>();
        ChannelUnion channelUnion = mock(ChannelUnion.class);
        when(channelCreateEvent.getChannel()).thenReturn(channelUnion);
        when(channelUnion.getIdLong()).thenReturn(1L);
        when(channelUnion.getType()).thenReturn(ChannelType.GUILD_PRIVATE_THREAD);
        ThreadChannel threadChannel = mock(ThreadChannel.class);
        when(channelUnion.asThreadChannel()).thenReturn(threadChannel);
        IThreadContainerUnion parentChannel = mock(IThreadContainerUnion.class);
        when(threadChannel.getParentChannel()).thenReturn(parentChannel);
        when(parentChannel.getIdLong()).thenReturn(2L);


        JdaClient.onChannelCreateHandler(channelCreateEvent, allChildrenChannelCreationEvents::add);


        assertThat(allChildrenChannelCreationEvents).containsExactly(new ChildrenChannelCreationEvent(1L, 2L));
    }

    @Test
    void onChannelCreateHandler_publicThread() {
        ChannelCreateEvent channelCreateEvent = mock(ChannelCreateEvent.class);
        List<ChildrenChannelCreationEvent> allChildrenChannelCreationEvents = new ArrayList<>();
        ChannelUnion channelUnion = mock(ChannelUnion.class);
        when(channelCreateEvent.getChannel()).thenReturn(channelUnion);
        when(channelUnion.getIdLong()).thenReturn(1L);
        when(channelUnion.getType()).thenReturn(ChannelType.GUILD_PUBLIC_THREAD);
        ThreadChannel threadChannel = mock(ThreadChannel.class);
        when(channelUnion.asThreadChannel()).thenReturn(threadChannel);
        IThreadContainerUnion parentChannel = mock(IThreadContainerUnion.class);
        when(threadChannel.getParentChannel()).thenReturn(parentChannel);
        when(parentChannel.getIdLong()).thenReturn(2L);


        JdaClient.onChannelCreateHandler(channelCreateEvent, allChildrenChannelCreationEvents::add);


        assertThat(allChildrenChannelCreationEvents).containsExactly(new ChildrenChannelCreationEvent(1L, 2L));
    }

    @Test
    void onChannelCreateHandler_test() {
        ChannelCreateEvent channelCreateEvent = mock(ChannelCreateEvent.class);
        List<ChildrenChannelCreationEvent> allChildrenChannelCreationEvents = new ArrayList<>();
        ChannelUnion channelUnion = mock(ChannelUnion.class);
        when(channelCreateEvent.getChannel()).thenReturn(channelUnion);
        when(channelUnion.getIdLong()).thenReturn(1L);
        when(channelUnion.getType()).thenReturn(ChannelType.TEXT);


        JdaClient.onChannelCreateHandler(channelCreateEvent, allChildrenChannelCreationEvents::add);


        assertThat(allChildrenChannelCreationEvents).isEmpty();
    }

    @Test
    void waitingForShardStartAndSendStatus() {
        ShardManager shardManager = mock(ShardManager.class);
        JDA shard = mock(JDA.class);
        JDA.ShardInfo shardInfo = mock(JDA.ShardInfo.class);
        when(shard.getShardInfo()).thenReturn(shardInfo);
        when(shardInfo.getShardString()).thenReturn("shardString");
        when(shardManager.getShards()).thenReturn(List.of(shard), List.of(shard, shard));
        Config.setProperty("newsGuildId", "guildId");
        Config.setProperty("newsChannelId", "channelId");
        Guild guild = mock(Guild.class);
        when(shard.getGuildById("guildId")).thenReturn(guild);
        NewsChannel newsChannel = mock(NewsChannel.class);
        when(guild.getChannelById(StandardGuildMessageChannel.class, "channelId")).thenReturn(newsChannel);
        when(newsChannel.getGuild()).thenReturn(guild);
        SelfMember member = mock(SelfMember.class);
        when(guild.getSelfMember()).thenReturn(member);
        when(member.hasPermission(newsChannel, Permission.MESSAGE_SEND)).thenReturn(true);
        when(member.hasPermission(newsChannel, Permission.MESSAGE_MANAGE)).thenReturn(true);
        MessageCreateAction messageCreateAction = mock(MessageCreateAction.class);
        when(newsChannel.sendMessage(anyString())).thenReturn(messageCreateAction);
        when(messageCreateAction.complete()).thenReturn(mock(Message.class));
        when(newsChannel.crosspostMessageById(any())).thenReturn(mock(RestAction.class));

        List<JDA> shards = JdaClient.waitingForShardStartAndSendStatus(shardManager, Set.of(), longs -> {
        }, Stopwatch.createStarted());

        assertThat(shards).hasSize(2);

    }

    @Test
    void setupApplication() {
        JDA shard = mock(JDA.class);
        ApplicationManager applicationManager = mock(ApplicationManager.class);
        when(shard.getApplicationManager()).thenReturn(applicationManager);
        when(applicationManager.setDescription(any())).thenReturn(applicationManager);
        when(applicationManager.setIcon(any())).thenReturn(applicationManager);
        when(applicationManager.setIntegrationTypeConfig(any())).thenReturn(applicationManager);
        when(applicationManager.setTags(any())).thenReturn(applicationManager);
        when(applicationManager.setInstallParams(any())).thenReturn(applicationManager);

        JdaClient.setupApplication(List.of(shard, shard));

        verify(applicationManager).setDescription("""
                A visual dice roller bot that uses buttons to trigger rolls.
                Documentation:  https://github.com/twonirwana/DiscordDiceBot
                Help: https://discord.gg/e43BsqKpFr
                Support: https://ko-fi.com/2nirwana""");
        verify(applicationManager).setTags(List.of("Dice", "RPG", "Roller", "TTRPG", "D20"));
    }

    @Test
    void onGuildReadyHandler() {
        Set<Long> guildIdSet = new HashSet<>();
        GuildReadyEvent guildReadyEvent = mock(GuildReadyEvent.class);
        DatabaseConnector databaseConnector = mock(DatabaseConnector.class);
        Guild guild = mock(Guild.class);
        when(guildReadyEvent.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(1L);

        JdaClient.onGuildReadyHandler(guildReadyEvent, guildIdSet, databaseConnector);

        verify(databaseConnector, times(1)).unmarkDataOfJoiningGuilds(1L);
        assertThat(guildIdSet).containsExactly(1L);
    }

    @Test
    void onGuildLeaveHandler() {
        Set<Long> guildIdSet = Sets.newHashSet(1L);
        GuildLeaveEvent guildLeaveEvent = mock(GuildLeaveEvent.class);
        DatabaseConnector databaseConnector = mock(DatabaseConnector.class);
        Guild guild = mock(Guild.class);
        when(guildLeaveEvent.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(1L);

        JdaClient.onGuildLeaveHandler(guildLeaveEvent, guildIdSet, databaseConnector);

        verify(databaseConnector, times(1)).markDataOfLeavingGuildsToDelete(1L);
        assertThat(guildIdSet).isEmpty();
    }

    @Test
    void onGuildJoinHandler_notStarted() {
        JdaClient.started.set(false);
        Set<Long> guildIdSet = Sets.newHashSet();
        GuildJoinEvent guildJoinEvent = mock(GuildJoinEvent.class);
        DatabaseConnector databaseConnector = mock(DatabaseConnector.class);
        WelcomeMessageCreator welcomeMessageCreator = mock(WelcomeMessageCreator.class);
        Scheduler scheduler = mock(Scheduler.class);
        Guild guild = mock(Guild.class);
        when(guildJoinEvent.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(1L);

        JdaClient.onGuildJoinHandler(guildJoinEvent, guildIdSet, databaseConnector, welcomeMessageCreator, scheduler);

        verify(databaseConnector, times(1)).unmarkDataOfJoiningGuilds(1L);
        assertThat(guildIdSet).containsExactly(1L);
        verifyNoInteractions(welcomeMessageCreator);
    }

    @Test
    void onGuildJoinHandler_started() {
        JdaClient.started.set(true);
        Set<Long> guildIdSet = Sets.newHashSet();
        GuildJoinEvent guildJoinEvent = mock(GuildJoinEvent.class);
        DatabaseConnector databaseConnector = mock(DatabaseConnector.class);
        WelcomeMessageCreator welcomeMessageCreator = mock(WelcomeMessageCreator.class);
        Guild guild = mock(Guild.class);
        when(guildJoinEvent.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(1L);
        TextChannel textChannel = mock(TextChannel.class);
        when(guild.getSystemChannel()).thenReturn(textChannel);
        when(textChannel.canTalk()).thenReturn(true);
        when(guild.getLocale()).thenReturn(DiscordLocale.ENGLISH_US);
        when(welcomeMessageCreator.getWelcomeMessage(any())).thenReturn(new WelcomeMessageCreator.MessageAndConfigId(EmbedOrMessageDefinition.builder().shortedContent("test").build(), UUID.randomUUID()));
        MessageCreateAction messageCreateAction = mock(MessageCreateAction.class);
        Message message = mock(Message.class);
        when(messageCreateAction.submit()).thenReturn(CompletableFuture.completedFuture(message));
        when(textChannel.sendMessage(any(MessageCreateData.class))).thenReturn(messageCreateAction);

        JdaClient.onGuildJoinHandler(guildJoinEvent, guildIdSet, databaseConnector, welcomeMessageCreator, Schedulers.immediate());

        verify(databaseConnector, times(1)).unmarkDataOfJoiningGuilds(1L);
        assertThat(guildIdSet).containsExactly(1L);
        verify(textChannel, times(1)).sendMessage(any(MessageCreateData.class));
    }

    @Test
    void onComponentEventHandler() {
        GenericComponentInteractionCreateEvent event = mock(GenericComponentInteractionCreateEvent.class);
        ComponentInteraction interaction = mock(ComponentInteraction.class);
        when(event.getInteraction()).thenReturn(interaction);
        User user = mock(User.class);
        when(interaction.getUser()).thenReturn(user);
        when(interaction.getUserLocale()).thenReturn(DiscordLocale.ENGLISH_US);
        MessageChannelUnion messageChannelUnion = mock(MessageChannelUnion.class);
        when(event.getChannel()).thenReturn(messageChannelUnion);
        when(messageChannelUnion.getName()).thenReturn("channelName");
        JDA jda = mock(JDA.class);
        when(event.getJDA()).thenReturn(jda);
        when(jda.getShardInfo()).thenReturn(new JDA.ShardInfo(0, 1));
        Message message = mock(Message.class);
        when(event.getMessage()).thenReturn(message);
        when(message.isPinned()).thenReturn(false);
        when(event.getUser()).thenReturn(user);
        when(event.getUser().getIdLong()).thenReturn(1L);

        TestComponentCommand testComponentCommand1 = new TestComponentCommand("test1");
        TestComponentCommand testComponentCommand2 = new TestComponentCommand("test2");

        List<ComponentCommand> componentCommands = List.of(testComponentCommand1, testComponentCommand2);

        JdaClient.onComponentEventHandler("test21_button00000000-0000-0000-0000-000000000000", event, componentCommands, Schedulers.immediate());

        assertThat(testComponentCommand1.isCalled).isFalse();
        assertThat(testComponentCommand2.isCalled).isTrue();
    }

    static class TestComponentCommand implements ComponentCommand {

        final String id;
        boolean isCalled = false;

        TestComponentCommand(String id) {
            this.id = id;
        }


        @Override
        public Mono<Void> handleComponentInteractEvent(ButtonEventAdaptor event) {
            isCalled = true;
            return Mono.empty();
        }

        @Override
        public String getCommandId() {
            return id;
        }

    }
}