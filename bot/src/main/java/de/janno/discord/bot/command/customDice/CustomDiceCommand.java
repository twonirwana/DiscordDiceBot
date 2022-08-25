package de.janno.discord.bot.command.customDice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceParserHelper;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.BotConstants;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class CustomDiceCommand extends AbstractCommand<CustomDiceConfig, EmptyData> {

    //todo expression not in button value. The button value is only the key
    private static final String COMMAND_NAME = "custom_dice";
    private static final List<String> DICE_COMMAND_OPTIONS_IDS = IntStream.range(1, 25).mapToObj(i -> i + "_button").toList();
    private static final String LABEL_DELIMITER = "@";
    private static final String BUTTON_MESSAGE = "Click on a button to roll the dice";
    private static final String CONFIG_TYPE_ID = "CustomDiceConfig";
    private final DiceParserHelper diceParserHelper;

    public CustomDiceCommand(MessageDataDAO messageDataDAO) {
        this(messageDataDAO, new DiceParserHelper());
    }

    @VisibleForTesting
    public CustomDiceCommand(MessageDataDAO messageDataDAO, DiceParserHelper diceParserHelper) {
        super(messageDataDAO);
        this.diceParserHelper = diceParserHelper;
    }

    @Override
    protected Optional<ConfigAndState<CustomDiceConfig, EmptyData>> getMessageDataAndUpdateWithButtonValue(long channelId,
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
    ConfigAndState<CustomDiceConfig, EmptyData> deserializeAndUpdateState(@NonNull MessageDataDTO messageDataDTO, @NonNull String buttonValue) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageDataDTO.getConfigClassId()), "Unknown configClassId: %s", messageDataDTO.getConfigClassId());
        return new ConfigAndState<>(messageDataDTO.getConfigUUID(),
                Mapper.deserializeObject(messageDataDTO.getConfig(), CustomDiceConfig.class),
                new State<>(buttonValue, new EmptyData()));
    }

    @Override
    public Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID,
                                                                   long channelId,
                                                                   long messageId,
                                                                   @NonNull Config config,
                                                                   @Nullable State<EmptyData> state) {
        Preconditions.checkArgument(config instanceof CustomDiceConfig, "Wrong config: %s", config);
        return Optional.of(new MessageDataDTO(configUUID, channelId, messageId, getCommandId(), CONFIG_TYPE_ID,
                Mapper.serializedObject(config),
                Mapper.NO_PERSISTED_STATE, null));
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure a custom set of dice buttons";
    }

    @Override
    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return DICE_COMMAND_OPTIONS_IDS.stream()
                .map(id -> CommandDefinitionOption.builder()
                        .name(id)
                        .description("xdy for a set of x dice with y sides, e.g. '3d6'")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    protected @NonNull EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Creates up to 25 buttons with custom dice expression e.g. '/custom_dice start 1_button:3d6 2_button:10d10 3_button:3d20'. \n" + DiceParserHelper.HELP)
                .build();
    }

    @Override
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options) {
        List<String> diceExpressionWithOptionalLabel = DICE_COMMAND_OPTIONS_IDS.stream()
                .flatMap(id -> options.getStringSubOptionWithName(id).stream())
                .distinct()
                .collect(Collectors.toList());
        String answerTargetChannel = getAnswerTargetChannelIdFromStartCommandOption(options).map(Object::toString).orElse("");
        int maxCharacter = 100 - COMMAND_NAME.length()
                - 2 // delimiter;
                - answerTargetChannel.length();
        return diceParserHelper.validateListOfExpressions(diceExpressionWithOptionalLabel, LABEL_DELIMITER, BotConstants.CUSTOM_ID_DELIMITER, "/custom_dice help", maxCharacter);
    }

    protected @NonNull CustomDiceConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        return getConfigOptionStringList(DICE_COMMAND_OPTIONS_IDS.stream()
                .flatMap(id -> options.getStringSubOptionWithName(id).stream())
                .collect(Collectors.toList()), getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null));

    }

    @VisibleForTesting
    CustomDiceConfig getConfigOptionStringList(List<String> startOptions, Long channelId) {
        return new CustomDiceConfig(channelId, startOptions.stream()
                .filter(s -> !s.contains(BotConstants.CUSTOM_ID_DELIMITER))
                .filter(s -> !s.contains(LABEL_DELIMITER) || s.split(LABEL_DELIMITER).length == 2)
                .map(s -> {
                    if (s.contains(LABEL_DELIMITER)) {
                        String[] split = s.split(LABEL_DELIMITER);
                        return new LabelAndDiceExpression(split[1].trim(), split[0].trim());
                    }
                    return new LabelAndDiceExpression(s.trim(), s.trim());
                })
                .filter(s -> !s.getDiceExpression().isEmpty())
                .filter(s -> !s.getLabel().isEmpty())
                .filter(lv -> diceParserHelper.validExpression(lv.getDiceExpression()))
                .filter(s -> createButtonCustomId(s.getDiceExpression()).length() <= 100) //limit for the ids are 100 characters and we need also some characters for the type...
                .filter(s -> s.getLabel().length() <= 80) //https://discord.com/developers/docs/interactions/message-components#buttons
                .distinct()
                .limit(25)
                .collect(Collectors.toList()));
    }

    @Override
    protected @NonNull Optional<EmbedDefinition> getAnswer(CustomDiceConfig config, State<EmptyData> state) {
        String label = config.getLabelAndExpression().stream()
                .filter(ld -> !ld.getDiceExpression().equals(ld.getLabel()))
                .filter(ld -> ld.getDiceExpression().equals(state.getButtonValue()))
                .map(LabelAndDiceExpression::getLabel)
                .findFirst().orElse(null);
        return Optional.of(diceParserHelper.roll(state.getButtonValue(), label));
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(CustomDiceConfig config, State<EmptyData> state) {
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
        List<ButtonDefinition> buttons = config.getLabelAndExpression().stream()
                .map(d -> ButtonDefinition.builder()
                        .id(createButtonCustomId(d.getDiceExpression()))
                        .label(d.getLabel())
                        .build())
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    @Override
    protected @NonNull CustomDiceConfig getConfigFromEvent(@NonNull IButtonEventAdaptor event) {
        String[] split = event.getCustomId().split(BotConstants.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
        Long answerTargetChannelId = getOptionalLongFromArray(split, 2);
        return new CustomDiceConfig(answerTargetChannelId, event.getAllButtonIds().stream()
                .map(lv -> new LabelAndDiceExpression(lv.getLabel(), getButtonValueFromLegacyCustomId(lv.getCustomId())))
                .collect(Collectors.toList()));
    }

    @Override
    protected @NonNull State<EmptyData> getStateFromEvent(@NonNull IButtonEventAdaptor event) {
        return new State<>(getButtonValueFromLegacyCustomId(event.getCustomId()), new EmptyData());
    }

    @Override
    public String getCommandId() {
        return COMMAND_NAME;
    }

}
