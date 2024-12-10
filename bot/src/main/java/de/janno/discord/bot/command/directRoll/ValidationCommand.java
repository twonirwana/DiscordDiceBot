package de.janno.discord.bot.command.directRoll;

import com.google.common.base.Strings;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandIntegrationType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
public class ValidationCommand extends DirectRollCommand {

    public static final String ROLL_COMMAND_ID = "validation";
    private final DiceEvaluatorAdapter diceEvaluatorAdapter;

    public ValidationCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        super(persistenceManager, cachingDiceEvaluator);
        this.diceEvaluatorAdapter = new DiceEvaluatorAdapter(cachingDiceEvaluator);
    }

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest option, @NonNull Locale userLocale, long channelId, Long guildId, long userId) {
        if (!expressionOptionName.equals(option.getFocusedOptionName())) {
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

    private AutoCompleteAnswer getValidAutoCompleteMessage(@NonNull String typedExpression, @NonNull Locale userLocale) {
        if (typedExpression.length() <= 100) {
            return new AutoCompleteAnswer(typedExpression, typedExpression);
        }
        return new AutoCompleteAnswer(I18n.getMessage("validation.autoComplete.tooLong", userLocale), "'" + I18n.getMessage("validation.autoComplete.tooLong", userLocale) + "'");
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
                .integrationTypes(CommandIntegrationType.ALL)
                .option(CommandDefinitionOption.builder()
                        .name(expressionOptionName)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("validation.expression.name"))
                        .description(I18n.getMessage("validation.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("validation.description"))
                        .required(true)
                        .autoComplete(true)
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build();
    }

    protected EmbedOrMessageDefinition getHelpMessage(Locale userLocale) {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent(I18n.getMessage("validation.help.message", userLocale) + "\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.example.field.name", userLocale), I18n.getMessage("validation.help.example.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.documentation.field.name", userLocale), I18n.getMessage("help.documentation.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.discord.server.field.name", userLocale), I18n.getMessage("help.discord.server.field.value", userLocale), false))
                .build();
    }
}