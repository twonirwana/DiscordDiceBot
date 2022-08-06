package de.janno.discord.bot.command.countSuccesses;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.CommandUtils;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.connector.api.BotConstants;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
public class CountSuccessesCommand extends AbstractCommand<CountSuccessesConfig, CountSuccessesState> {

    private static final String COMMAND_NAME = "count_successes";
    private static final String ACTION_SIDE_OPTION = "dice_sides";
    private static final String ACTION_TARGET_OPTION = "target_number";
    private static final String ACTION_MAX_DICE_OPTION = "max_dice";
    private static final long MAX_NUMBER_OF_DICE = 25;
    private static final String ACTION_GLITCH_OPTION = "glitch";
    private static final long MAX_NUMBER_SIDES_OR_TARGET_NUMBER = 1000;
    private static final String GLITCH_OPTION_HALF_ONES = "half_dice_one";
    private static final String GLITCH_NO_OPTION = "no_glitch";
    private static final String GLITCH_COUNT_ONES = "count_ones";
    private static final String GLITCH_SUBTRACT_ONES = "subtract_ones";
    private static final ButtonMessageCache BUTTON_MESSAGE_CACHE = new ButtonMessageCache(COMMAND_NAME);
    private final DiceUtils diceUtils;

    public CountSuccessesCommand() {
        this(new DiceUtils());
    }

    @VisibleForTesting
    public CountSuccessesCommand(DiceUtils diceUtils) {
        super(BUTTON_MESSAGE_CACHE);
        this.diceUtils = diceUtils;
    }

    private static String createButtonLabel(String value, CountSuccessesConfig config) {
        return String.format("%sd%s", value, config.getDiceSides());
    }


    @Override
    protected CountSuccessesConfig getConfigFromEvent(IButtonEventAdaptor event) {
        String[] split = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);
        int sideOfDie = Integer.parseInt(split[2]);
        int target = Integer.parseInt(split[3]);
        //legacy message could be missing the glitch and max dice option
        String glitchOption = split.length < 5 ? GLITCH_NO_OPTION : split[4];
        int maxNumberOfButtons = split.length < 6 ? 15 : Integer.parseInt(split[5]);
        Long answerTargetChannelId = getOptionalLongFromArray(split, 6);
        return new CountSuccessesConfig(sideOfDie, target, glitchOption, maxNumberOfButtons, answerTargetChannelId);
    }

    @Override
    protected Optional<Long> getAnswerTargetChannelId(CountSuccessesConfig config) {
        return Optional.ofNullable(config.getAnswerTargetChannelId());
    }

    @Override
    protected CountSuccessesState getStateFromEvent(IButtonEventAdaptor event) {
        return new CountSuccessesState(event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX)[1]);
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure buttons for dice, with the same side, that counts successes against a target number";
    }

    @Override
    protected EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder().description("Use '/count_successes start dice_sides:X target_number:Y' " + "to get Buttons that roll with X sided dice against the target of Y and count the successes." + " A successes are all dice that have a result greater or equal then the target number")
                .field(new EmbedDefinition.Field("Example", "/count_successes start dice_sides:10 target_number:7", false)).build();
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected List<CommandDefinitionOption> getStartOptions() {
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
                        .choice(CommandDefinitionOptionChoice.builder().name(GLITCH_SUBTRACT_ONES).value(GLITCH_SUBTRACT_ONES).build())
                        .build(),
                CommandDefinitionOption.builder()
                        .name(ACTION_MAX_DICE_OPTION)
                        .required(false)
                        .description("Max number of dice")
                        .type(CommandDefinitionOption.Type.INTEGER)
                        .minValue(1L)
                        .maxValue(MAX_NUMBER_OF_DICE)
                        .build(),
                ANSWER_TARGET_CHANNEL_COMMAND_OPTION);
    }

    @Override
    protected Optional<EmbedDefinition> getAnswer(CountSuccessesState state, CountSuccessesConfig config) {
        List<Integer> rollResult = diceUtils.rollDiceOfType(state.getNumberOfDice(), config.getDiceSides()).stream()
                .sorted()
                .collect(Collectors.toList());

        return switch (config.getGlitchOption()) {
            case GLITCH_OPTION_HALF_ONES ->
                    halfOnesGlitch(state.getNumberOfDice(), config.getDiceSides(), config.getTarget(), rollResult);
            case GLITCH_COUNT_ONES ->
                    countOnesGlitch(state.getNumberOfDice(), config.getDiceSides(), config.getTarget(), rollResult);
            case GLITCH_SUBTRACT_ONES ->
                    subtractOnesGlitch(state.getNumberOfDice(), config.getDiceSides(), config.getTarget(), rollResult);
            default -> noneGlitch(state.getNumberOfDice(), config.getDiceSides(), config.getTarget(), rollResult);
        };
    }

    private Optional<EmbedDefinition> noneGlitch(int numberOfDice, int sidesOfDie, int targetNumber, List<Integer> rollResult) {
        int numberOfSuccesses = DiceUtils.numberOfDiceResultsGreaterEqual(rollResult, targetNumber);
        Set<Integer> toMark = IntStream.range(targetNumber, sidesOfDie + 1).boxed().collect(Collectors.toSet());
        String details = String.format("%s ≥%d = %s", CommandUtils.markIn(rollResult, toMark), targetNumber, numberOfSuccesses);
        String title = String.format("%dd%d = %d", numberOfDice, sidesOfDie, numberOfSuccesses);
        return Optional.of(new EmbedDefinition(title, details, ImmutableList.of()));
    }

    private Optional<EmbedDefinition> countOnesGlitch(int numberOfDice, int sidesOfDie, int targetNumber, List<Integer> rollResult) {
        int numberOfSuccesses = DiceUtils.numberOfDiceResultsGreaterEqual(rollResult, targetNumber);
        int numberOfOnes = DiceUtils.numberOfDiceResultsEqual(rollResult, ImmutableSet.of(1));
        Set<Integer> toMark = IntStream.range(targetNumber, sidesOfDie + 1).boxed().collect(Collectors.toSet());
        toMark.add(1);
        String details = String.format("%s ≥%d = %s", CommandUtils.markIn(rollResult, toMark), targetNumber, numberOfSuccesses);
        String title = String.format("%dd%d = %d successes and %d ones", numberOfDice, sidesOfDie, numberOfSuccesses, numberOfOnes);
        return Optional.of(new EmbedDefinition(title, details, ImmutableList.of()));
    }

    private Optional<EmbedDefinition> subtractOnesGlitch(int numberOfDice, int sidesOfDie, int targetNumber, List<Integer> rollResult) {
        int numberOfSuccesses = DiceUtils.numberOfDiceResultsGreaterEqual(rollResult, targetNumber);
        int numberOfOnes = DiceUtils.numberOfDiceResultsEqual(rollResult, ImmutableSet.of(1));
        Set<Integer> toMark = IntStream.range(targetNumber, sidesOfDie + 1).boxed().collect(Collectors.toSet());
        toMark.add(1);
        String details = String.format("%s ≥%d -1s = %s", CommandUtils.markIn(rollResult, toMark), targetNumber, numberOfSuccesses - numberOfOnes);
        String title = String.format("%dd%d = %d", numberOfDice, sidesOfDie, numberOfSuccesses - numberOfOnes);
        return Optional.of(new EmbedDefinition(title, details, ImmutableList.of()));
    }

    private Optional<EmbedDefinition> halfOnesGlitch(int numberOfDice, int sidesOfDie, int targetNumber, List<Integer> rollResult) {
        boolean isGlitch = DiceUtils.numberOfDiceResultsEqual(rollResult, ImmutableSet.of(1)) > (numberOfDice / 2);
        int numberOfSuccesses = DiceUtils.numberOfDiceResultsGreaterEqual(rollResult, targetNumber);
        String glitchDescription = isGlitch ? " and more then half of all dice show 1s" : "";
        Set<Integer> toMark = IntStream.range(targetNumber, sidesOfDie + 1).boxed().collect(Collectors.toSet());
        if (isGlitch) {
            toMark.add(1);
        }
        String details = String.format("%s ≥%d = %s%s", CommandUtils.markIn(rollResult, toMark), targetNumber, numberOfSuccesses, glitchDescription);
        String glitch = isGlitch ? " - Glitch!" : "";
        String title = String.format("%dd%d = %d%s", numberOfDice, sidesOfDie, numberOfSuccesses, glitch);
        return Optional.of(new EmbedDefinition(title, details, ImmutableList.of()));
    }

    @Override
    protected Optional<MessageDefinition> createNewButtonMessageWithState(CountSuccessesState state, CountSuccessesConfig config) {
        return Optional.of(createNewButtonMessage(config));
    }

    @Override
    public MessageDefinition createNewButtonMessage(CountSuccessesConfig config) {
        return MessageDefinition.builder()
                .content(String.format("Click to roll the dice against %s%s", config.getTarget(), getGlitchDescription(config)))
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
    protected CountSuccessesConfig getConfigFromStartOptions(CommandInteractionOption options) {
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
        Long answerTargetChannelId = getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null);
        return new CountSuccessesConfig(sideValue, targetValue, glitchOption, maxDice, answerTargetChannelId);
    }

    private List<ComponentRowDefinition> createButtonLayout(CountSuccessesConfig config) {
        List<ButtonDefinition> buttons = IntStream.range(1, config.getMaxNumberOfButtons() + 1)
                .mapToObj(i -> ButtonDefinition.builder()
                        .id(createButtonCustomId(String.valueOf(i), config))
                        .label(createButtonLabel(String.valueOf(i), config))
                        .build())
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream().map(bl -> ComponentRowDefinition.builder()
                        .buttonDefinitions(bl)
                        .build())
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    String createButtonCustomId(String number, CountSuccessesConfig config) {
        Preconditions.checkArgument(!config.getGlitchOption().contains(BotConstants.CONFIG_DELIMITER));

        return String.join(BotConstants.CONFIG_DELIMITER,
                COMMAND_NAME,
                number,
                String.valueOf(config.getDiceSides()),
                String.valueOf(config.getTarget()),
                config.getGlitchOption(),
                String.valueOf(config.getMaxNumberOfButtons()),
                Optional.ofNullable(config.getAnswerTargetChannelId()).map(Object::toString).orElse("")
        );
    }

}
