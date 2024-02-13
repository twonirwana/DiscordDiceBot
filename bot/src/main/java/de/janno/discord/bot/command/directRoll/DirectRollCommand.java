package de.janno.discord.bot.command.directRoll;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.command.RollAnswerConverter;
import de.janno.discord.bot.command.channelConfig.AliasHelper;
import de.janno.discord.bot.command.channelConfig.DirectRollConfig;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.dice.DiceSystemAdapter;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.janno.discord.bot.command.channelConfig.ChannelConfigCommand.DIRECT_ROLL_CONFIG_TYPE_ID;

@Slf4j
public class DirectRollCommand implements SlashCommand {

    public static final String ROLL_COMMAND_ID = "r";
    private static final String HELP = "help";
    protected final String expressionOptionName;
    protected final PersistenceManager persistenceManager;
    private final DiceEvaluatorAdapter diceEvaluatorAdapter;

    public DirectRollCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        this(persistenceManager, cachingDiceEvaluator, "expression");
    }


    public DirectRollCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator, String expressionOptionName) {
        this.diceEvaluatorAdapter = new DiceEvaluatorAdapter(cachingDiceEvaluator);
        this.persistenceManager = persistenceManager;
        this.expressionOptionName = expressionOptionName;
    }

    @Override
    public @NonNull String getCommandId() {
        return ROLL_COMMAND_ID;
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .nameLocales(I18n.allNoneEnglishMessagesNames("r.name"))
                .description(I18n.getMessage("r.description", Locale.ENGLISH))
                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("r.description"))
                .option(CommandDefinitionOption.builder()
                        .name(expressionOptionName)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("r.expression.name"))
                        .description(I18n.getMessage("r.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("r.description"))
                        .required(true)
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build();
    }

    private DirectRollConfig getDirectRollConfig(long channelId) {
        return persistenceManager.getChannelConfig(channelId, DIRECT_ROLL_CONFIG_TYPE_ID)
                .map(this::deserializeConfig)
                //default direct roll config is english
                .orElse(new DirectRollConfig(null, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), Locale.ENGLISH));
    }

    @VisibleForTesting
    DirectRollConfig deserializeConfig(ChannelConfigDTO channelConfigDTO) {
        Preconditions.checkArgument(DIRECT_ROLL_CONFIG_TYPE_ID.equals(channelConfigDTO.getConfigClassId()), "Unknown configClassId: %s", channelConfigDTO.getConfigClassId());
        return Mapper.deserializeObject(channelConfigDTO.getConfig(), DirectRollConfig.class);
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocale) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Optional<String> checkPermissions = event.checkPermissions(userLocale);
        if (checkPermissions.isPresent()) {
            return event.reply(checkPermissions.get(), false);
        }

        final String commandString = event.getCommandString();

        Optional<CommandInteractionOption> expressionOptional = event.getOption(expressionOptionName);
        if (expressionOptional.isPresent()) {
            final String commandParameter = expressionOptional
                    .map(CommandInteractionOption::getStringValue)
                    .orElseThrow();
            if (commandParameter.equals(HELP)) {
                BotMetrics.incrementSlashHelpMetricCounter(getCommandId());
                return event.replyWithEmbedOrMessageDefinition(getHelpMessage(userLocale), true);
            }

            final String expressionWithOptionalLabelsAndAppliedAliases = AliasHelper.getAndApplyAliaseToExpression(event.getChannelId(), event.getUserId(), persistenceManager, commandParameter);
            Optional<String> labelValidationMessage = DiceSystemAdapter.validateLabel(expressionWithOptionalLabelsAndAppliedAliases, userLocale);
            if (labelValidationMessage.isPresent()) {
                return replyValidationMessage(event, labelValidationMessage.get(), commandString);
            }
            String diceExpression = DiceSystemAdapter.getExpressionFromExpressionWithOptionalLabel(expressionWithOptionalLabelsAndAppliedAliases);
            Optional<String> expressionValidationMessage = diceEvaluatorAdapter.validateDiceExpression(diceExpression, "`/r expression:help`", userLocale);
            if (expressionValidationMessage.isPresent()) {
                return replyValidationMessage(event, expressionValidationMessage.get(), commandString);
            }
            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), "[%s, %s]".formatted(diceExpression, commandParameter));
            DirectRollConfig config = getDirectRollConfig(event.getChannelId());
            BotMetrics.incrementAnswerFormatCounter(config.getAnswerFormatType(), getCommandId());

            RollAnswer answer = diceEvaluatorAdapter.answerRollWithOptionalLabelInExpression(expressionWithOptionalLabelsAndAppliedAliases, DiceSystemAdapter.LABEL_DELIMITER, config.isAlwaysSumResult(), config.getAnswerFormatType(), config.getDiceStyleAndColor(), userLocale);
            return createResponse(event, commandString, diceExpression, answer, stopwatch, userLocale);

        }

        return Mono.empty();
    }

    protected EmbedOrMessageDefinition getHelpMessage(Locale userLocale) {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent(I18n.getMessage("r.help.message", userLocale) + "\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.example.field.name", userLocale), I18n.getMessage("r.help.example.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.documentation.field.name", userLocale), I18n.getMessage("help.documentation.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.discord.server.field.name", userLocale), I18n.getMessage("help.discord.server.field.value", userLocale), false))
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
        return Flux.merge(Strings.isNullOrEmpty(answer.getWarning()) ? Mono.defer(event::acknowledgeAndRemoveSlash) : event.reply(replayMessage, true),
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

    private Mono<Void> replyValidationMessage(@NonNull SlashEventAdaptor event, @NonNull String validationMessage, @NonNull String commandString) {
        log.info("{} Validation message: {} for {}", event.getRequester().toLogString(),
                validationMessage,
                commandString);
        return event.reply(String.format("%s\n%s", commandString, validationMessage), true);
    }

}
