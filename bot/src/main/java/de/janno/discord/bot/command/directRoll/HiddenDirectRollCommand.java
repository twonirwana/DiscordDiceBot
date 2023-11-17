package de.janno.discord.bot.command.directRoll;

import com.google.common.base.Stopwatch;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.command.RollAnswerConverter;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.ComponentInteractEventHandler;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HiddenDirectRollCommand extends DirectRollCommand implements ComponentInteractEventHandler {

    public static final String ROLL_COMMAND_ID = "h";
    private static final String REVEAL_BUTTON_ID = "reveal";


    public HiddenDirectRollCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        super(persistenceManager, cachingDiceEvaluator);
    }

    @Override
    public @NonNull String getCommandId() {
        return ROLL_COMMAND_ID;
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                //todo i18n
                .description("hidden direct roll of dice expression, configuration with /channel_config")
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_EXPRESSION)
                        .required(true)
                        .description("dice expression, e.g. '2d6'")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build();
    }

    @Override
    protected @NonNull Mono<Void> createResponse(@NonNull SlashEventAdaptor event,
                                                 @NonNull String commandString,
                                                 @NonNull String diceExpression,
                                                 @NonNull RollAnswer answer,
                                                 @NonNull Stopwatch stopwatch) {
        EmbedOrMessageDefinition embedOrMessageDefinition = RollAnswerConverter.toEmbedOrMessageDefinition(answer).toBuilder()
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .buttonDefinition(ButtonDefinition.builder()
                                .id(BottomCustomIdUtils.createButtonCustomIdWithoutConfigId(getCommandId(), REVEAL_BUTTON_ID))
                                .label("Reveal").build())
                        .build()
                )
                .build();
        return Flux.merge(1, event.replyWithEmbedOrMessageDefinition(embedOrMessageDefinition, true)
                                .doOnSuccess(v ->
                                        log.info("{}: '{}'={} -> {} in {}ms",
                                                event.getRequester().toLogString(),
                                                commandString.replace("`", ""),
                                                diceExpression,
                                                answer.toShortString(),
                                                stopwatch.elapsed(TimeUnit.MILLISECONDS)
                                        )),
                        event.createResultMessageWithReference(EmbedOrMessageDefinition.builder()
                                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                                .descriptionOrContent("Made a hidden roll")
                                .build()))
                .parallel()
                .then();
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        //only one button so we don't check the button id
        return Flux.merge(1,
                        event.acknowledgeAndRemoveButtons(), //ephemeral message cant be deleted
                        event.createResultMessageWithReference(event.getMessageDefinitionOfEventMessageWithoutButtons(), null)
                                .doOnSuccess(v ->
                                        log.info("{}:-> {} in {}ms",
                                                event.getRequester().toLogString(),
                                                REVEAL_BUTTON_ID,
                                                stopwatch.elapsed(TimeUnit.MILLISECONDS)
                                        ))
                )
                .parallel()
                .then();
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return Objects.equals(getCommandId(), BottomCustomIdUtils.getCommandNameFromCustomId(buttonCustomId));
    }
}
