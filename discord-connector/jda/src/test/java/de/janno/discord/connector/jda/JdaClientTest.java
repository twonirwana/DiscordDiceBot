package de.janno.discord.connector.jda;

import com.google.common.base.Stopwatch;
import de.janno.discord.connector.api.ChildrenChannelCreationEvent;
import io.avaje.config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.ChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.IThreadContainerUnion;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.managers.ApplicationManager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        Member member = mock(Member.class);
        when(guild.getSelfMember()).thenReturn(member);
        when(member.hasPermission(newsChannel, Permission.MESSAGE_SEND)).thenReturn(true);
        when(member.hasPermission(newsChannel, Permission.MESSAGE_MANAGE)).thenReturn(true);
        MessageCreateAction messageCreateAction = mock(MessageCreateAction.class);
        when(newsChannel.sendMessage(anyString())).thenReturn(messageCreateAction);
        when(messageCreateAction.complete()).thenReturn(mock(Message.class));
        when(newsChannel.crosspostMessageById(any())).thenReturn(mock(RestAction.class));


        List<JDA> shards = JdaClient.waitingForShardStartAndSendStatus(shardManager, Set.of(), Set.of(), Stopwatch.createStarted());

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
}