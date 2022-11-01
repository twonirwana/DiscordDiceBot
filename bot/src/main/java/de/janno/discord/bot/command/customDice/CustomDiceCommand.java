package de.janno.discord.bot.command.customDice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.Dice;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.DiceSystemAdapter;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.NumberSupplier;
import de.janno.evaluator.dice.RandomNumberSupplier;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class CustomDiceCommand extends AbstractCommand<CustomDiceConfig, StateData> {

    private static final String COMMAND_NAME = "custom_dice";
    private static final List<String> LEGACY_DICE_COMMAND_OPTIONS_IDS = IntStream.range(1, 25).mapToObj(i -> i + "_button").toList();
    private static final String LEGACY_START_ACTION = "legacy_start";
    private static final String BUTTONS_COMMAND_OPTIONS_ID = "buttons";
    private static final String LABEL_DELIMITER = "@";
    private static final String BUTTON_MESSAGE = "Click on a button to roll the dice";
    private static final String CONFIG_TYPE_ID = "CustomDiceConfig";
    private final DiceSystemAdapter diceSystemAdapter;

    public CustomDiceCommand(MessageDataDAO messageDataDAO) {
        this(messageDataDAO, new DiceParser(), new RandomNumberSupplier(), 1000);
    }

    @VisibleForTesting
    public CustomDiceCommand(MessageDataDAO messageDataDAO, Dice dice, NumberSupplier numberSupplier, int maxRolls) {
        super(messageDataDAO);
        this.diceSystemAdapter = new DiceSystemAdapter(numberSupplier, maxRolls, dice);
    }

    @Override
    protected Optional<ConfigAndState<CustomDiceConfig, StateData>> getMessageDataAndUpdateWithButtonValue(long channelId,
                                                                                                           long messageId,
                                                                                                           @NonNull String buttonValue,
                                                                                                           @NonNull String invokingUserName) {
        final Optional<MessageDataDTO> messageDataDTO = messageDataDAO.getDataForMessage(channelId, messageId);
        if (messageDataDTO.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(deserializeAndUpdateState(messageDataDTO.get(), buttonValue));
    }

    @VisibleForTesting
    ConfigAndState<CustomDiceConfig, StateData> deserializeAndUpdateState(@NonNull MessageDataDTO messageDataDTO, @NonNull String buttonValue) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageDataDTO.getConfigClassId()), "Unknown configClassId: %s", messageDataDTO.getConfigClassId());
        return new ConfigAndState<>(messageDataDTO.getConfigUUID(),
                Mapper.deserializeObject(messageDataDTO.getConfig(), CustomDiceConfig.class),
                new State<>(buttonValue, StateData.empty()));
    }

    @Override
    public Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID,
                                                                   long guildId,
                                                                   long channelId,
                                                                   long messageId,
                                                                   @NonNull CustomDiceConfig config,
                                                                   @Nullable State<StateData> state) {
        return Optional.of(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), CONFIG_TYPE_ID,
                Mapper.serializedObject(config),
                Mapper.NO_PERSISTED_STATE, null));
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure a custom set of dice buttons";
    }

    @Override
    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return List.of(CommandDefinitionOption.builder()
                .name(BUTTONS_COMMAND_OPTIONS_ID)
                .description("Define one or more buttons separated by ';'")
                .type(CommandDefinitionOption.Type.STRING)
                .required(true)
                .build());
    }

    @Override
    protected Collection<CommandDefinitionOption> additionalCommandOptions() {
        return List.of(CommandDefinitionOption.builder()
                .name(LEGACY_START_ACTION)
                .description("Old start command")
                .type(CommandDefinitionOption.Type.SUB_COMMAND)
                .options(LEGACY_DICE_COMMAND_OPTIONS_IDS.stream()
                        .map(id -> CommandDefinitionOption.builder()
                                .name(id)
                                .description("xdy for a set of x dice with y sides, e.g. '3d6'")
                                .type(CommandDefinitionOption.Type.STRING)
                                .build())
                        .collect(Collectors.toList()))
                .option(ANSWER_TARGET_CHANNEL_COMMAND_OPTION)
                .build());
    }


    @Override
    protected @NonNull EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Creates up to 25 buttons with custom dice expression e.g. '/custom_dice start buttons:3d6;10d10;3d20'. \n" + diceSystemAdapter.getHelpText(DiceParserSystem.DICE_EVALUATOR))
                .build();
    }

    @Override
    protected Set<String> getStartOptionIds() {
        return Set.of(ACTION_START, LEGACY_START_ACTION);
    }

    @Override
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options) {
        List<String> diceExpressionWithOptionalLabel = getButtonsFromCommandOption(options).stream()
                .map(ButtonIdAndExpression::getExpression)
                .distinct()
                .collect(Collectors.toList());
        DiceParserSystem diceParserSystem = options.getName().equals(LEGACY_START_ACTION) ? DiceParserSystem.DICEROLL_PARSER : DiceParserSystem.DICE_EVALUATOR;
        return diceSystemAdapter.validateListOfExpressions(diceExpressionWithOptionalLabel, "/custom_dice help", diceParserSystem);

    }

    private List<ButtonIdAndExpression> getButtonsFromCommandOption(@NonNull CommandInteractionOption options) {
        if (LEGACY_START_ACTION.equals(options.getName())) {
            return LEGACY_DICE_COMMAND_OPTIONS_IDS.stream()
                    .flatMap(id -> options.getStringSubOptionWithName(id).stream()
                            .map(e -> new ButtonIdAndExpression(id, e)))
                    .collect(Collectors.toList());
        }
        ImmutableList.Builder<ButtonIdAndExpression> builder = ImmutableList.builder();
        String buttons = options.getStringSubOptionWithName(BUTTONS_COMMAND_OPTIONS_ID).orElseThrow();
        int idCounter = 1;
        for (String button : buttons.split(";")) {
            builder.add(new ButtonIdAndExpression(idCounter++ + "_button", button));
        }
        return builder.build();
    }

    protected @NonNull CustomDiceConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        if (LEGACY_START_ACTION.equals(options.getName())) {
            return getConfigOptionStringList(getButtonsFromCommandOption(options), getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null),
                    DiceParserSystem.DICEROLL_PARSER);
        }
        return getConfigOptionStringList(getButtonsFromCommandOption(options), getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null), DiceParserSystem.DICE_EVALUATOR);
    }

    @VisibleForTesting
    CustomDiceConfig getConfigOptionStringList(List<ButtonIdAndExpression> startOptions, Long channelId, DiceParserSystem diceParserSystem) {
        return new CustomDiceConfig(channelId, startOptions.stream()
                .filter(be -> !be.getExpression().contains(BottomCustomIdUtils.CUSTOM_ID_DELIMITER))
                .filter(be -> !be.getExpression().contains(LABEL_DELIMITER) || be.getExpression().split(LABEL_DELIMITER).length == 2)
                .map(be -> {
                    if (be.getExpression().contains(LABEL_DELIMITER)) {
                        String[] split = be.getExpression().split(LABEL_DELIMITER);
                        return new ButtonIdLabelAndDiceExpression(be.getButtonId(), split[1].trim(), split[0].trim());
                    }
                    return new ButtonIdLabelAndDiceExpression(be.getButtonId(), be.getExpression().trim(), be.getExpression().trim());
                })
                .filter(s -> !s.getDiceExpression().isEmpty())
                .filter(s -> !s.getLabel().isEmpty())
                .filter(lv -> diceSystemAdapter.isValidExpression(lv.getDiceExpression(), diceParserSystem))
                .filter(s -> s.getDiceExpression().length() <= 2000) //limit of the discord message content
                .filter(s -> s.getLabel().length() <= 80) //https://discord.com/developers/docs/interactions/message-components#buttons
                .distinct()
                .limit(25)
                .collect(Collectors.toList()),
                diceParserSystem);
    }

    @Override
    protected @NonNull Optional<EmbedDefinition> getAnswer(CustomDiceConfig config, State<StateData> state) {
        Optional<ButtonIdLabelAndDiceExpression> selectedButton = Optional.ofNullable(state).map(State::getButtonValue)
                .flatMap(bv -> config.getButtonIdLabelAndDiceExpressions().stream()
                        .filter(bld -> bld.getButtonId().equals(bv))
                        .findFirst()
                );
        if (selectedButton.isEmpty()) {
            return Optional.empty();
        }
        //add the label only if it is different from the expression
        final String label = selectedButton.get().getDiceExpression().equals(selectedButton.get().getLabel()) ? null : selectedButton.get().getLabel();
        return Optional.of(diceSystemAdapter.answerRollWithGivenLabel(selectedButton.get().getDiceExpression(), label, false, config.getDiceParserSystem()));
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(CustomDiceConfig config, State<StateData> state) {
        return Optional.of(createNewButtonMessage(config));
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(CustomDiceConfig config) {
        return MessageDefinition.builder()
                .content(BUTTON_MESSAGE)
                .componentRowDefinitions(createButtonLayout(config))
                .build();
    }

    private List<ComponentRowDefinition> createButtonLayout(CustomDiceConfig config) {
        List<ButtonDefinition> buttons = config.getButtonIdLabelAndDiceExpressions().stream()
                .map(d -> ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), d.getButtonId()))
                        .label(d.getLabel())
                        .build())
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    @Override
    protected @NonNull CustomDiceConfig getConfigFromEvent(@NonNull ButtonEventAdaptor event) {
        String[] split = event.getCustomId().split(BottomCustomIdUtils.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
        Long answerTargetChannelId = getOptionalLongFromArray(split, 2);
        Deque<String> buttonIds = new ArrayDeque<>(IntStream.range(1, 26).mapToObj(i -> i + "_button").toList()); //legacy can have 25 buttons

        return new CustomDiceConfig(answerTargetChannelId, event.getAllButtonIds().stream()
                .map(lv -> new ButtonIdLabelAndDiceExpression(buttonIds.pop(), lv.getLabel(), BottomCustomIdUtils.getButtonValueFromLegacyCustomId(lv.getCustomId())))
                .collect(Collectors.toList()), DiceParserSystem.DICEROLL_PARSER);
    }

    @Override
    protected @NonNull State<StateData> getStateFromEvent(@NonNull ButtonEventAdaptor event) {
        final CustomDiceConfig config = getConfigFromEvent(event);
        final String buttonValue = BottomCustomIdUtils.getButtonValueFromLegacyCustomId(event.getCustomId());
        final String mappedButtonValue = config.getButtonIdLabelAndDiceExpressions().stream()
                .filter(bld -> bld.getDiceExpression().equals(buttonValue))
                .map(ButtonIdLabelAndDiceExpression::getButtonId)
                .findFirst()
                .orElseThrow();
        return new State<>(mappedButtonValue, StateData.empty());
    }

    @Override
    public String getCommandId() {
        return COMMAND_NAME;
    }

    @Value
    static class ButtonIdAndExpression {
        @NonNull
        String buttonId;
        @NonNull
        String expression;
    }

}
