package de.janno.discord.bot;

import com.google.common.collect.Sets;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.RollConfig;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.persistance.PersistenceManager;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static de.janno.discord.bot.ButtonEventAdaptorMock.*;

public class ButtonEventAdaptorMockFactory<C extends RollConfig, S extends StateData> {
    private final String commandId;
    private final AtomicLong messageIdCounter;
    private final Set<Long> pinnedMessageIds;
    private final UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public ButtonEventAdaptorMockFactory(String commandId, AbstractCommand<C, S> command, C config, PersistenceManager persistenceManager, boolean firstMessagePinned) {
        this.commandId = commandId;
        this.messageIdCounter = new AtomicLong(0);
        this.pinnedMessageIds = firstMessagePinned ? Sets.newHashSet(messageIdCounter.get()) : Collections.emptySet();
        command.createMessageConfig(configUUID, GUILD_ID, CHANNEL_ID, USER_ID, config).ifPresent(persistenceManager::saveMessageConfig);
        command.createEmptyMessageData(configUUID, GUILD_ID, CHANNEL_ID, messageIdCounter.get());
    }

    public ButtonEventAdaptorMock getButtonClickOnLastButtonMessage(String buttonValue) {
        return new ButtonEventAdaptorMock(commandId, buttonValue, configUUID, messageIdCounter, pinnedMessageIds);
    }

    public ButtonEventAdaptorMock getButtonClickOnLastButtonMessage(String buttonValue, String invokingUser) {
        return new ButtonEventAdaptorMock(commandId, buttonValue, configUUID, messageIdCounter, pinnedMessageIds, invokingUser);
    }
}