package de.janno.discord.bot.command.directRoll;

import com.google.common.base.Stopwatch;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.command.RollAnswerConverter;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.ComponentCommand;
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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HiddenDirectRollCommand extends DirectRollCommand implements ComponentCommand {

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
                .nameLocales(I18n.allNoneEnglishMessagesNames("h.name"))
                .description(I18n.getMessage("h.description", Locale.ENGLISH))
                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("h.description"))
                .option(CommandDefinitionOption.builder()
                        .name(expressionOptionName)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("r.expression.name"))
                        .description(I18n.getMessage("h.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("h.description"))
                        .required(true)
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build();
    }

    @Override
    protected @NonNull Mono<Void> createResponse(@NonNull SlashEventAdaptor event,
                                                 @NonNull String commandString,
                                                 @NonNull String diceExpression,
                                                 @NonNull RollAnswer answer,
                                                 @NonNull Stopwatch stopwatch,
                                                 @NonNull Locale userLocale) {
        EmbedOrMessageDefinition embedOrMessageDefinition = RollAnswerConverter.toEmbedOrMessageDefinition(answer).toBuilder()
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .componentDefinition(ButtonDefinition.builder()
                                .id(BottomCustomIdUtils.createButtonCustomIdWithoutConfigId(getCommandId(), REVEAL_BUTTON_ID))
                                .label(I18n.getMessage("h.button.reveal.label", userLocale)).build())
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
                        event.sendMessage(EmbedOrMessageDefinition.builder()
                                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                                .userReference(true)
                                .descriptionOrContent(I18n.getMessage("h.madeHiddenRoll.message", userLocale))
                                .build()))
                .parallel()
                .then();
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        //only one button so we don't check the button id
        return Flux.merge(1,
                        event.acknowledgeAndDeleteOriginal(),
                        event.sendMessage(event.getMessageDefinitionOfEventMessageWithoutButtons()
                                        .toBuilder()
                                        .userReference(true)
                                        .build())
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


    protected EmbedOrMessageDefinition getHelpMessage(Locale userLocale) {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent(I18n.getMessage("h.help.message", userLocale) + "\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.example.field.name", userLocale), I18n.getMessage("h.help.example.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.documentation.field.name", userLocale), I18n.getMessage("help.documentation.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.discord.server.field.name", userLocale), I18n.getMessage("help.discord.server.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.discord.support.field.name", userLocale), I18n.getMessage("help.discord.support.field.value", userLocale), false))
                .build();
    }
}
