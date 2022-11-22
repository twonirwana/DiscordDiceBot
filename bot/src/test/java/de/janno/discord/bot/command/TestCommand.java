package de.janno.discord.bot.command;

import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class TestCommand extends AbstractCommand<Config, StateData> {
    protected TestCommand(MessageDataDAO messageDataDAO) {
        super(messageDataDAO);
        setMessageDataDeleteDuration(Duration.ofMillis(10));
    }

    @Override
    protected Optional<ConfigAndState<Config, StateData>> getMessageDataAndUpdateWithButtonValue(long channelId, long messageId, @NonNull String buttonValue, @NonNull String invokingUserName) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID, long guildId, long channelId, long messageId, @NonNull Config config, @Nullable State<StateData> state) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    protected @NonNull String getCommandDescription() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(Config config, State<StateData> state) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(Config config, State<StateData> state) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(Config config) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    protected @NonNull Config getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    protected @NonNull Config getConfigFromEvent(@NonNull ButtonEventAdaptor event) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    protected @NonNull State<StateData> getStateFromEvent(@NonNull ButtonEventAdaptor event) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public String getCommandId() {
        throw new NotImplementedException("Not implemented");
    }
}
