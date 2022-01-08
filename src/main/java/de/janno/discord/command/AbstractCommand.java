package de.janno.discord.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import de.janno.discord.Metrics;
import de.janno.discord.cache.ButtonMessageCache;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.discord4j.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.janno.discord.dice.DiceUtils.MINUS;

@Slf4j
public abstract class AbstractCommand<C extends IConfig, S extends IState> implements ISlashCommand, IComponentInteractEventHandler {

    protected static final String ACTION_START = "start";
    protected static final String ACTION_HELP = "help";
    protected static final String CONFIG_DELIMITER = ",";
    protected final ButtonMessageCache buttonMessageCache;

    protected AbstractCommand(ButtonMessageCache buttonMessageCache) {
        this.buttonMessageCache = buttonMessageCache;
    }

    @VisibleForTesting
    Map<Long, Set<ButtonMessageCache.ButtonWithConfigHash>> getButtonMessageCache() {
        return buttonMessageCache.getCacheContent();
    }

    @Override
    public ApplicationCommand getApplicationCommand() {
        return ApplicationCommand.builder()
                .name(getName())
                .description(getCommandDescription())
                .option(ApplicationCommandOptionData.builder()
                        .name(ACTION_START)
                        .description("Start")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addAllOptions(getStartOptions())
                        .build())
                .option(ApplicationCommandOptionData.builder()
                        .name(ACTION_HELP)
                        .description("Help")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .build()
                )
                .build();
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull IButtonEventAdaptor event) {
        C config = getConfigFromEvent(event);
        //adding the message of the event to the cache, in the case that the bot was restarted and has forgotten the button
        Long messageId = event.getMessageId();
        Long channelId = event.getChannelId();
        buttonMessageCache.addChannelWithButton(channelId, messageId, config.hashCode());

        S state = getStateFromEvent(event);
        List<Mono<Void>> actions = new ArrayList<>();
        boolean triggeringMessageIsPinned = event.isPinned();
        String editMessage;

        if (triggeringMessageIsPinned) {
            //if the old button is pined, the old message will be edited or reset to the slash default
            editMessage = getEditButtonMessage(state, config) != null ? getEditButtonMessage(state, config) : getButtonMessage(config);
        } else {
            //edit the current message if the command changes it or mark it as processing
            editMessage = getEditButtonMessage(state, config) != null ? getEditButtonMessage(state, config) : "processing ...";
        }
        actions.add(event.editMessage(editMessage));
        if (createAnswerMessage(state, config)) {
            Metrics.incrementButtonMetricCounter(getName(), config.toShortString());
            List<DiceResult> result = getDiceResult(state, config);
            result.forEach(d -> log.info(String.format("%s:%s -> %s: %s", getName(), config.toShortString(), d.getResultTitle(), d.getResultDetails()
                    .replace("▢", "0")
                    .replace("＋", "+")
                    .replace(MINUS, "-")
                    .replace("*", ""))));
            actions.add(event.createResultMessageWithEventReference(result));
        }
        if (copyButtonMessageToTheEnd(state, config)) {
            Mono<Long> newMessageIdMono = event.createButtonMessage(getButtonMessageWithState(state, config), getButtonLayoutWithState(state, config))
                    .map(m -> {
                        buttonMessageCache.addChannelWithButton(channelId, m, config.hashCode());
                        return m;
                    });


            if (triggeringMessageIsPinned) {
                //removing from cache on pin event would be better but currently not possible with discord4j
                //if the message was not removed, we don't want that it is removed later
                buttonMessageCache.removeButtonFromChannel(channelId, messageId, config.hashCode());
            }

            actions.add(newMessageIdMono
                    .flux()
                    .flatMap(id -> Flux.fromIterable(buttonMessageCache.getAllWithoutOneAndRemoveThem(channelId, id, config.hashCode())))
                    .flatMap(event::deleteMessage)
                    .then());
        }

        return Flux.mergeDelayError(1, actions.toArray(new Mono<?>[0]))
                .parallel()
                .then();
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ISlashEventAdaptor event) {
        String checkPermissions = event.checkPermissions();
        if (checkPermissions != null) {
            return event.reply(checkPermissions);
        }

        String commandString = event.getCommandString();
        log.info("Application command: {}", commandString);
        if (event.getOption(ACTION_START).isPresent()) {
            ApplicationCommandInteractionOption options = event.getOption(ACTION_START).get();
            String validationMessage = getStartOptionsValidationMessage(options);
            if (validationMessage != null) {
                log.info("Validation message: {}", validationMessage);
                return event.reply(String.format("%s\n%s", commandString, validationMessage));
            }
            C config = getConfigFromStartOptions(options);
            Metrics.incrementSlashStartMetricCounter(getName(), config.toShortString());


            return event.reply(commandString)
                    .then(event.createButtonMessage(getButtonMessage(config), getButtonLayout(config))
                            .map(m -> {
                                buttonMessageCache.addChannelWithButton(event.getChannelId(), m, config.hashCode());
                                return m;
                            }).ofType(Void.class));

        } else if (event.getOption(ACTION_HELP).isPresent()) {
            Metrics.incrementSlashHelpMetricCounter(getName());
            return event.replyEphemeral(getHelpMessage());
        }
        return Mono.empty();
    }

    protected List<ApplicationCommandOptionData> getStartOptions() {
        return ImmutableList.of();
    }

    protected abstract String getCommandDescription();

    protected abstract EmbedCreateSpec getHelpMessage();

    /**
     * if an answer message (without buttons) should be created
     */
    protected boolean createAnswerMessage(S state, C config) {
        return true;
    }

    /**
     * if the old button message should be moved
     */
    protected boolean copyButtonMessageToTheEnd(S state, C config) {
        return true;
    }

    /**
     * The text content for the old button message, after a button event. Returns null means no editing should be done.
     */
    protected String getEditButtonMessage(S state, C config) {
        return null;
    }

    /**
     * The text content for the new button message, after a button event
     */
    protected abstract String getButtonMessageWithState(S state, C config);

    /**
     * The text content for the new button message, after a slash event
     */
    protected abstract String getButtonMessage(C config);

    protected abstract List<DiceResult> getDiceResult(S state, C config);

    /**
     * The button layout for the new button message, after a button event
     */
    protected abstract List<LayoutComponent> getButtonLayoutWithState(S state, C config);

    /**
     * The button layout for the new button message, after a slash event
     */
    protected abstract List<LayoutComponent> getButtonLayout(C config);

    protected String getStartOptionsValidationMessage(ApplicationCommandInteractionOption options) {
        //standard is no validation
        return null;
    }

    protected abstract C getConfigFromStartOptions(ApplicationCommandInteractionOption options);

    protected abstract C getConfigFromEvent(IButtonEventAdaptor event);

    protected abstract S getStateFromEvent(IButtonEventAdaptor event);

}
