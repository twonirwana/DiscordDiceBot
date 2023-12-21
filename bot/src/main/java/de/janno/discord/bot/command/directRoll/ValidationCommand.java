package de.janno.discord.bot.command.directRoll;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.command.RollAnswerConverter;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ValidationCommand extends DirectRollCommand {

    public static final String ROLL_COMMAND_ID = "validation";
    private final DiceEvaluatorAdapter diceEvaluatorAdapter;

    public ValidationCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        super(persistenceManager, cachingDiceEvaluator);
        this.diceEvaluatorAdapter = new DiceEvaluatorAdapter(cachingDiceEvaluator);
    }

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest option, @NonNull Locale userLocale) {
        if (!EXPRESSION_OPTION_NAME.equals(option.getFocusedOptionName())) {
            return List.of();
        }
        if (Strings.isNullOrEmpty(option.getFocusedOptionValue())) {
            return List.of(new AutoCompleteAnswer(I18n.getMessage("validation.autoComplete.example", userLocale), I18n.getMessage("validation.autoComplete.example", userLocale)));
        }
        Optional<String> validation = diceEvaluatorAdapter.shortValidateDiceExpressionWitOptionalLabel(option.getFocusedOptionValue(), userLocale);
        BotMetrics.incrementValidationCounter(validation.isEmpty());
        return validation
                .map(s -> List.of(new AutoCompleteAnswer(s, option.getFocusedOptionValue())))
                .orElse(List.of(getValidAutoCompleteMessage(option.getFocusedOptionValue(), userLocale)));
    }

    private AutoCompleteAnswer getValidAutoCompleteMessage(@NonNull String typedExpression, @NonNull Locale userLocale){
        if(typedExpression.length() <= 100){
            return new AutoCompleteAnswer(typedExpression, typedExpression);
        }
        return new AutoCompleteAnswer(I18n.getMessage("validation.autoComplete.tooLong", userLocale), I18n.getMessage("validation.autoComplete.tooLong", userLocale));
    }

    @Override
    public @NonNull String getCommandId() {
        return ROLL_COMMAND_ID;
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .nameLocales(I18n.allNoneEnglishMessagesNames("validation.name"))
                .description(I18n.getMessage("validation.description", Locale.ENGLISH))
                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("validation.description"))
                .option(CommandDefinitionOption.builder()
                        .name(EXPRESSION_OPTION_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("r.expression.name"))
                        .description(I18n.getMessage("validation.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("validation.description"))
                        .required(true)
                        .autoComplete(true)
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build();
    }

    protected @NonNull Mono<Void> createResponse(@NonNull SlashEventAdaptor event,
                                                 @NonNull String commandString,
                                                 @NonNull String diceExpression,
                                                 @NonNull RollAnswer answer,
                                                 @NonNull Stopwatch stopwatch,
                                                 @NonNull Locale userLocale) {
        String replayMessage = Stream.of(commandString, answer.getWarning())
                .filter(s -> !Strings.isNullOrEmpty(s))
                .collect(Collectors.joining(" "));
        return Flux.merge(event.reply(replayMessage, true),
                        Mono.defer(() -> event.createResultMessageWithReference(RollAnswerConverter.toEmbedOrMessageDefinition(answer))
                                .doOnSuccess(v ->
                                        log.info("{}: '{}'={} -> {} in {}ms",
                                                event.getRequester().toLogString(),
                                                commandString.replace("`", ""),
                                                diceExpression,
                                                answer.toShortString(),
                                                stopwatch.elapsed(TimeUnit.MILLISECONDS)
                                        )))
                )
                .parallel().then();
    }
}