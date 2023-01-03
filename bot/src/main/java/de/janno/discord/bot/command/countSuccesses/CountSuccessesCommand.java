package de.janno.discord.bot.command.countSuccesses;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
public class CountSuccessesCommand extends AbstractCommand<CountSuccessesConfig, StateData> {

    static final String SUBSET_DELIMITER = ";";
    private static final String COMMAND_NAME = "count_successes";
    private static final String ACTION_SIDE_OPTION = "dice_sides";
    private static final String ACTION_TARGET_OPTION = "target_number";
    private static final String ACTION_MAX_DICE_OPTION = "max_dice";
    private static final String ACTION_MIN_DICE_COUNT_OPTION = "min_dice_count";
    private static final String ACTION_REROLL_SET_OPTION = "reroll_set";
    private static final String ACTION_BOTCH_SET_OPTION = "botch_set";
    private static final long MAX_NUMBER_OF_DICE = 25;
    private static final String ACTION_GLITCH_OPTION = "glitch";
    private static final long MAX_NUMBER_SIDES_OR_TARGET_NUMBER = 1000;
    private static final String GLITCH_OPTION_HALF_ONES = "half_dice_one";
    private static final String GLITCH_NO_OPTION = "no_glitch";
    private static final String GLITCH_COUNT_ONES = "count_ones";
    @Deprecated
    //replaced with the botch set
    private static final String GLITCH_SUBTRACT_ONES = "subtract_ones";
    private final static String CONFIG_TYPE_ID = "CountSuccessesConfig";
    private final DiceUtils diceUtils;

    public CountSuccessesCommand(MessageDataDAO messageDataDAO) {
        this(messageDataDAO, new DiceUtils());
    }

    @VisibleForTesting
    public CountSuccessesCommand(MessageDataDAO messageDataDAO, DiceUtils diceUtils) {
        super(messageDataDAO);
        this.diceUtils = diceUtils;
    }

    private static String createButtonLabel(String value, CountSuccessesConfig config) {
        return String.format("%sd%s", value, config.getDiceSides());
    }

    @Override
    protected Optional<ConfigAndState<CountSuccessesConfig, StateData>> getMessageDataAndUpdateWithButtonValue(long channelId,
                                                                                                               long messageId,
                                                                                                               @NonNull String buttonValue,
                                                                                                               @NonNull String invokingUserName) {
        final Optional<MessageDataDTO> messageDataDTO = messageDataDAO.getDataForMessage(channelId, messageId);
        return messageDataDTO.map(dataDTO -> deserializeAndUpdateState(dataDTO, buttonValue));
    }

    @VisibleForTesting
    ConfigAndState<CountSuccessesConfig, StateData> deserializeAndUpdateState(@NonNull MessageDataDTO messageDataDTO, @NonNull String buttonValue) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageDataDTO.getConfigClassId()), "Unknown configClassId: %s", messageDataDTO.getConfigClassId());
        return new ConfigAndState<>(messageDataDTO.getConfigUUID(),
                Mapper.deserializeObject(messageDataDTO.getConfig(), CountSuccessesConfig.class),
                new State<>(buttonValue, StateData.empty()));
    }

    @Override
    public Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID,
                                                                   long guildId,
                                                                   long channelId,
                                                                   long messageId,
                                                                   @NonNull CountSuccessesConfig config,
                                                                   @Nullable State<StateData> state) {
        return Optional.of(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(),
                CONFIG_TYPE_ID, Mapper.serializedObject(config),
                Mapper.NO_PERSISTED_STATE, null));
    }

    @Override
    protected @NonNull CountSuccessesConfig getConfigFromEvent(@NonNull ButtonEventAdaptor event) {
        String[] split = event.getCustomId().split(BottomCustomIdUtils.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
        int sideOfDie = Integer.parseInt(split[2]);
        int target = Integer.parseInt(split[3]);
        //legacy message could be missing the glitch and max dice option
        String glitchOption = split.length < 5 ? GLITCH_NO_OPTION : split[4];
        int maxNumberOfButtons = split.length < 6 ? 15 : Integer.parseInt(split[5]);
        Long answerTargetChannelId = getOptionalLongFromArray(split, 6);
        return new CountSuccessesConfig(answerTargetChannelId, sideOfDie, target, glitchOption, maxNumberOfButtons, 1, Set.of(), Set.of(), AnswerFormatType.full);
    }

    @Override
    protected @NonNull State<StateData> getStateFromEvent(@NonNull ButtonEventAdaptor event) {
        return new State<>(BottomCustomIdUtils.getButtonValueFromLegacyCustomId(event.getCustomId()), StateData.empty());
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure buttons for dice, with the same side, that counts successes against a target number";
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage() {
        return EmbedOrMessageDefinition.builder().descriptionOrContent("Use '/count_successes start dice_sides:X target_number:Y' to get Buttons that roll with X sided dice against the target of Y and count the successes. A successes are all dice that have a result greater or equal then the target number")
                .field(new EmbedOrMessageDefinition.Field("Example", "`/count_successes start dice_sides:10 target_number:7`", false))
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server", "https://discord.gg/e43BsqKpFr", false))
                .build();
    }

    @Override
    public String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of(CommandDefinitionOption.builder()
                        .name(ACTION_SIDE_OPTION)
                        .required(true)
                        .description("Dice side")
                        .type(CommandDefinitionOption.Type.INTEGER)
                        .minValue(2L)
                        .maxValue(MAX_NUMBER_SIDES_OR_TARGET_NUMBER).build(),
                CommandDefinitionOption.builder()
                        .name(ACTION_TARGET_OPTION)
                        .required(true)
                        .description("Target number")
                        .type(CommandDefinitionOption.Type.INTEGER)
                        .minValue(1L)
                        .maxValue(MAX_NUMBER_SIDES_OR_TARGET_NUMBER)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(ACTION_GLITCH_OPTION)
                        .required(false)
                        .description("Glitch option")
                        .type(CommandDefinitionOption.Type.STRING)
                        .choice(CommandDefinitionOptionChoice.builder().name(GLITCH_OPTION_HALF_ONES).value(GLITCH_OPTION_HALF_ONES).build())
                        .choice(CommandDefinitionOptionChoice.builder().name(GLITCH_COUNT_ONES).value(GLITCH_COUNT_ONES).build())
                        .build(),
                CommandDefinitionOption.builder()
                        .name(ACTION_MAX_DICE_OPTION)
                        .required(false)
                        .description("Max number of dice")
                        .type(CommandDefinitionOption.Type.INTEGER)
                        .minValue(1L)
                        .maxValue(MAX_NUMBER_OF_DICE)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(ACTION_MIN_DICE_COUNT_OPTION)
                        .required(false)
                        .description("The minimal number of dice")
                        .type(CommandDefinitionOption.Type.INTEGER)
                        .minValue(1L)
                        .maxValue(100L)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(ACTION_REROLL_SET_OPTION)
                        .required(false)
                        .description("Result numbers that are reroll, seperated by ','")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(ACTION_BOTCH_SET_OPTION)
                        .required(false)
                        .description("Failure dice numbers, seperated by ','")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build()
        );
    }

    @Override
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options) {
        long sideValue = options.getLongSubOptionWithName(ACTION_SIDE_OPTION).orElseThrow();
        int rerollSetSize = CommandUtils.getSetFromCommandOptions(options, ACTION_REROLL_SET_OPTION, ",").size();
        if ((rerollSetSize * 2L) >= sideValue) {
            return Optional.of("The reroll set must be smaller then half the number of dice sides");
        }
        return Optional.empty();
    }


    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(CountSuccessesConfig config, State<StateData> state) {
        final int numberOfDice = Integer.parseInt(state.getButtonValue());

        final List<Integer> rollResult = diceUtils.explodingReroll(config.getDiceSides(), diceUtils.rollDiceOfType(numberOfDice, config.getDiceSides()), config.getRerollSet()).stream()
                .sorted().collect(Collectors.toList());
        final int numberOfSuccesses = DiceUtils.numberOfDiceResultsGreaterEqual(rollResult, config.getTarget());
        final Set<Integer> botchSet = GLITCH_SUBTRACT_ONES.equals(config.getGlitchOption()) ? Set.of(1) : config.getBotchSet();
        final int numberOfBotches = DiceUtils.numberOfDiceResultsEqual(rollResult, botchSet);
        Set<Integer> toMark = IntStream.range(config.getTarget(), config.getDiceSides() + 1).boxed().collect(Collectors.toSet());
        toMark.addAll(botchSet);
        toMark.addAll(config.getRerollSet());

        final int totalResults = numberOfSuccesses - numberOfBotches;

        final String glitchResult;
        final String glitchDetails;
        if (GLITCH_COUNT_ONES.equals(config.getGlitchOption())) {
            int numberOfOnes = DiceUtils.numberOfDiceResultsEqual(rollResult, ImmutableSet.of(1));
            glitchResult = String.format(" successes and %d ones", numberOfOnes);
            toMark.add(1);
            glitchDetails = "";
        } else if (GLITCH_OPTION_HALF_ONES.equals(config.getGlitchOption())) {
            boolean isGlitch = DiceUtils.numberOfDiceResultsEqual(rollResult, ImmutableSet.of(1)) > (rollResult.size() / 2);
            glitchDetails = isGlitch ? " and more then half of all dice show 1s" : "";
            if (isGlitch) {
                toMark.add(1);
            }
            glitchResult = isGlitch ? " - Glitch!" : "";
        } else {
            glitchResult = "";
            glitchDetails = "";
        }

        return Optional.of(RollAnswer.builder()
                .answerFormatType(config.getAnswerFormatType())
                .expression(String.format("%dd%d", numberOfDice, config.getDiceSides()))
                .result(totalResults + glitchResult)
                .rollDetails(String.format("%s ≥%d = %s%s%s",
                        CommandUtils.markIn(rollResult, toMark),
                        config.getTarget(),
                        totalResults,
                        getRerollDescription(config),
                        getBotchDescription(config)
                ) + glitchDetails)
                .build());
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(CountSuccessesConfig config, State<StateData> state) {
        return Optional.of(createNewButtonMessage(config));
    }

    private String getRerollDescription(CountSuccessesConfig config) {
        return config.getRerollSet().isEmpty() ? "" : String.format(", reroll for: %s", config.getRerollSet());
    }

    private String getBotchDescription(CountSuccessesConfig config) {
        return config.getBotchSet().isEmpty() ? "" : String.format(", remove success for: %s", config.getBotchSet());
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(CountSuccessesConfig config) {

        return MessageDefinition.builder()
                .content(String.format("Click to roll the dice against %s%s%s%s", config.getTarget(), getRerollDescription(config), getBotchDescription(config), getGlitchDescription(config)))
                .componentRowDefinitions(createButtonLayout(config))
                .build();
    }

    private String getGlitchDescription(CountSuccessesConfig config) {
        String glitchOption = config.getGlitchOption();
        return switch (glitchOption) {
            case GLITCH_OPTION_HALF_ONES -> " and check for more then half of dice 1s";
            case GLITCH_COUNT_ONES -> " and count the 1s";
            case GLITCH_SUBTRACT_ONES -> " minus 1s";
            default -> "";
        };
    }

    @Override
    protected @NonNull CountSuccessesConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        int sideValue = Math.toIntExact(options.getLongSubOptionWithName(ACTION_SIDE_OPTION)
                .map(l -> Math.min(l, MAX_NUMBER_SIDES_OR_TARGET_NUMBER))
                .orElse(6L));
        int targetValue = Math.toIntExact(options.getLongSubOptionWithName(ACTION_TARGET_OPTION)
                .map(l -> Math.min(l, MAX_NUMBER_SIDES_OR_TARGET_NUMBER))
                .orElse(6L));
        String glitchOption = options.getStringSubOptionWithName(ACTION_GLITCH_OPTION)
                .orElse(GLITCH_NO_OPTION);
        int maxDice = Math.toIntExact(options.getLongSubOptionWithName(ACTION_MAX_DICE_OPTION)
                .map(l -> Math.min(l, MAX_NUMBER_OF_DICE))
                .orElse(15L));
        int minDiceCount = Math.toIntExact(options.getLongSubOptionWithName(ACTION_MIN_DICE_COUNT_OPTION)
                .map(l -> Math.min(l, 100))
                .orElse(1L));
        Set<Integer> rerollSet = CommandUtils.getSetFromCommandOptions(options, ACTION_REROLL_SET_OPTION, ",").stream()
                .sorted()
                .limit(sideValue / 2)
                .collect(Collectors.toSet());
        Set<Integer> botchSet = CommandUtils.getSetFromCommandOptions(options, ACTION_BOTCH_SET_OPTION, ",");
        Long answerTargetChannelId = getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null);
        AnswerFormatType answerType = getAnswerTypeFromStartCommandOption(options);
        return new CountSuccessesConfig(answerTargetChannelId, sideValue, targetValue, glitchOption, maxDice, minDiceCount, rerollSet, botchSet, answerType);
    }

    private List<ComponentRowDefinition> createButtonLayout(CountSuccessesConfig config) {
        List<ButtonDefinition> buttons = IntStream.range(config.getMinDiceCount(), config.getMinDiceCount() + config.getMaxNumberOfButtons())
                .mapToObj(i -> ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), String.valueOf(i)))
                        .label(createButtonLabel(String.valueOf(i), config))
                        .build())
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream().map(bl -> ComponentRowDefinition.builder()
                        .buttonDefinitions(bl)
                        .build())
                .collect(Collectors.toList());
    }


}
