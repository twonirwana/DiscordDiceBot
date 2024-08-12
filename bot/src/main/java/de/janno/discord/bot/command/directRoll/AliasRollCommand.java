package de.janno.discord.bot.command.directRoll;

import com.google.common.base.Strings;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.channelConfig.Alias;
import de.janno.discord.bot.command.channelConfig.AliasHelper;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class AliasRollCommand extends DirectRollCommand {

    public static final String ROLL_COMMAND_ID = "a";

    public AliasRollCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        super(persistenceManager, cachingDiceEvaluator, "alias_or_expression");
    }

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest option, @NonNull Locale userLocale, long channelId, long userId) {
        List<Alias> channelAlias = AliasHelper.getChannelAlias(channelId, persistenceManager).stream().filter(a -> a.getType() == Alias.Type.Replace).toList();
        List<Alias> userAlias = AliasHelper.getUserChannelAlias(channelId, userId, persistenceManager).stream().filter(a -> a.getType() == Alias.Type.Replace).toList();

        Map<String, Alias> combinedAlias = channelAlias.stream().collect(Collectors.toMap(Alias::getName, Function.identity()));
        userAlias.forEach(a -> combinedAlias.put(a.getName(), a)); //user alias overwrite channel alias, they have higher priority
        if (combinedAlias.isEmpty() && Strings.isNullOrEmpty(option.getFocusedOptionValue())) {
            return List.of(new AutoCompleteAnswer(I18n.getMessage("a.autoComplete.missingAlias", userLocale), "help"));
        }
        List<AutoCompleteAnswer> filteredAlias = combinedAlias.values().stream()
                .filter(p -> Strings.isNullOrEmpty(option.getFocusedOptionValue()) ||
                       p.getName().toLowerCase().contains(option.getFocusedOptionValue().toLowerCase()))
                .sorted(Comparator.comparing(Alias::getName))
                .map(p -> new AutoCompleteAnswer(p.getName(), p.getName()))
                .collect(Collectors.toList());
        if (filteredAlias.isEmpty()) {
            return List.of(); //no autocomplete so the user can enter his own expression
        }
        return filteredAlias;
    }

    @Override
    public @NonNull String getCommandId() {
        return ROLL_COMMAND_ID;
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .nameLocales(I18n.allNoneEnglishMessagesNames("a.name"))
                .description(I18n.getMessage("a.description", Locale.ENGLISH))
                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("a.description"))
                .option(CommandDefinitionOption.builder()
                        .name(expressionOptionName)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("a.expression.name"))
                        .description(I18n.getMessage("a.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("a.description"))
                        .required(true)
                        .autoComplete(true)
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build();
    }

    protected EmbedOrMessageDefinition getHelpMessage(Locale userLocale) {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent(I18n.getMessage("a.help.message", userLocale) + "\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.example.field.name", userLocale), I18n.getMessage("a.help.example.alias.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.example.field.name", userLocale), I18n.getMessage("a.help.example.expression.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.documentation.field.name", userLocale), I18n.getMessage("help.documentation.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.discord.server.field.name", userLocale), I18n.getMessage("help.discord.server.field.value", userLocale), false))
                .build();
    }
}