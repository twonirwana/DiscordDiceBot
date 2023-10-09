package de.janno.discord.bot.command.customDice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.command.channelConfig.AliasHelper;
import de.janno.discord.bot.dice.*;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class CustomDiceCommand extends AbstractCommand<CustomDiceConfig, StateData> {

    static final String BUTTONS_COMMAND_OPTIONS_ID = "buttons";
    private static final String COMMAND_NAME = "custom_dice";
    private static final String LABEL_DELIMITER = "@";
    private static final String BUTTON_MESSAGE = "Click on a button to roll the dice";
    private static final String CONFIG_TYPE_ID = "CustomDiceConfig";
    private final DiceSystemAdapter diceSystemAdapter;

    public CustomDiceCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        this(persistenceManager, new DiceParser(), cachingDiceEvaluator);
    }

    @VisibleForTesting
    public CustomDiceCommand(PersistenceManager persistenceManager, Dice dice, CachingDiceEvaluator cachingDiceEvaluator) {
        super(persistenceManager);
        this.diceSystemAdapter = new DiceSystemAdapter(cachingDiceEvaluator, dice);
    }

    @Override
    protected ConfigAndState<CustomDiceConfig, StateData> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                                 @NonNull MessageDataDTO messageDataDTO,
                                                                                                 @NonNull String buttonValue,
                                                                                                 @NonNull String invokingUserName) {
        return deserializeAndUpdateState(messageConfigDTO, buttonValue);
    }


    @VisibleForTesting
    ConfigAndState<CustomDiceConfig, StateData> deserializeAndUpdateState(@NonNull MessageConfigDTO messageConfigDTO, @NonNull String buttonValue) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(),
                Mapper.deserializeObject(messageConfigDTO.getConfig(), CustomDiceConfig.class),
                new State<>(buttonValue, StateData.empty()));
    }

    @Override
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, long guildId, long channelId, @NonNull CustomDiceConfig config) {
        return Optional.of(new MessageConfigDTO(configUUID, guildId, channelId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure dice buttons like: 1d6;2d8=;1d10+10=";
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
    protected @NonNull EmbedOrMessageDefinition getHelpMessage() {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Creates up to 25 buttons with custom dice expression.\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field("Example", "`/custom_dice start buttons:3d6;10d10;3d20`", false))
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server for Help and News", "https://discord.gg/e43BsqKpFr", false))
                .build();
    }

    @Override
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options, long channelId, long userId) {
        List<String> diceExpressionWithOptionalLabel = getButtonsFromCommandOption(options).stream()
                .map(ButtonIdAndExpression::getExpression)
                .map(e -> AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, e))
                .distinct()
                .collect(Collectors.toList());
        DiceParserSystem diceParserSystem = DiceParserSystem.DICE_EVALUATOR;
        return diceSystemAdapter.validateListOfExpressions(diceExpressionWithOptionalLabel, "/custom_dice help", diceParserSystem);

    }

    private List<ButtonIdAndExpression> getButtonsFromCommandOption(@NonNull CommandInteractionOption options) {
        ImmutableList.Builder<ButtonIdAndExpression> builder = ImmutableList.builder();
        String buttons = options.getStringSubOptionWithName(BUTTONS_COMMAND_OPTIONS_ID).orElseThrow();
        int idCounter = 1;
        for (String button : buttons.split(";")) {
            builder.add(new ButtonIdAndExpression(idCounter++ + "_button", button));
        }
        return builder.build();
    }

    protected @NonNull CustomDiceConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        return getConfigOptionStringList(getButtonsFromCommandOption(options),
                BaseCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null),
                BaseCommandOptions.getAnswerTypeFromStartCommandOption(options).orElse(defaultAnswerFormat()),
                BaseCommandOptions.getDiceStyleOptionFromStartCommandOption(options).orElse(DiceImageStyle.polyhedral_3d),
                BaseCommandOptions.getDiceColorOptionFromStartCommandOption(options).orElse(DiceImageStyle.polyhedral_3d.getDefaultColor())
        );
    }

    @VisibleForTesting
    CustomDiceConfig getConfigOptionStringList(List<ButtonIdAndExpression> startOptions,
                                               Long channelId,
                                               AnswerFormatType answerFormatType,
                                               DiceImageStyle diceImageStyle,
                                               String defaultDiceColor) {
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
                .filter(s -> s.getDiceExpression().length() <= 2000) //limit of the discord message content
                .distinct()
                .limit(25)
                .collect(Collectors.toList()),
                DiceParserSystem.DICE_EVALUATOR,
                answerFormatType,
                null,
                new DiceStyleAndColor(diceImageStyle, defaultDiceColor)
        );
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(CustomDiceConfig config, State<StateData> state, long channelId, long userId) {
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
        final String expression = AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, selectedButton.get().getDiceExpression());
        return Optional.of(diceSystemAdapter.answerRollWithGivenLabel(expression,
                label,
                false,
                config.getDiceParserSystem(),
                config.getAnswerFormatType(),
                config.getDiceStyleAndColor()));
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(UUID configUUID, CustomDiceConfig config, State<StateData> state, long guildId, long channelId) {
        return Optional.of(createNewButtonMessage(configUUID, config));
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(UUID configUUID, CustomDiceConfig config) {
        return MessageDefinition.builder()
                .content(BUTTON_MESSAGE)
                .componentRowDefinitions(createButtonLayout(configUUID, config))
                .build();
    }

    private List<ComponentRowDefinition> createButtonLayout(UUID configUUID, CustomDiceConfig config) {
        List<ButtonDefinition> buttons = config.getButtonIdLabelAndDiceExpressions().stream()
                .map(d -> ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), d.getButtonId(), configUUID))
                        .label(d.getLabel())
                        .build())
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    @Override
    public @NonNull String getCommandId() {
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
