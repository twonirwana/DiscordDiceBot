package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractCommand<C extends IConfig, S extends IState> implements ISlashCommand, IComponentInteractEventHandler {

    protected static final String ACTION_START = "start";
    protected static final String ACTION_HELP = "help";
    protected static final String ANSWER_TARGET_CHANNEL_NAME = "target_channel";
    protected static final CommandDefinitionOption ANSWER_TARGET_CHANNEL_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(ANSWER_TARGET_CHANNEL_NAME)
            .description("The channel where the answer will be given")
            .type(CommandDefinitionOption.Type.CHANNEL)
            .build();
    protected final ButtonMessageCache buttonMessageCache;

    protected AbstractCommand(ButtonMessageCache buttonMessageCache) {
        this.buttonMessageCache = buttonMessageCache;
    }

    protected Optional<Long> getAnswerTargetChannelIdFromStartCommandOption(CommandInteractionOption options) {
        return options.getChannelIdSubOptionWithName(ANSWER_TARGET_CHANNEL_NAME);
    }

    protected Long getOptionalLongFromArray(String[] optionArray, int index) {
        if (optionArray.length >= index + 1 && !Strings.isNullOrEmpty(optionArray[index])) {
            return Long.parseLong(optionArray[index]);
        }
        return null;
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return buttonCustomId.matches("^" + getName() + BotConstants.CONFIG_SPLIT_DELIMITER_REGEX + ".*");
    }

    @VisibleForTesting
    Map<Long, Set<ButtonMessageCache.ButtonWithConfigHash>> getButtonMessageCache() {
        return buttonMessageCache.getCacheContent();
    }

    protected abstract Optional<Long> getAnswerTargetChannelId(C config);

    @Override
    public CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getName())
                .description(getCommandDescription())
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_START)
                        .description("Start")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .options(getStartOptions())
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_HELP)
                        .description("Help")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .build()
                )
                .build();
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull IButtonEventAdaptor event) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        C config = getConfigFromEvent(event);
        Long answerTargetChannelId = getAnswerTargetChannelId(config).orElse(null);
        Optional<String> checkPermissions = event.checkPermissions(answerTargetChannelId);
        if (checkPermissions.isPresent()) {
            return event.editMessage(checkPermissions.get());
        }

        //adding the message of the event to the cache, in the case that the bot was restarted and has forgotten the button
        long messageId = event.getMessageId();
        long channelId = event.getChannelId();
        buttonMessageCache.addChannelWithButton(channelId, messageId, config.hashCode());

        S state = getStateFromEvent(event);

        //all the answer actions
        List<Mono<Void>> actions = new ArrayList<>();
        actions.add(event.acknowledge());
        //the delete action must be the last action
        Mono<Void> deleteAction = Mono.empty();
        boolean keepExistingButtonMessage = shouldKeepExistingButtonMessage(event);
        String editMessage;

        if (keepExistingButtonMessage) {
            //if the old button is pined, the old message will be edited or reset to the slash default
            editMessage = getEditButtonMessage(state, config).orElse(getButtonMessage(config).getContent());
        } else {
            //edit the current message if the command changes it or mark it as processing
            editMessage = getEditButtonMessage(state, config).orElse("processing ...");
        }
        actions.add(event.editMessage(editMessage));

        Optional<EmbedDefinition> answer = getAnswer(state, config);
        if (answer.isPresent()) {
            BotMetrics.incrementButtonMetricCounter(getName(), config.toShortString());

            actions.add(event.createResultMessageWithEventReference(answer.get(), answerTargetChannelId).then(
                    event.getRequester()
                            .doOnNext(requester -> log.info("'{}'.'{}' from '{}' button: '{}'={}{} -> {} in {}ms",
                                            requester.getGuildName(),
                                            requester.getChannelName(),
                                            requester.getUserName(),
                                            event.getCustomId().replace(BotConstants.CONFIG_DELIMITER, ","),
                                            config.toShortString(),
                                            state.toShortString(),
                                            answer.get().toShortString(),
                                            stopwatch.elapsed(TimeUnit.MILLISECONDS)
                                    )
                            ).ofType(Void.class)));
        }
        Optional<MessageDefinition> newButtonMessage = getButtonMessageWithState(state, config);

        //todo don't create new message if the answer is in another channel
        if (newButtonMessage.isPresent()) {
            Mono<Long> newMessageIdMono = event.createButtonMessage(newButtonMessage.get())
                    .map(m -> {
                        buttonMessageCache.addChannelWithButton(channelId, m, config.hashCode());
                        return m;
                    });


            if (keepExistingButtonMessage) {
                //removing from cache on pin event would be better?
                //if the message was not removed, we don't want that it is removed later
                buttonMessageCache.removeButtonFromChannel(channelId, messageId, config.hashCode());
            }

            deleteAction = newMessageIdMono
                    .flux()
                    .flatMap(id -> Flux.fromIterable(buttonMessageCache.getAllWithoutOneAndRemoveThem(channelId, id, config.hashCode())))
                    .flatMap(event::deleteMessage)
                    .then();
        }

        return Flux.merge(1, actions.toArray(new Mono<?>[0]))
                .parallel()
                .then()
                .then(deleteAction);
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ISlashEventAdaptor event) {
        Optional<String> checkPermissions = event.checkPermissions();
        if (checkPermissions.isPresent()) {
            return event.reply(checkPermissions.get());
        }

        String commandString = event.getCommandString();
        Optional<CommandInteractionOption> startOption = event.getOption(ACTION_START);
        if (startOption.isPresent()) {
            CommandInteractionOption options = startOption.get();

            Optional<Long> answerTargetChannelId = getAnswerTargetChannelIdFromStartCommandOption(options);
            if (answerTargetChannelId.isPresent() && !event.isValidAnswerChannel(answerTargetChannelId.get())) {
                log.info("Invalid answer target channel for {}", commandString);
                return event.reply("The target channel is not a valid message channel");
            }

            Optional<String> validationMessage = getStartOptionsValidationMessage(options);
            if (validationMessage.isPresent()) {
                log.info("Validation message: {} for {}", validationMessage.get(), commandString);
                return event.reply(String.format("%s\n%s", commandString, validationMessage.get()));
            }
            C config = getConfigFromStartOptions(options);
            BotMetrics.incrementSlashStartMetricCounter(getName(), config.toShortString());

            long channelId = event.getChannelId();

            return event.reply(commandString)
                    .then(event.createButtonMessage(getButtonMessage(config))
                            .map(m -> {
                                buttonMessageCache.addChannelWithButton(channelId, m, config.hashCode());
                                return m;
                            })
                            .flux()
                            .flatMap(id -> Flux.fromIterable(buttonMessageCache.getAllWithoutOneAndRemoveThem(channelId, id, config.hashCode())))
                            .flatMap(event::deleteMessage).then())
                    .then(event.getRequester()
                            .doOnNext(requester -> log.info("'{}'.'{}' from '{}' slash: '{}'",
                                    requester.getGuildName(),
                                    requester.getChannelName(),
                                    requester.getUserName(),
                                    commandString
                            ))
                            .ofType(Void.class));

        } else if (event.getOption(ACTION_HELP).isPresent()) {
            BotMetrics.incrementSlashHelpMetricCounter(getName());
            return event.replyEmbed(getHelpMessage(), true);
        }
        return Mono.empty();
    }

    protected List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of();
    }

    protected abstract @NonNull String getCommandDescription();

    protected abstract EmbedDefinition getHelpMessage();

    /**
     * The text content for the old button message, after a button event. Returns null means no editing should be done.
     */
    protected Optional<String> getEditButtonMessage(S state, C config) {
        return Optional.empty();
    }

    /**
     * The new button message, after a button event
     */
    protected abstract Optional<MessageDefinition> getButtonMessageWithState(S state, C config);

    protected abstract Optional<EmbedDefinition> getAnswer(S state, C config);

    /**
     * The new button message, after a slash event
     */
    protected abstract MessageDefinition getButtonMessage(C config);

    protected Optional<String> getStartOptionsValidationMessage(CommandInteractionOption options) {
        //standard is no validation
        return Optional.empty();
    }

    protected boolean shouldKeepExistingButtonMessage(IButtonEventAdaptor event) {
        return event.isPinned();
    }

    protected abstract C getConfigFromStartOptions(CommandInteractionOption options);

    protected abstract C getConfigFromEvent(IButtonEventAdaptor event);

    protected abstract S getStateFromEvent(IButtonEventAdaptor event);

}
