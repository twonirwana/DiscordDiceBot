package de.janno.discord.bot.command;

import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import org.apache.commons.lang3.NotImplementedException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class TestCommand extends AbstractCommand<Config, StateData> {
    protected TestCommand(PersistenceManager persistenceManager) {
        super(persistenceManager);
        setMessageDataDeleteDuration(Duration.ofMillis(10));
    }

    @Override
    protected ConfigAndState<Config, StateData> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO, @NonNull MessageDataDTO messageDataDTO, @NonNull String buttonValue, @NonNull String invokingUserName) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, long guildId, long channelId, @NonNull Config config) {
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
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(UUID configId, Config config, State<StateData> state, long guildId, long channelId) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(Config config, State<StateData> state, long channelId, long userId) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(UUID configId, Config config) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    protected @NonNull Config getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public @NonNull String getCommandId() {
        throw new NotImplementedException("Not implemented");
    }
}
