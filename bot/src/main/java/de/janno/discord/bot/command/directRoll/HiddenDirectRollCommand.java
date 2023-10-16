package de.janno.discord.bot.command.directRoll;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import de.janno.discord.bot.BotMetrics;
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
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static de.janno.discord.bot.command.channelConfig.ChannelConfigCommand.DIRECT_ROLL_CONFIG_TYPE_ID;

@Slf4j
public class HiddenDirectRollCommand implements SlashCommand, ComponentInteractEventHandler {

    public static final String ROLL_COMMAND_ID = "h";
    protected static final String ACTION_EXPRESSION = "expression";
    private static final String HELP = "help";
    protected final boolean removeSlash;
    private final DiceEvaluatorAdapter diceEvaluatorAdapter;
    private final PersistenceManager persistenceManager;

    public HiddenDirectRollCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        this(persistenceManager, cachingDiceEvaluator, true);
    }

    public HiddenDirectRollCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator, boolean removeSlash) {
        this.diceEvaluatorAdapter = new DiceEvaluatorAdapter(cachingDiceEvaluator);
        this.persistenceManager = persistenceManager;
        this.removeSlash = removeSlash;
    }

    @Override
    public @NonNull String getCommandId() {
        return ROLL_COMMAND_ID;
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .description("direct roll of dice expression, configuration with /channel_config")
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_EXPRESSION)
                        .required(true)
                        .description("dice expression, e.g. '2d6'")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build();
    }

    private DirectRollConfig getDirectRollConfig(long channelId) {
        return persistenceManager.getChannelConfig(channelId, DIRECT_ROLL_CONFIG_TYPE_ID)
                .map(this::deserializeConfig)
                .orElse(new DirectRollConfig(null, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor())));
    }

    @VisibleForTesting
    DirectRollConfig deserializeConfig(ChannelConfigDTO channelConfigDTO) {
        Preconditions.checkArgument(DIRECT_ROLL_CONFIG_TYPE_ID.equals(channelConfigDTO.getConfigClassId()), "Unknown configClassId: %s", channelConfigDTO.getConfigClassId());
        return Mapper.deserializeObject(channelConfigDTO.getConfig(), DirectRollConfig.class);
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Optional<String> checkPermissions = event.checkPermissions();
        if (checkPermissions.isPresent()) {
            return event.reply(checkPermissions.get(), false);
        }

        final String commandString = event.getCommandString();

        Optional<CommandInteractionOption> expressionOptional = event.getOption(ACTION_EXPRESSION);
        if (expressionOptional.isPresent()) {
            final String commandParameter = expressionOptional
                    .map(CommandInteractionOption::getStringValue)
                    .orElseThrow();
            if (commandParameter.equals(HELP)) {
                BotMetrics.incrementSlashHelpMetricCounter(getCommandId());
                return event.replyWithEmbedOrMessageDefinition(EmbedOrMessageDefinition.builder()
                        .descriptionOrContent("Type /r and a dice expression, configuration with /channel_config\n" + DiceEvaluatorAdapter.getHelp())
                        .field(new EmbedOrMessageDefinition.Field("Example", "`/r expression:1d6`", false))
                        .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                        .field(new EmbedOrMessageDefinition.Field("Discord Server for Help and News", "https://discord.gg/e43BsqKpFr", false))
                        .build(), true);
            }

            final String expressionWithOptionalLabelsAndAppliedAliases = AliasHelper.getAndApplyAliaseToExpression(event.getChannelId(), event.getUserId(), persistenceManager, commandParameter);
            Optional<String> labelValidationMessage = DiceSystemAdapter.validateLabel(expressionWithOptionalLabelsAndAppliedAliases);
            if (labelValidationMessage.isPresent()) {
                return replyValidationMessage(event, labelValidationMessage.get(), commandString);
            }
            String diceExpression = DiceSystemAdapter.getExpressionFromExpressionWithOptionalLabel(expressionWithOptionalLabelsAndAppliedAliases);
            Optional<String> expressionValidationMessage = diceEvaluatorAdapter.validateDiceExpression(diceExpression, "`/r expression:help`");
            if (expressionValidationMessage.isPresent()) {
                return replyValidationMessage(event, expressionValidationMessage.get(), commandString);
            }

            BotMetrics.incrementSlashStartMetricCounter(getCommandId(), diceExpression);
            DirectRollConfig config = getDirectRollConfig(event.getChannelId());
            BotMetrics.incrementAnswerFormatCounter(config.getAnswerFormatType(), getCommandId());

            RollAnswer answer = diceEvaluatorAdapter.answerRollWithOptionalLabelInExpression(expressionWithOptionalLabelsAndAppliedAliases, DiceSystemAdapter.LABEL_DELIMITER, config.isAlwaysSumResult(), config.getAnswerFormatType(), config.getDiceStyleAndColor());
            EmbedOrMessageDefinition embedOrMessageDefinition = RollAnswerConverter.toEmbedOrMessageDefinition(answer).toBuilder()
                    .componentRowDefinition(ComponentRowDefinition.builder()
                            .buttonDefinition(ButtonDefinition.builder()
                                    .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "reveal", UUID.randomUUID()))
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
                            event.createResultMessageWithEventReference(EmbedOrMessageDefinition.builder()
                                    .type(EmbedOrMessageDefinition.Type.MESSAGE)
                                    .descriptionOrContent("Made a hidden roll")
                                    .build()))
                    .parallel()
                    .then();
        }

        return Mono.empty();
    }

    private Mono<Void> replyValidationMessage(@NonNull SlashEventAdaptor event, @NonNull String validationMessage, @NonNull String commandString) {
        log.info("{} Validation message: {} for {}", event.getRequester().toLogString(),
                validationMessage,
                commandString);
        return event.reply(String.format("%s\n%s", commandString, validationMessage), true);
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        return Flux.merge(1,
                        event.acknowledgeAndRemoveButtons(), //ephemeral message cant be deleted
                        event.createResultMessageWithReference(event.getMessageDefinitionOfEventMessageWithoutButtons(), null)
                                .doOnSuccess(v ->
                                        log.info("{}:-> {} in {}ms",
                                                event.getRequester().toLogString(),
                                                "reveal",
                                                stopwatch.elapsed(TimeUnit.MILLISECONDS)
                                        ))
                )
                .parallel()
                .then();
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return Objects.equals(getCommandId(), BottomCustomIdUtils.getCommandNameFromCustomIdWithPersistence(buttonCustomId));
    }
}
