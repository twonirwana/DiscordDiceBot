package de.janno.discord.bot.command.channelConfig;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistenceManager;
import lombok.NonNull;
import javax.annotation.Nullable;

import java.util.List;

public class AliasHelper {
    public static final String CHANNEL_ALIAS_CONFIG_TYPE_ID = "AliasConfig";
    public static final String USER_ALIAS_CONFIG_TYPE_ID = "UserAliasConfig";

    @VisibleForTesting
    static AliasConfig deserializeAliasConfig(ChannelConfigDTO channelConfigDTO) {
        Preconditions.checkArgument(CHANNEL_ALIAS_CONFIG_TYPE_ID.equals(channelConfigDTO.getConfigClassId()) || USER_ALIAS_CONFIG_TYPE_ID.equals(channelConfigDTO.getConfigClassId()), "Unknown configClassId: %s", channelConfigDTO.getConfigClassId());
        return Mapper.deserializeObject(channelConfigDTO.getConfig(), AliasConfig.class);
    }

    public static List<Alias> getChannelAlias(long channelId, PersistenceManager persistenceManager) {
        return persistenceManager.getChannelConfig(channelId, CHANNEL_ALIAS_CONFIG_TYPE_ID)
                .map(AliasHelper::deserializeAliasConfig)
                .map(AliasConfig::getAliasList)
                .orElse(List.of());
    }

    public static List<Alias> getUserChannelAlias(long channelId, long userId, PersistenceManager persistenceManager) {
        return persistenceManager.getUserChannelConfig(channelId, userId, USER_ALIAS_CONFIG_TYPE_ID)
                .map(AliasHelper::deserializeAliasConfig)
                .map(AliasConfig::getAliasList)
                .orElse(List.of());
    }

    static String applyAliaseToExpression(@NonNull List<Alias> channelAlias, @NonNull List<Alias> userAlias, final String expressionWithOptionalLabel) {
        if (channelAlias.isEmpty() && userAlias.isEmpty()) {
            return expressionWithOptionalLabel;
        }
        String expressionWithOptionalLabelsAndAppliedAliases = expressionWithOptionalLabel;

        //specific before general
        for (Alias alias : userAlias) {
            if (expressionWithOptionalLabelsAndAppliedAliases.contains(alias.getName())) {
                BotMetrics.incrementAliasUseMetricCounter("userChannel", alias.toString());
                expressionWithOptionalLabelsAndAppliedAliases = expressionWithOptionalLabelsAndAppliedAliases.replace(alias.getName(), alias.getValue());
            }
        }

        for (Alias alias : channelAlias) {
            if (expressionWithOptionalLabelsAndAppliedAliases.contains(alias.getName())) {
                BotMetrics.incrementAliasUseMetricCounter("channel", alias.toString());
                expressionWithOptionalLabelsAndAppliedAliases = expressionWithOptionalLabelsAndAppliedAliases.replace(alias.getName(), alias.getValue());
            }
        }

        return expressionWithOptionalLabelsAndAppliedAliases;
    }

    public static String getAndApplyAliaseToExpression(long channelId, @Nullable Long userId, PersistenceManager persistenceManager, final String expressionWithOptionalLabel) {
        final List<Alias> channelAlias = getChannelAlias(channelId, persistenceManager);
        final List<Alias> userChannelAlias;
        if (userId == null) {
            userChannelAlias = List.of();
        } else {
            userChannelAlias = getUserChannelAlias(channelId, userId, persistenceManager);
        }
        return applyAliaseToExpression(channelAlias, userChannelAlias, expressionWithOptionalLabel);
    }
}
