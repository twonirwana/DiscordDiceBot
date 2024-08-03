package de.janno.discord.bot.command.reroll;

import com.google.common.base.Preconditions;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.evaluator.dice.DieId;
import de.janno.evaluator.dice.DieIdAndValue;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class RerollAnswerHandler extends AbstractComponentInteractEventHandler<RerollAnswerConfig, RerollAnswerStateData> {
    private static final String CONFIG_TYPE_ID = "RerollAnswerConfig";
    private static final String STATE_DATA_TYPE_ID = "RerollAnswerStateData";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String WRONG_USER_ID = "wrongUser";
    private static final String FINISH_BUTTON_ID = "finish";
    private static final String COMMAND_ID = "reroll_answer";
    private final PersistenceManager persistenceManager;
    private final DiceEvaluatorAdapter diceEvaluatorAdapter;

    public RerollAnswerHandler(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator, Supplier<UUID> uuidSupplier) {
        super(persistenceManager, uuidSupplier);
        this.persistenceManager = persistenceManager;
        this.diceEvaluatorAdapter = new DiceEvaluatorAdapter(cachingDiceEvaluator);

    }

    public RerollAnswerHandler(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        this(persistenceManager, cachingDiceEvaluator, UUID::randomUUID);
    }

    private static EmbedOrMessageDefinition applyToAnswer(EmbedOrMessageDefinition input, List<DieIdTypeAndValue> dieIdTypeAndValues, Locale locale, UUID configUUID) {
        List<ComponentRowDefinition> buttons = createButtons(dieIdTypeAndValues, Set.of(), locale, configUUID);
        return input.toBuilder()
                .componentRowDefinitions(buttons)
                .build();
    }

    private static String getDiceTypeLabel(DieIdTypeAndValue dieIdTypeAndValue) {
        if (dieIdTypeAndValue.getSelectedFrom() != null) {
            return StringUtils.abbreviate("d[" + String.join("/", dieIdTypeAndValue.getSelectedFrom()) + "]", 10);
        }
        return "d" + dieIdTypeAndValue.getDiceSides();
    }

    private static List<ComponentRowDefinition> createButtons(List<DieIdTypeAndValue> dieIdTypeAndValues, Set<DieId> selectedDieIds, Locale locale, UUID configUUID) {

        List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions = dieIdTypeAndValues.stream()
                .limit(20)
                .map(dv -> new ButtonIdLabelAndDiceExpression(dv.getDieIdDb().toDieId().toString(), dv.getValue() + " ∈ " + getDiceTypeLabel(dv), dv.getDieIdDb().toDieId().toString(), false, false))
                .toList();

        Set<String> selectedDieIdsAsString = selectedDieIds.stream().map(Objects::toString).collect(Collectors.toSet());

        return ButtonHelper.extendButtonLayout(ButtonHelper.createButtonLayoutDetail(COMMAND_ID, configUUID, buttonIdLabelAndDiceExpressions.stream()
                        .map(b -> new ButtonHelper.ButtonIdLabelAndDiceExpressionExtension(b, false, selectedDieIdsAsString.contains(b.getButtonId()) ? ButtonDefinition.Style.PRIMARY : ButtonDefinition.Style.SECONDARY))
                        .toList()),
                List.of(ButtonDefinition.builder()
                                .id(BottomCustomIdUtils.createButtonCustomId(COMMAND_ID, ROLL_BUTTON_ID, configUUID))
                                .label(I18n.getMessage("reroll.button.label.roll", locale))
                                .style(ButtonDefinition.Style.SUCCESS)
                                .disabled(selectedDieIds.isEmpty())
                                .build(),
                        ButtonDefinition.builder()
                                .id(BottomCustomIdUtils.createButtonCustomId(COMMAND_ID, FINISH_BUTTON_ID, configUUID))
                                .label(I18n.getMessage("reroll.button.label.finish", locale))
                                .style(ButtonDefinition.Style.DANGER)
                                .build()), true);
    }

    public static EmbedOrMessageDefinition createConfigAndApplyToAnswer(RollConfig config,
                                                                        RollAnswer answer,
                                                                        EmbedOrMessageDefinition baseAnswer,
                                                                        String invokingUserName,
                                                                        Long guildId,
                                                                        long channelId,
                                                                        PersistenceManager persistenceManager,
                                                                        UUID rerollConfigId) {
        RerollAnswerConfig rerollAnswerConfig = RerollAnswerHandler.createNewRerollAnswerConfig(config,
                answer.getExpression(),
                answer.getExpressionLabel(),
                answer.getDieIdTypeAndValues(),
                1,
                invokingUserName);
        createMessageConfig(rerollConfigId, guildId, channelId, rerollAnswerConfig).ifPresent(persistenceManager::saveMessageConfig);
        return RerollAnswerHandler.applyToAnswer(baseAnswer, answer.getDieIdTypeAndValues(), config.getConfigLocale(), rerollConfigId);
    }

    private static RerollAnswerConfig createNewRerollAnswerConfig(@NonNull RollConfig parentConfig, @NonNull String expression, @Nullable String label, @NonNull List<DieIdTypeAndValue> dieIdTypeAndValues, int rerollCount, String owner) {

        return new RerollAnswerConfig(null,
                parentConfig.getAnswerFormatType(),
                null, //legacy
                parentConfig.getDiceStyleAndColor(),
                parentConfig.getConfigLocale(),
                AnswerInteractionType.reroll,
                expression,
                dieIdTypeAndValues,
                rerollCount,
                owner,
                parentConfig.alwaysSumResultUp(),
                label);
    }

    public static Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, @Nullable Long guildId, long channelId, @NonNull RerollAnswerConfig config) {
        return Optional.of(new MessageConfigDTO(configUUID, guildId, channelId, COMMAND_ID, CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected ConfigAndState<RerollAnswerConfig, RerollAnswerStateData> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                                               @NonNull MessageDataDTO messageDataDTO,
                                                                                                               @NonNull String buttonValue,
                                                                                                               @NonNull String invokingUserName) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        Preconditions.checkArgument(Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId)
                .map(c -> Set.of(STATE_DATA_TYPE_ID, Mapper.NO_PERSISTED_STATE).contains(c))
                .orElse(true), "Unknown stateDataClassId: %s", Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId).orElse("null"));

        final RerollAnswerStateData loadedStateData;
        if (messageDataDTO.getStateDataClassId().equals(STATE_DATA_TYPE_ID)) {
            loadedStateData = Optional.of(messageDataDTO)
                    .map(MessageDataDTO::getStateData)
                    .map(sd -> Mapper.deserializeObject(sd, RerollAnswerStateData.class))
                    .orElse(null);
        } else {
            loadedStateData = null;
        }
        final RerollAnswerConfig loadedConfig = Mapper.deserializeObject(messageConfigDTO.getConfig(), RerollAnswerConfig.class);
        if (!loadedConfig.getOwner().equals(invokingUserName)) {
            //unmodified state if the user is not the owner, we need to change the buttonValue or else it will trigger actions
            return new ConfigAndState<>(messageConfigDTO.getConfigUUID(), loadedConfig, new State<>(WRONG_USER_ID, Optional.ofNullable(loadedStateData)
                    .orElse(new RerollAnswerStateData(new ArrayList<>()))));
        }

        final State<RerollAnswerStateData> updatedState = updateStateWithButtonValue(buttonValue,
                Optional.ofNullable(loadedStateData).map(RerollAnswerStateData::getRerollDice).stream().flatMap(Collection::stream).map(DieIdDb::toDieId).toList(),
                loadedConfig.getDieIdTypeAndValues().stream().map(DieIdTypeAndValue::getDieIdDb).map(DieIdDb::toDieId).toList(),
                messageConfigDTO.getConfigUUID());
        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(), loadedConfig, updatedState);
    }


    private State<RerollAnswerStateData> updateStateWithButtonValue(@NonNull final String buttonValue,
                                                                    final List<DieId> currentlySelectedIds,
                                                                    @NonNull final List<DieId> allIds,
                                                                    @NonNull UUID configUUID) {
        if (ROLL_BUTTON_ID.equals(buttonValue)) {
            return new State<>(buttonValue, new RerollAnswerStateData(currentlySelectedIds.stream()
                    .map(DieIdDb::fromDieId)
                    .toList()));
        }

        if (FINISH_BUTTON_ID.equals(buttonValue)) {
            return new State<>(buttonValue, new RerollAnswerStateData(List.of()));
        }

        List<DieId> matchingDieIds = allIds.stream()
                .filter(d -> buttonValue.equals(d.toString()))
                .toList();

        if (matchingDieIds.size() != 1) {
            throw new IllegalStateException("Missing dieId %s in %s in configUUID %s".formatted(buttonValue, allIds, configUUID));
        }

        DieId clickedDieId = matchingDieIds.getFirst();
        List<DieId> newSelectedIds = new ArrayList<>(currentlySelectedIds);
        if (currentlySelectedIds.contains(clickedDieId)) {
            newSelectedIds.remove(clickedDieId);
        } else {
            newSelectedIds.add(clickedDieId);
        }
        return new State<>(buttonValue, new RerollAnswerStateData(newSelectedIds.stream().map(DieIdDb::fromDieId).toList()));
    }

    @Override
    protected void updateCurrentMessageStateData(UUID configUUID, @Nullable Long guildId, long channelId, long messageId, @NonNull RerollAnswerConfig config, @NonNull State<RerollAnswerStateData> state) {
        if (Set.of(ROLL_BUTTON_ID, FINISH_BUTTON_ID).contains(state.getButtonValue())) {
            persistenceManager.deleteStateForMessage(channelId, messageId);
            deleteMessageConfigWithDelay(configUUID)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
        } else if (state.getData() != null) {
            persistenceManager.deleteStateForMessage(channelId, messageId);
            persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), STATE_DATA_TYPE_ID, Mapper.serializedObject(state.getData())));
        }
    }

    public Mono<Void> deleteMessageConfigWithDelay(UUID configUUID) {
        final Duration delay = Duration.ofMillis(io.avaje.config.Config.getLong("command.delayMessageDataDeletionMs", 10000));
        return Mono.defer(() -> Mono.just(configUUID)
                .delayElement(delay)
                .doOnNext(persistenceManager::deleteMessageConfig).ofType(Void.class));
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID configUUID, RerollAnswerConfig config, State<RerollAnswerStateData> state, long channelId, long userId) {
        if (Set.of(ROLL_BUTTON_ID, FINISH_BUTTON_ID).contains(state.getButtonValue())) {
            return Optional.of(List.of());
        }
        return Optional.of(createButtonLayout(configUUID, config, state.getData()));
    }

    @Override
    protected @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessage(@NonNull UUID configId,
                                                                                 @NonNull RerollAnswerConfig config,
                                                                                 @Nullable State<RerollAnswerStateData> state,
                                                                                 @Nullable Long guildId,
                                                                                 long channelId) {
        if (state == null) {
            return Optional.empty();
        }
        if (ROLL_BUTTON_ID.equals(state.getButtonValue())) {

            Set<DieIdDb> rerollDieIds = new HashSet<>(Optional.ofNullable(state.getData()).map(RerollAnswerStateData::getRerollDice).orElse(List.of()));

            List<DieIdAndValue> givenDiceNumberMap = config.getDieIdTypeAndValues().stream()
                    .filter(dv -> !rerollDieIds.contains(dv.getDieIdDb()))
                    .map(DieIdTypeAndValue::toDieIdAndValue)
                    .toList();

            RollAnswer rollAnswer = diceEvaluatorAdapter.answerRollWithGivenLabel(config.getExpression(),
                    config.getLabel(),
                    config.isAlwaysSumUp(),
                    config.getAnswerFormatType(),
                    config.getDiceStyleAndColor(),
                    config.getConfigLocale(),
                    givenDiceNumberMap);

            RerollAnswerConfig rerollAnswerConfig = createNewRerollAnswerConfig(config, config.getExpression(), config.getLabel(), rollAnswer.getDieIdTypeAndValues(), config.getRerollCount() + 1, config.getOwner());

            UUID newMessageUUID = uuidSupplier.get();
            createMessageConfig(newMessageUUID, guildId, channelId, rerollAnswerConfig).ifPresent(persistenceManager::saveMessageConfig);

            EmbedOrMessageDefinition newMessage = RerollAnswerHandler.applyToAnswer(RollAnswerConverter.toEmbedOrMessageDefinition(rollAnswer), rollAnswer.getDieIdTypeAndValues(), config.getConfigLocale(), newMessageUUID);
            newMessage = newMessage.toBuilder()
                    .title(config.getRerollCount() + ": " + newMessage.getTitle())
                    .build();
            return Optional.of(newMessage);

        }
        return Optional.empty();
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(RerollAnswerConfig config, State<RerollAnswerStateData> state, long channelId, long userId) {
        return Optional.empty();
    }

    @Override
    protected boolean shouldKeepExistingButtonMessage(@NonNull ButtonEventAdaptor event) {
        return true;
    }

    private List<ComponentRowDefinition> createButtonLayout(UUID configUUID, RerollAnswerConfig config, @Nullable RerollAnswerStateData stateData) {
        Set<DieId> alreadySelectedButtons = Optional.ofNullable(stateData).stream()
                .flatMap(s -> s.getRerollDice().stream())
                .map(DieIdDb::toDieId)
                .collect(Collectors.toSet());

        return createButtons(config.getDieIdTypeAndValues(), alreadySelectedButtons, config.getConfigLocale(), configUUID);
    }

    @Override
    public @NonNull String getCommandId() {
        return COMMAND_ID;
    }
}
