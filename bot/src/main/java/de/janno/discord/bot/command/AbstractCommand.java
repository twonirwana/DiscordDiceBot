package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


import java.util.*;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractCommand<C extends RollConfig, S extends StateData> implements SlashCommand, ComponentCommand {

    protected final PersistenceManager persistenceManager;
    private final ComponentCommandImpl<C, S> componentCommand;
    private final SlashCommandImpl<C> slashCommand;

    protected AbstractCommand(PersistenceManager persistenceManager, Supplier<UUID> uuidSupplier) {
        this.persistenceManager = persistenceManager;
        componentCommand = new ComponentCommandImpl<>(persistenceManager, uuidSupplier) {

            @Override
            public @NonNull String getCommandId() {
                return AbstractCommand.this.getCommandId();
            }

            @Override
            protected ConfigAndState<C, S> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO, @NonNull MessageDataDTO messageDataDTO, @NonNull String buttonValue, @NonNull String invokingUserName) {
                return AbstractCommand.this.getMessageDataAndUpdateWithButtonValue(messageConfigDTO, messageDataDTO, buttonValue, invokingUserName);
            }

            @Override
            protected @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessage(@NonNull UUID configId, @NonNull C config, State<S> state, Long guildId, long channelId, long userId) {
                return AbstractCommand.this.createNewButtonMessageWithState(configId, config, state, guildId, channelId, userId);
            }

            @Override
            protected @NonNull Optional<RollAnswer> getAnswer(C config, State<S> state, long channelId, long userId) {
                return AbstractCommand.this.getAnswer(config, state, channelId, userId);
            }

            @Override
            protected void updateCurrentMessageStateData(UUID configUUID, Long guildId, long channelId, long messageId, @NonNull C config, @NonNull State<S> state) {
                AbstractCommand.this.updateCurrentMessageStateData(configUUID, guildId, channelId, messageId, config, state);
            }

            @Override
            protected Optional<String> replyEphemeralMessage(@NonNull C config, @NonNull State<S> state, String invokingUserName) {
                return AbstractCommand.this.replyEphemeralMessage(config, state, invokingUserName);
            }

            @Override
            protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID configUUID, C config, State<S> state, long channelId, long userId, boolean keepExistingButtonMessage) {
                return AbstractCommand.this.getCurrentMessageComponentChange(configUUID, config, state, channelId, userId, keepExistingButtonMessage);
            }

            @Override
            protected Optional<ConfigAndState<C, S>> createNewConfigAndStateIfMissing(String buttonValue) {
                return AbstractCommand.this.createNewConfigAndStateIfMissing(buttonValue);
            }

            @Override
            protected boolean shouldKeepExistingButtonMessage(@NonNull ButtonEventAdaptor event) {
                return AbstractCommand.this.shouldKeepExistingButtonMessage(event);
            }

            @Override
            public @NonNull Optional<String> getCurrentMessageContentChange(C config, State<S> state, boolean keepExistingButtonMessage) {
                return AbstractCommand.this.getCurrentMessageContentChange(config, state, keepExistingButtonMessage);
            }
        };
        slashCommand = new SlashCommandImpl<>(persistenceManager) {
            @Override
            protected @NonNull C getConfigFromStartOptions(@NonNull CommandInteractionOption options, @NonNull Locale userLocale) {
                return AbstractCommand.this.getConfigFromStartOptions(options, userLocale);
            }

            @Override
            public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, Long guildId, long channelId, long userId, @NonNull C config) {
                return AbstractCommand.this.createMessageConfig(configUUID, guildId, channelId, userId, config);
            }

            @Override
            public @NonNull EmbedOrMessageDefinition createSlashResponseMessage(@NonNull UUID configId, @NonNull C config, long channelId) {
                return AbstractCommand.this.createSlashResponseMessage(configId, config, channelId);
            }

            @Override
            protected @NonNull EmbedOrMessageDefinition getHelpMessage(Locale userLocale) {
                return AbstractCommand.this.getHelpMessage(userLocale);
            }

            @Override
            public @NonNull String getCommandId() {
                return AbstractCommand.this.getCommandId();
            }

            @Override
            protected boolean supportsResultImages() {
                return AbstractCommand.this.supportsResultImages();
            }

            @Override
            protected boolean supportsAnswerFormat() {
                return AbstractCommand.this.supportsAnswerFormat();
            }

            @Override
            protected boolean supportsAnswerInteraction() {
                return AbstractCommand.this.supportsAnswerInteraction();
            }

            @Override
            protected boolean supportsTargetChannel() {
                return AbstractCommand.this.supportsTargetChannel();
            }

            @Override
            protected Collection<CommandDefinitionOption> additionalCommandOptions() {
                return AbstractCommand.this.additionalCommandOptions();
            }

            @Override
            protected @NonNull Optional<String> getConfigWarnMessage(C config, Locale userLocale) {
                return AbstractCommand.this.getConfigWarnMessage(config, userLocale);
            }

            @Override
            protected @NonNull List<CommandDefinitionOption> getStartOptions() {
                return AbstractCommand.this.getStartOptions();
            }

            @Override
            public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest autoCompleteRequest, @NonNull Locale userLocale, long channelId, Long guildId, long userId) {
                return AbstractCommand.this.getAutoCompleteAnswer(autoCompleteRequest, userLocale, channelId, guildId, userId);
            }

            @Override
            protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options, long channelId, long userId, @NonNull Locale userLocale) {
                return AbstractCommand.this.getStartOptionsValidationMessage(options, channelId, userId, userLocale);
            }
        };
    }

    protected Optional<String> replyEphemeralMessage(@NonNull C config, @NonNull State<S> state, String invokingUserName) {
        return Optional.empty();
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return slashCommand.getCommandDefinition();
    }

    protected boolean supportsResultImages() {
        return true;
    }

    protected boolean supportsAnswerFormat() {
        return true;
    }

    protected boolean supportsTargetChannel() {
        return true;
    }

    protected boolean supportsAnswerInteraction() {
        return true;
    }

    protected Collection<CommandDefinitionOption> additionalCommandOptions() {
        return Collections.emptyList();
    }

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest autoCompleteRequest, @NonNull Locale userLocale, long channelId, Long guildId, long userId) {
        return BaseCommandOptions.autoCompleteColorOption(autoCompleteRequest, userLocale);
    }

    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID configUUID, C config, State<S> state, long channelId, long userId, boolean keepExistingButtonMessage) {
        return Optional.empty();
    }

    protected abstract ConfigAndState<C, S> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                   @NonNull MessageDataDTO messageDataDTO,
                                                                                   @NonNull String buttonValue,
                                                                                   @NonNull String invokingUserName);


    //visible for welcome command
    public abstract Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID,
                                                                   Long guildId,
                                                                   long channelId,
                                                                   long userId,
                                                                   @NonNull C config);

    /**
     * update the saved state if the current button message is not deleted. StateData need to be set to null if the there is a answer message
     */
    protected void updateCurrentMessageStateData(UUID configUUID, Long guildId, long channelId, long messageId, @NonNull C config, @NonNull State<S> state) {
    }

    /**
     * Creates a config and state if there is no saved config for a button event
     */
    protected Optional<ConfigAndState<C, S>> createNewConfigAndStateIfMissing(String buttonValue) {
        return Optional.empty();
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event) {
        return componentCommand.handleComponentInteractEvent(event);
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocale) {
        return slashCommand.handleSlashCommandEvent(event, uuidSupplier, userLocale);
    }

    protected @NonNull Optional<String> getConfigWarnMessage(C config, Locale userLocale) {
        return Optional.empty();
    }

    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of();
    }

    protected abstract @NonNull EmbedOrMessageDefinition getHelpMessage(Locale userLocale);

    /**
     * The text content for the old button message, after a button event. Returns null means no editing should be done.
     */
    @VisibleForTesting
    public @NonNull Optional<String> getCurrentMessageContentChange(C config, State<S> state, boolean keepExistingButtonMessage) {
        return Optional.empty();
    }

    /**
     * The new button message, after a button event
     */
    protected abstract @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessageWithState(@NonNull UUID configId,
                                                                                                   @NonNull C config,
                                                                                                   State<S> state,
                                                                                                   Long guildId,
                                                                                                   long channelId,
                                                                                                   long userId);

    protected abstract @NonNull Optional<RollAnswer> getAnswer(C config, State<S> state, long channelId, long userId);

    /**
     * The new button message, after a slash event
     */
    public abstract @NonNull EmbedOrMessageDefinition createSlashResponseMessage(@NonNull UUID configId, @NonNull C config, long channelId);

    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options, long channelId, long userId, @NonNull Locale userLocale) {
        //standard is no validation
        return Optional.empty();
    }

    protected boolean shouldKeepExistingButtonMessage(@NonNull ButtonEventAdaptor event) {
        return event.isPinned();
    }

    protected abstract @NonNull C getConfigFromStartOptions(@NonNull CommandInteractionOption options, @NonNull Locale userLocale);
}
