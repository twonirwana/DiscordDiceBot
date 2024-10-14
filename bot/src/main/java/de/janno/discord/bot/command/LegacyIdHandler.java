package de.janno.discord.bot.command;

import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.ComponentCommand;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.Set;

public class LegacyIdHandler implements ComponentCommand {

    private final static String COUNT_SUCCESSES_ID = "count_successes";
    private final static String FATE_ID = "fate";
    private final static String HOLD_REROLL_ID = "hold_reroll";
    private final static String POOL_TARGET_ID = "pool_target";
    private final static String SUM_DICE_SET_ID = "sum_dice_set";

    private final Set<String> ALL_LEGACY_IDS = Set.of(COUNT_SUCCESSES_ID, FATE_ID, HOLD_REROLL_ID, POOL_TARGET_ID, SUM_DICE_SET_ID);


    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event) {
        String buttonCommandId = BottomCustomIdUtils.getCommandNameFromCustomId(event.getCustomId());
        BotMetrics.incrementLegacyCommandButtonMetricCounter(buttonCommandId);
        return event.reply(I18n.getMessage("legacy.message", event.getRequester().getUserLocal()), false);
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        String buttonCommandId = BottomCustomIdUtils.getCommandNameFromCustomId(buttonCustomId);
        return ALL_LEGACY_IDS.contains(buttonCommandId);
    }

    @Override
    public @NonNull String getCommandId() {
        return "legacy";
    }
}
