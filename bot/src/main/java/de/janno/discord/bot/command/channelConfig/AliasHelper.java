package de.janno.discord.bot.command.channelConfig;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistenceManager;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
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

        //specific before general, because the user specific have the higher priority and by applying it stop the application of the general alias
        for (Alias alias : userAlias) {
            expressionWithOptionalLabelsAndAppliedAliases = applyAliasIfMatch(alias, expressionWithOptionalLabelsAndAppliedAliases, "userChannel");
        }

        for (Alias alias : channelAlias) {
            expressionWithOptionalLabelsAndAppliedAliases = applyAliasIfMatch(alias, expressionWithOptionalLabelsAndAppliedAliases, "channel");
        }

        return expressionWithOptionalLabelsAndAppliedAliases;
    }

    private static String applyAliasIfMatch(Alias alias, String input, String scope) {
        switch (alias.getType()) {
            case Regex -> {
                Pattern pattern = Pattern.compile(alias.getName());
                Matcher matcher = pattern.matcher(input);

                if (matcher.find()) {
                    BotMetrics.incrementAliasUseMetricCounter(scope, Alias.Type.Regex, alias.toString());
                    try {
                        return matcher.replaceAll(alias.getValue());
                    } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                        log.warn("regex error with alias:{}, input:{}", alias.getName(), input, e);
                        //invalid group count or named group in the replacement
                        return input;
                    }
                } else {
                    return input; // No match found, return the original string
                }
            }
            case Replace -> {
                if (input.contains(alias.getName())) {
                    BotMetrics.incrementAliasUseMetricCounter(scope, Alias.Type.Replace, alias.toString());
                    return input.replace(alias.getName(), alias.getValue());
                } else {
                    return input;
                }
            }
        }
        throw new RuntimeException("Unknown alias: " + alias.getName());

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
