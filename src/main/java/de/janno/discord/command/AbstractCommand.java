package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.Metrics;
import de.janno.discord.cache.ActiveButtonsCache;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.discord4j.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static de.janno.discord.dice.DiceUtils.MINUS;

@Slf4j
public abstract class AbstractCommand<C extends IConfig, S extends IState> implements ISlashCommand, IComponentInteractEventHandler {

    protected static final String ACTION_START = "start";
    protected static final String ACTION_HELP = "help";
    protected static final String CONFIG_DELIMITER = ",";
    protected final ActiveButtonsCache activeButtonsCache;

    protected AbstractCommand(ActiveButtonsCache activeButtonsCache) {
        this.activeButtonsCache = activeButtonsCache;
    }

    protected abstract String createButtonCustomId(String system, String value, C config, @Nullable S state);

    protected List<ApplicationCommandOptionData> getStartOptions() {
        return ImmutableList.of();
    }

    protected abstract String getCommandDescription();

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
        activeButtonsCache.addChannelWithButton(event.getChannelId(), event.getMessageId(), config.hashCode());

        S state = getStateFromEvent(event);
        List<Mono<Void>> actions = new ArrayList<>();
        boolean triggeringMessageIsPinned = event.isPinned();

        actions.add(event
                //if the button is pined it keeps its message
                .editMessage(editMessage(state, config)));
        if (createAnswerMessage(state, config)) {
            Metrics.incrementButtonMetricCounter(getName(), config.toMetricString());
            actions.add(createButtonEventAnswer(event, config));
        }
        if (copyButtonMessageToTheEnd(state, config)) {
            actions.add(event.moveButtonMessage(triggeringMessageIsPinned,
                    editMessage(state, config),
                    getButtonMessage(state, config),
                    activeButtonsCache,
                    getButtonLayout(state, config),
                    config.hashCode()));
        }

        return Flux.mergeDelayError(1, actions.toArray(new Mono<?>[0]))
                .parallel()
                .then();
    }

    protected Mono<Void> createButtonEventAnswer(@NonNull IButtonEventAdaptor event, @NonNull C config) {
        List<DiceResult> result = getDiceResult(getStateFromEvent(event), config);
        result.forEach(d -> log.info(String.format("%s:%s -> %s: %s", getName(), config, d.getResultTitle(), d.getResultDetails()
                .replace("▢", "0")
                .replace("＋", "+")
                .replace(MINUS, "-")
                .replace("*", ""))));
        return event.createResultMessageWithEventReference(result);
    }

    //default is to leave the message unaltered
    //TODO can be combinded mit buttonMessage?
    protected String editMessage(S state, C config) {
        return getButtonMessage(state, config);
    }

    protected boolean createAnswerMessage(S state, C config) {
        return true;
    }

    protected boolean copyButtonMessageToTheEnd(S state, C config) {
        return true;
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ISlashEventAdaptor event) {

        if (event.getOption(ACTION_START).isPresent()) {
            ApplicationCommandInteractionOption options = event.getOption(ACTION_START).get();
            String validationMessage = getStartOptionsValidationMessage(options);
            if (validationMessage != null) {
                log.info("Validation message: {}", validationMessage);
                return event.reply(validationMessage);
            }
            C config = getConfigValuesFromStartOptions(options);
            Metrics.incrementSlashStartMetricCounter(getName(), config.toMetricString());

            return event.reply("...")
                    .then(event.createButtonMessage(activeButtonsCache,
                            getButtonMessage(null, config),
                            getButtonLayout(null, config),
                            config.hashCode()));

        } else if (event.getOption(ACTION_HELP).isPresent()) {
            Metrics.incrementSlashHelpMetricCounter(getName());
            return event.replyEphemeral(getHelpMessage());
        }
        return Mono.empty();
    }

    protected abstract EmbedCreateSpec getHelpMessage();

    protected abstract String getButtonMessage(@Nullable S state, C config);

    protected abstract C getConfigValuesFromStartOptions(ApplicationCommandInteractionOption options);

    protected abstract List<DiceResult> getDiceResult(S state, C config);

    protected abstract List<LayoutComponent> getButtonLayout(@Nullable S state, C config);

    protected String getStartOptionsValidationMessage(ApplicationCommandInteractionOption options) {
        return null;
    }

    protected abstract C getConfigFromEvent(IButtonEventAdaptor event);

    protected abstract S getStateFromEvent(IButtonEventAdaptor event);


}
