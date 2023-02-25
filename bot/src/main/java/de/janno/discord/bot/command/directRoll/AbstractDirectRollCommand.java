package de.janno.discord.bot.command.directRoll;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.SlashCommand;

import java.util.List;

public abstract class AbstractDirectRollCommand implements SlashCommand {
    protected static final String CONFIG_TYPE_ID = "DirectRollConfig";
    protected static final String ALIAS_CONFIG_TYPE_ID = "AliasConfig";
    protected static final String ROLL_COMMAND_ID = "r";
    protected final PersistenceManager persistenceManager;

    protected AbstractDirectRollCommand(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @VisibleForTesting
    AliasConfig deserializeAliasConfig(ChannelConfigDTO channelConfigDTO) {
        Preconditions.checkArgument(ALIAS_CONFIG_TYPE_ID.equals(channelConfigDTO.getConfigClassId()), "Unknown configClassId: %s", channelConfigDTO.getConfigClassId());
        return Mapper.deserializeObject(channelConfigDTO.getConfig(), AliasConfig.class);
    }

    protected List<Alias> getChannelAlias(long channelId) {
        return persistenceManager.getChannelConfig(channelId, ALIAS_CONFIG_TYPE_ID)
                .map(this::deserializeAliasConfig)
                .map(AliasConfig::getAliasList)
                .orElse(List.of());
    }

    protected List<Alias> getUserChannelAlias(long channelId, long userId) {
        return persistenceManager.getUserChannelConfig(channelId, userId, ALIAS_CONFIG_TYPE_ID)
                .map(this::deserializeAliasConfig)
                .map(AliasConfig::getAliasList)
                .orElse(List.of());
    }
}
