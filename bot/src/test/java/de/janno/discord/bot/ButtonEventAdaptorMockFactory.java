package de.janno.discord.bot;

import com.google.common.collect.Sets;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.persistance.MessageDataDAO;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static de.janno.discord.bot.ButtonEventAdaptorMock.CHANNEL_ID;
import static de.janno.discord.bot.ButtonEventAdaptorMock.GUILD_ID;

public class ButtonEventAdaptorMockFactory<C extends Config, S extends StateData> {
    private final String customId;
    private final AtomicLong messageIdCounter;
    private final Set<Long> pinnedMessageIds;

    public ButtonEventAdaptorMockFactory(String customId, AbstractCommand<C, S> command, C config, MessageDataDAO messageDataDAO, boolean firstMessagePinned) {
        this.customId = customId;
        this.messageIdCounter = new AtomicLong(0);
        this.pinnedMessageIds = firstMessagePinned ? Sets.newHashSet(messageIdCounter.get()) : Collections.emptySet();
        command.createMessageDataForNewMessage(UUID.randomUUID(), GUILD_ID, CHANNEL_ID, messageIdCounter.get(), config, null).ifPresent(messageDataDAO::saveMessageData);
    }

    public ButtonEventAdaptorMock getButtonClickOnLastButtonMessage(String buttonValue) {
        return new ButtonEventAdaptorMock(customId, buttonValue, messageIdCounter, pinnedMessageIds);
    }

    public ButtonEventAdaptorMock getButtonClickOnLastButtonMessage(String buttonValue, String invokingUser) {
        return new ButtonEventAdaptorMock(customId, buttonValue, messageIdCounter, pinnedMessageIds, invokingUser);
    }
}