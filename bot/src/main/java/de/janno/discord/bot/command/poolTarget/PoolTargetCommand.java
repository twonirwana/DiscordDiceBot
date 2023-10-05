package de.janno.discord.bot.command.poolTarget;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceUtils;
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
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class PoolTargetCommand extends AbstractCommand<PoolTargetConfig, PoolTargetStateData> {

    static final String SUBSET_DELIMITER = ";";
    static final String SIDES_OF_DIE_OPTION = "sides";
    static final String MAX_DICE_OPTION = "max_dice";
    static final String REROLL_SET_OPTION = "reroll_set";
    static final String BOTCH_SET_OPTION = "botch_set";
    static final String REROLL_VARIANT_OPTION = "reroll_variant";
    private static final String COMMAND_NAME = "pool_target";
     static final String ALWAYS_REROLL = "always";
    private static final String ASK_FOR_REROLL = "ask";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String DO_REROLL_ID = "do_reroll";
    private static final String DO_NOT_REROLL_ID = "no_reroll";
    private static final long MAX_NUMBER_OF_DICE = 25;
    private static final String DICE_SYMBOL = "d";

    private static final String CONFIG_TYPE_ID = "PoolTargetConfig";
    private static final String STATE_DATA_TYPE_ID = "PoolTargetStateData";
    private final DiceUtils diceUtils;

    public PoolTargetCommand(PersistenceManager persistenceManager) {
        this(persistenceManager, new DiceUtils());
    }

    @VisibleForTesting
    public PoolTargetCommand(PersistenceManager persistenceManager, DiceUtils diceUtils) {
        super(persistenceManager);
        this.diceUtils = diceUtils;
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Roll dice and against a variable target";
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage() {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Use '/pool_target start' to get message, where the user can roll dice")
                .field(new EmbedOrMessageDefinition.Field("Example", "`/pool_target start sides:10 max_dice:15 reroll_set:10 botch_set:1 reroll_variant:ask`", false))
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server for Help and News", "https://discord.gg/e43BsqKpFr", false))
                .build();
    }

    @Override
    public @NonNull String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected ConfigAndState<PoolTargetConfig, PoolTargetStateData> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                                           @NonNull MessageDataDTO messageDataDTO,
                                                                                                           @NonNull String buttonValue,
                                                                                                           @NonNull String invokingUserName) {
        return deserializeAndUpdateState(messageConfigDTO, messageDataDTO, buttonValue);
    }

    @VisibleForTesting
    ConfigAndState<PoolTargetConfig, PoolTargetStateData> deserializeAndUpdateState(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                    @NonNull MessageDataDTO messageDataDTO,
                                                                                    @NonNull String buttonValue) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        Preconditions.checkArgument(Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId)
                .map(c -> Set.of(STATE_DATA_TYPE_ID, Mapper.NO_PERSISTED_STATE).contains(c))
                .orElse(true), "Unknown stateDataClassId: %s", Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId).orElse("null"));

        final PoolTargetStateData loadedStateData = Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateData)
                .map(sd -> Mapper.deserializeObject(sd, PoolTargetStateData.class))
                .orElse(null);
        final PoolTargetConfig loadedConfig = Mapper.deserializeObject(messageConfigDTO.getConfig(), PoolTargetConfig.class);
        final PoolTargetStateData updatedData = updatePoolTargetStateData(loadedConfig,
                buttonValue,
                Optional.ofNullable(loadedStateData).map(PoolTargetStateData::getDicePool).orElse(null),
                Optional.ofNullable(loadedStateData).map(PoolTargetStateData::getTargetNumber).orElse(null));
        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(), loadedConfig, new State<>(buttonValue, updatedData));
    }

    @Override
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, long guildId, long channelId, @NonNull PoolTargetConfig config) {
        return Optional.of(new MessageConfigDTO(configUUID, guildId, channelId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected void updateCurrentMessageStateData(UUID configUUID, long guildId, long channelId, long messageId, @NonNull PoolTargetConfig config, @NonNull State<PoolTargetStateData> state) {
        Optional<PoolTargetStateData> stateData = Optional.ofNullable(state.getData());
        if (stateData.map(PoolTargetStateData::getDicePool).isPresent() &&
                stateData.map(PoolTargetStateData::getTargetNumber).isPresent() &&
                stateData.map(PoolTargetStateData::getDoReroll).isPresent()) {
            persistenceManager.deleteStateForMessage(channelId, messageId);
            //message data so we knew the button message exists
            persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), Mapper.NO_PERSISTED_STATE, null));
        } else if (state.getData() != null) {
            persistenceManager.deleteStateForMessage(channelId, messageId);
            persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), STATE_DATA_TYPE_ID, Mapper.serializedObject(state.getData())));
        }

    }

    @Override
    protected boolean supportsResultImages() {
        return false;
    }

    @Override
    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of(
                CommandDefinitionOption.builder()
                        .name(SIDES_OF_DIE_OPTION)
                        .required(true)
                        .description("Dice sides")
                        .type(CommandDefinitionOption.Type.INTEGER)
                        .minValue(2L)
                        .maxValue(25L).build(),
                CommandDefinitionOption.builder()
                        .name(MAX_DICE_OPTION)
                        .required(false)
                        .description("Max number of dice")
                        .type(CommandDefinitionOption.Type.INTEGER)
                        .minValue(1L)
                        .maxValue(MAX_NUMBER_OF_DICE)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(REROLL_SET_OPTION)
                        .required(false)
                        .description("Result numbers that are reroll, seperated by ','")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(BOTCH_SET_OPTION)
                        .required(false)
                        .description("Failure dice numbers, seperated by ','")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(REROLL_VARIANT_OPTION)
                        .required(false)
                        .description("Options for special reroll handling")
                        .type(CommandDefinitionOption.Type.STRING)
                        .choice(CommandDefinitionOptionChoice.builder().name(ALWAYS_REROLL).value(ALWAYS_REROLL).build())
                        .choice(CommandDefinitionOptionChoice.builder().name(ASK_FOR_REROLL).value(ASK_FOR_REROLL).build())
                        .build()
        );
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(PoolTargetConfig config, State<PoolTargetStateData> state, long channelId, long userId) {
        Optional<PoolTargetStateData> stateData = Optional.ofNullable(state.getData());
        if (stateData.map(PoolTargetStateData::getDicePool).isEmpty() ||
                stateData.map(PoolTargetStateData::getTargetNumber).isEmpty() ||
                stateData.map(PoolTargetStateData::getDoReroll).isEmpty()) {
            return Optional.empty();
        }
        List<Integer> rollResult = diceUtils.rollDiceOfType(state.getData().getDicePool(), config.getDiceSides());
        if (state.getData().getDoReroll()) {
            rollResult = diceUtils.explodingReroll(config.getDiceSides(), rollResult, config.getRerollSet());
        }
        rollResult = rollResult.stream().sorted().collect(Collectors.toList());
        int numberOfSuccesses = DiceUtils.numberOfDiceResultsGreaterEqual(rollResult, state.getData().getTargetNumber());
        int numberOfBotches = DiceUtils.numberOfDiceResultsEqual(rollResult, config.getBotchSet());

        int totalResults = numberOfSuccesses - numberOfBotches;

        Set<Integer> toMark = IntStream.range(state.getData().getTargetNumber(), config.getDiceSides() + 1).boxed().collect(Collectors.toSet());
        toMark.addAll(config.getBotchSet());
        if (state.getData().getDoReroll()) {
            toMark.addAll(config.getRerollSet());
        }
        return Optional.of(RollAnswer.builder()
                .answerFormatType(config.getAnswerFormatType())
                .expression(String.format("%dd%d ≥%d", state.getData().getDicePool(), config.getDiceSides(), state.getData().getTargetNumber()))
                .result(String.valueOf(totalResults))
                .rollDetails(CommandUtils.markIn(rollResult, toMark))
                .build());
    }

    private PoolTargetStateData updatePoolTargetStateData(PoolTargetConfig config,
                                                          String buttonValue,
                                                          Integer dicePool,
                                                          Integer targetNumber) {
        if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            return new PoolTargetStateData(null, null, null);
        }
        //pool size in config is empty and button value is number -> pool size was set
        if (dicePool == null && StringUtils.isNumeric(buttonValue)) {
            Integer buttonNumber = Integer.valueOf(buttonValue);
            return new PoolTargetStateData(buttonNumber, null, null);
        }

        //pool size is already given and button value is number -> target was set
        if (dicePool != null && StringUtils.isNumeric(buttonValue)) {
            //if the config is always reroll we can set it, else we need to ask
            Boolean doReroll = ALWAYS_REROLL.equals(config.getRerollVariant()) ? true : null;
            return new PoolTargetStateData(dicePool, Integer.valueOf(buttonValue), doReroll);
        }

        //pool size is already given and target is given -> do reroll was asked
        if (dicePool != null && targetNumber != null) {
            boolean doReroll = DO_REROLL_ID.equals(buttonValue);
            return new PoolTargetStateData(dicePool, targetNumber, doReroll);
        }

        return new PoolTargetStateData(null, null, null);
    }

    @Override
    protected @NonNull PoolTargetConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        int sideValue = Math.toIntExact(options.getLongSubOptionWithName(SIDES_OF_DIE_OPTION)
                .map(l -> Math.min(l, 1000))
                .orElse(10L));
        int maxButton = Math.toIntExact(options.getLongSubOptionWithName(MAX_DICE_OPTION)
                .map(l -> Math.min(l, 1000))
                .orElse(10L));

        Set<Integer> rerollSet = CommandUtils.getSetFromCommandOptions(options, REROLL_SET_OPTION, ",");
        Set<Integer> botchSet = CommandUtils.getSetFromCommandOptions(options, BOTCH_SET_OPTION, ",");
        String rerollVariant = options.getOptions().stream()
                .filter(o -> REROLL_VARIANT_OPTION.equals(o.getName()))
                .map(CommandInteractionOption::getStringValue)
                .findFirst()
                .orElse(ALWAYS_REROLL);
        Long answerTargetChannelId = BaseCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null);
        AnswerFormatType answerType = BaseCommandOptions.getAnswerTypeFromStartCommandOption(options).orElse(defaultAnswerFormat());
        return new PoolTargetConfig(answerTargetChannelId,
                sideValue,
                maxButton,
                rerollSet,
                botchSet,
                rerollVariant,
                answerType,
                null,
                new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor())
        );
    }


    private String getConfigDescription(PoolTargetConfig config) {
        String rerollDescription = null;
        String botchDescription = null;
        if (!config.getRerollSet().isEmpty()) {
            rerollDescription = String.format("%s reroll:%s", config.getRerollVariant(),
                    config.getRerollSet().stream().sorted().map(String::valueOf).collect(Collectors.joining(",")));
        }
        if (!config.getBotchSet().isEmpty()) {
            botchDescription = "botch:" + config.getBotchSet().stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
        }
        String configDescription = Stream.of(rerollDescription, botchDescription)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" and "));
        if (!configDescription.isEmpty()) {
            configDescription = String.format(", with %s", configDescription);
        }
        return configDescription;
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(UUID configUUID, PoolTargetConfig config) {
        String configDescription = getConfigDescription(config);
        return MessageDefinition.builder()
                .content(String.format("Click on the buttons to roll dice%s", configDescription))
                .componentRowDefinitions(createPoolButtonLayout(configUUID, config))
                .build();
    }


    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID configUUID, PoolTargetConfig config, State<PoolTargetStateData> state, long channelId, long userId) {
        if (Optional.ofNullable(state.getData()).map(PoolTargetStateData::getDicePool).orElse(null) == null && !CLEAR_BUTTON_ID.equals(state.getButtonValue())) {
            return Optional.empty();
        }
        return Optional.of(getButtonLayoutWithState(configUUID, state, config));
    }

    @Override
    public @NonNull Optional<String> getCurrentMessageContentChange(PoolTargetConfig config, State<PoolTargetStateData> state) {
        if (CLEAR_BUTTON_ID.equals(state.getButtonValue())) {
            return Optional.of(String.format("Click on the buttons to roll dice%s", getConfigDescription(config)));
        }
        if (Optional.ofNullable(state.getData()).map(PoolTargetStateData::getDicePool).orElse(null) != null && state.getData().getTargetNumber() != null && state.getData().getDoReroll() == null) {
            String rerollNumbers = config.getRerollSet().stream()
                    .map(String::valueOf)
                    .map(s -> String.format("%ss", s))
                    .collect(Collectors.joining(","));
            return Optional.of(String.format("Should %s in %dd%d against %d be be rerolled?", rerollNumbers, state.getData().getDicePool(), config.getDiceSides(), state.getData().getTargetNumber()));
        }
        if (Optional.ofNullable(state.getData()).map(PoolTargetStateData::getDicePool).orElse(null) != null && state.getData().getTargetNumber() == null) {
            String configDescription = getConfigDescription(config);
            return Optional.of(String.format("Click on the target to roll %dd%d against it%s", state.getData().getDicePool(), config.getDiceSides(), configDescription));
        }

        return Optional.empty();
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(UUID configUUID, PoolTargetConfig config, State<PoolTargetStateData> state, long guildId, long channelId) {
        if (Optional.ofNullable(state.getData()).map(PoolTargetStateData::getDicePool).orElse(null) != null && state.getData().getTargetNumber() != null && state.getData().getDoReroll() != null) {
            return Optional.of(MessageDefinition.builder()
                    .content(String.format("Click on the buttons to roll dice%s", getConfigDescription(config)))
                    .componentRowDefinitions(getButtonLayoutWithState(configUUID, state, config))
                    .build());
        }
        return Optional.empty();
    }

    private List<ComponentRowDefinition> getButtonLayoutWithState(UUID configUUID, State<PoolTargetStateData> state, PoolTargetConfig config) {
        if (Optional.ofNullable(state.getData()).map(PoolTargetStateData::getDicePool).orElse(null) != null &&
                state.getData().getTargetNumber() != null && state.getData().getDoReroll() == null) {
            return ImmutableList.of(
                    ComponentRowDefinition.builder()
                            .buttonDefinition(
                                    ButtonDefinition.builder()
                                            .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), DO_REROLL_ID, configUUID))
                                            .label("Reroll")
                                            .build())
                            .buttonDefinition(
                                    ButtonDefinition.builder()
                                            .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), DO_NOT_REROLL_ID, configUUID))
                                            .label("No reroll")
                                            .build())
                            .build()
            );
        }
        if (Optional.ofNullable(state.getData()).map(PoolTargetStateData::getDicePool).orElse(null) != null &&
                state.getData().getTargetNumber() == null) {
            List<ButtonDefinition> buttons = IntStream.range(2, config.getDiceSides() + 1)
                    .mapToObj(i -> ButtonDefinition.builder()
                            .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), String.valueOf(i), configUUID))
                            .label(String.format("%d", i))
                            .build()
                    )
                    .collect(Collectors.toList());
            buttons.add(ButtonDefinition.builder()
                    .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), CLEAR_BUTTON_ID, configUUID))
                    .label("Clear")
                    .build());
            return Lists.partition(buttons, 5).stream()
                    .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build()).collect(Collectors.toList());
        }
        return createPoolButtonLayout(configUUID, config);
    }


    private List<ComponentRowDefinition> createPoolButtonLayout(UUID configUUID, PoolTargetConfig config) {
        List<ButtonDefinition> buttons = IntStream.range(1, config.getMaxNumberOfButtons() + 1)
                .mapToObj(i -> ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), String.valueOf(i), configUUID))
                        .label(String.format("%d%s%s", i, DICE_SYMBOL, config.getDiceSides()))
                        .build())
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    @Override
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options, long channelId, long userId) {
        Optional<String> botchSetValidation = CommandUtils.validateIntegerSetFromCommandOptions(options, BOTCH_SET_OPTION, ",");
        if (botchSetValidation.isPresent()) {
            return botchSetValidation;
        }
        Optional<String> rerollSetValidation = CommandUtils.validateIntegerSetFromCommandOptions(options, REROLL_SET_OPTION, ",");
        if (rerollSetValidation.isPresent()) {
            return rerollSetValidation;
        }
        PoolTargetConfig conf = getConfigFromStartOptions(options);
        return validate(conf);
    }

    @VisibleForTesting
    Optional<String> validate(PoolTargetConfig config) {

        if (config.getRerollSet().stream().anyMatch(i -> i > config.getDiceSides())) {
            return Optional.of(String.format("Reroll set %s contains a number bigger then the sides of the die %s", config.getRerollSet(), config.getDiceSides()));
        }
        if (config.getBotchSet().stream().anyMatch(i -> i > config.getDiceSides())) {
            return Optional.of(String.format("Botch set %s contains a number bigger then the sides of the die %s", config.getBotchSet(), config.getDiceSides()));
        }
        if (config.getRerollSet().size() >= config.getDiceSides()) {
            return Optional.of("The reroll must not contain all numbers");
        }

        return Optional.empty();
    }


}
