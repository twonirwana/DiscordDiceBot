package de.janno.discord.bot.command.reroll;

import com.google.common.base.Preconditions;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.dice.NoCachDiceEvaluator;
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
import de.janno.evaluator.dice.random.GivenDiceNumberSupplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RerollAnswerHandler extends AbstractComponentInteractEventHandler<RerollAnswerConfig, RerollAnswerStateData> {
    private static final String CONFIG_TYPE_ID = "RerollAnswerConfig";
    private static final String STATE_DATA_TYPE_ID = "RerollAnswerStateData";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String COMMAND_ID = "reroll_answer";
    private final PersistenceManager persistenceManager;

    public RerollAnswerHandler(PersistenceManager persistenceManager) {
        super(persistenceManager);
        this.persistenceManager = persistenceManager;
    }

    public static EmbedOrMessageDefinition applyToAnswer(EmbedOrMessageDefinition input, List<DieIdAndValue> dieIdAndValues, Locale locale, UUID configUUID) {
        List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions = dieIdAndValues.stream()
                .map(dv -> new ButtonIdLabelAndDiceExpression(dv.getDieIdDb().toDieId().toString(), dv.getValue(), dv.getDieIdDb().toDieId().toString(), false, false))
                //todo better solution?
                .limit(20)
                .toList();
        List<ComponentRowDefinition> buttons = ButtonHelper.extendButtonLayout(ButtonHelper.createButtonLayout(COMMAND_ID, configUUID, buttonIdLabelAndDiceExpressions, Set.of()),
                List.of(ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(COMMAND_ID, ROLL_BUTTON_ID, configUUID))
                        .label(I18n.getMessage("sum_custom_set.button.label.roll", locale))
                        // .disabled(rollDisabled)
                        .style(ButtonDefinition.Style.SUCCESS)
                        //todo new line?
                        .build()), false);
        //todo finish button?

        return input.toBuilder()
                .componentRowDefinitions(buttons)
                .build();
    }

    public static RerollAnswerConfig createNewRerollAnswerConfig(Config parentConfig, String expression, List<DieIdAndValue> dieIdAndValues, int rerollCount) {
        return new RerollAnswerConfig(null,
                parentConfig.getAnswerFormatType(),
                null,
                parentConfig.getDiceStyleAndColor(),
                parentConfig.getConfigLocale(),
                AnswerInteractionType.reroll,
                expression,
                dieIdAndValues,
                rerollCount);
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

        final State<RerollAnswerStateData> updatedState = updateStateWithButtonValue(buttonValue,
                Optional.ofNullable(loadedStateData).map(RerollAnswerStateData::getRerollDice).stream().flatMap(Collection::stream).map(DieIdDb::toDieId).toList(),
                loadedConfig.getDieIdAndValues().stream().map(DieIdAndValue::getDieIdDb).map(DieIdDb::toDieId).toList(),
                invokingUserName,
                Optional.ofNullable(loadedStateData).map(RerollAnswerStateData::getLockedForUserName).orElse("")
        );
        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(), loadedConfig, updatedState);
    }


    private State<RerollAnswerStateData> updateStateWithButtonValue(@NonNull final String buttonValue,
                                                                    final List<DieId> currentlySelectedIds,
                                                                    @NonNull final List<DieId> allIds,
                                                                    @NonNull final String invokingUserName,
                                                                    @Nullable final String lockedToUser) {
        //todo user lock, only the orignial user can edit
        if (ROLL_BUTTON_ID.equals(buttonValue)) {
            return new State<>(buttonValue, new RerollAnswerStateData(currentlySelectedIds.stream()
                    .map(DieIdDb::fromDieId)
                    .toList(), lockedToUser));
        }

        List<DieId> matchingDieIds = allIds.stream()
                .filter(d -> buttonValue.equals(d.toString()))
                .toList();

        if (matchingDieIds.size() != 1) {
            //todo with config
            throw new IllegalStateException("Missing dieId %s in %s".formatted(buttonValue, allIds));
        }

        DieId clickedDieId = matchingDieIds.getFirst();
        List<DieId> newSelectedIds = new ArrayList<>(currentlySelectedIds);
        if (currentlySelectedIds.contains(clickedDieId)) {
            newSelectedIds.remove(clickedDieId);
        } else {
            newSelectedIds.add(clickedDieId);
        }
        return new State<>(buttonValue, new RerollAnswerStateData(newSelectedIds.stream().map(DieIdDb::fromDieId).toList(), lockedToUser));
    }

    @Override
    protected void updateCurrentMessageStateData(UUID configUUID, @Nullable Long guildId, long channelId, long messageId, @NonNull RerollAnswerConfig config, @NonNull State<RerollAnswerStateData> state) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue())) {
            persistenceManager.deleteStateForMessage(channelId, messageId);
            //message data so we knew the button message exists
            persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), Mapper.NO_PERSISTED_STATE, null));
        } else if (state.getData() != null) {
            persistenceManager.deleteStateForMessage(channelId, messageId);
            persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), STATE_DATA_TYPE_ID, Mapper.serializedObject(state.getData())));
        }
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID configUUID, RerollAnswerConfig config, State<RerollAnswerStateData> state, long channelId, long userId) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue())) {
            return Optional.of(List.of());
        }
        return Optional.of(createButtonLayout(configUUID, config, state.getData()));
    }

    public @NonNull EmbedOrMessageDefinition createNewButtonMessage(@NonNull UUID configUUID, @NonNull RerollAnswerConfig config, long channelId) {

        return EmbedOrMessageDefinition.builder()
                .type(EmbedOrMessageDefinition.Type.EMBED)
                //todo I18n
                .descriptionOrContent(config.getRerollCount() + ": " + config.getExpression())
                .componentRowDefinitions(createButtonLayout(configUUID, config, null))
                .build();
    }

    @Override
    protected @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessageWithState(@NonNull UUID configId,
                                                                                          @NonNull RerollAnswerConfig config,
                                                                                          @NonNull State<RerollAnswerStateData> state,
                                                                                          @Nullable Long guildId,
                                                                                          long channelId) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue())) {
            Map<DieId, String> dieIdValueMap = config.getDieIdAndValues().stream()
                    .collect(Collectors.toMap(dv -> dv.getDieIdDb().toDieId(), DieIdAndValue::getValue));
            Map<DieId, Integer> givenDiceNumberMap = state.getData().getRerollDice().stream()
                    //todo custom dice????
                    .collect(Collectors.toMap(DieIdDb::toDieId, d -> Integer.parseInt(dieIdValueMap.get(d.toDieId()))));
            DiceEvaluatorAdapter evaluatorAdapter = new DiceEvaluatorAdapter(new NoCachDiceEvaluator(new GivenDiceNumberSupplier(givenDiceNumberMap)));

            //todo label and sum up
            RollAnswer rollAnswer = evaluatorAdapter.answerRollWithGivenLabel(config.getExpression(), null, false, config.getAnswerFormatType(), config.getDiceStyleAndColor(), config.getConfigLocale());


            RerollAnswerConfig rerollAnswerConfig = createNewRerollAnswerConfig(config, config.getExpression(), rollAnswer.getDieIdAndValues(), config.getRerollCount() + 1);

            //todo supplier
            UUID newMessageUUID = UUID.randomUUID();
            RerollAnswerHandler.createMessageConfig(newMessageUUID, guildId, channelId, rerollAnswerConfig).ifPresent(persistenceManager::saveMessageConfig);

            EmbedOrMessageDefinition newMessage = RerollAnswerHandler.applyToAnswer(RollAnswerConverter.toEmbedOrMessageDefinition(rollAnswer), rollAnswer.getDieIdAndValues(), config.getConfigLocale(), newMessageUUID);
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
        //todo disabele/enable roll
        //todo change style for state
        Set<DieId> alreadySelectedButtons = Optional.ofNullable(stateData).stream()
                .flatMap(s -> s.getRerollDice().stream())
                .map(DieIdDb::toDieId)
                .collect(Collectors.toSet());

        List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions = config.getDieIdAndValues().stream()
                //todo better style selection
                .map(dv -> new ButtonIdLabelAndDiceExpression(dv.getDieIdDb().toDieId().toString(), dv.getValue(), dv.getDieIdDb().toDieId().toString(), false, alreadySelectedButtons.contains(dv.getDieIdDb().toDieId())))
                .toList();

        List<ComponentRowDefinition> diceValueButtons = ButtonHelper.createButtonLayout(getCommandId(), configUUID, buttonIdLabelAndDiceExpressions, Set.of());


        return ButtonHelper.extendButtonLayout(diceValueButtons,
                List.of(ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ROLL_BUTTON_ID, configUUID))
                        .label(I18n.getMessage("sum_custom_set.button.label.roll", config.getConfigLocale()))
                        // .disabled(rollDisabled)
                        .style(ButtonDefinition.Style.SUCCESS)
                        //todo new line?
                        .build()), false);
    }

    @Override
    public @NonNull String getCommandId() {
        return COMMAND_ID;
    }
}
