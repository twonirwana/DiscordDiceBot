package de.janno.discord.bot.command.customDice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.DiceParserHelper;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class CustomDiceCommand extends AbstractCommand<CustomDiceConfig, State> {
    //test with /custom_dice start 1_button:1d1 2_button:2d2 3_button:3d3 4_button:4d4 5_button:5d5 6_button:6d6 7_button:7d7 8_button:8d8 9_button:9d9 10_button:10d10 11_button:11d11 12_button:12d12 13_button:13d13 14_button:14d14 15_button:15d15 16_button:16d16 17_button:17d17 18_button:18d18 19_button:19d19 20_button:20d20 21_button:21d21 22_button:22d22 23_button:23d23 24_button:24d24

    private static final String COMMAND_NAME = "custom_dice";
    private static final List<String> DICE_COMMAND_OPTIONS_IDS = IntStream.range(1, 25).mapToObj(i -> i + "_button").toList();
    private static final String LABEL_DELIMITER = "@";
    private static final String BUTTON_MESSAGE = "Click on a button to roll the dice";
    private static final ButtonMessageCache BUTTON_MESSAGE_CACHE = new ButtonMessageCache(COMMAND_NAME);
    private final DiceParserHelper diceParserHelper;

    public CustomDiceCommand() {
        this(new DiceParserHelper());
    }

    @VisibleForTesting
    public CustomDiceCommand(DiceParserHelper diceParserHelper) {
        super(BUTTON_MESSAGE_CACHE);
        this.diceParserHelper = diceParserHelper;
    }

    @Override
    protected Optional<Long> getAnswerTargetChannelId(CustomDiceConfig config) {
        return Optional.ofNullable(config.getAnswerTargetChannelId());
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure a custom set of dice buttons";
    }

    @Override
    protected List<CommandDefinitionOption> getStartOptions() {
        return Stream.concat(DICE_COMMAND_OPTIONS_IDS.stream()
                                .map(id -> CommandDefinitionOption.builder()
                                        .name(id)
                                        .description("xdy for a set of x dice with y sides, e.g. '3d6'")
                                        .type(CommandDefinitionOption.Type.STRING)
                                        .build())
                        , Stream.of(ANSWER_TARGET_CHANNEL_COMMAND_OPTION))
                .collect(Collectors.toList());
    }

    @Override
    protected EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Creates up to 25 buttons with custom dice expression e.g. '/custom_dice start 1_button:3d6 2_button:10d10 3_button:3d20'. \n" + DiceParserHelper.HELP)
                .build();
    }

    @Override
    protected Optional<String> getStartOptionsValidationMessage(CommandInteractionOption options) {
        List<String> diceExpressionWithOptionalLabel = DICE_COMMAND_OPTIONS_IDS.stream()
                .flatMap(id -> options.getStringSubOptionWithName(id).stream())
                .distinct()
                .collect(Collectors.toList());
        String answerTargetChannel = getAnswerTargetChannelIdFromStartCommandOption(options).map(Object::toString).orElse("");
        int maxCharacter = 100 - COMMAND_NAME.length()
                - 2 // delimiter;
                - answerTargetChannel.length();
        return diceParserHelper.validateListOfExpressions(diceExpressionWithOptionalLabel, LABEL_DELIMITER, BotConstants.CONFIG_DELIMITER, "/custom_dice help", maxCharacter);
    }

    protected CustomDiceConfig getConfigFromStartOptions(CommandInteractionOption options) {
        return getConfigOptionStringList(DICE_COMMAND_OPTIONS_IDS.stream()
                .flatMap(id -> options.getStringSubOptionWithName(id).stream())
                .collect(Collectors.toList()), getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null));

    }

    @VisibleForTesting
    CustomDiceConfig getConfigOptionStringList(List<String> startOptions, Long channelId) {
        return new CustomDiceConfig(startOptions.stream()
                .filter(s -> !s.contains(BotConstants.CONFIG_DELIMITER))
                .filter(s -> !s.contains(LABEL_DELIMITER) || s.split(LABEL_DELIMITER).length == 2)
                .map(s -> {
                    if (s.contains(LABEL_DELIMITER)) {
                        String[] split = s.split(LABEL_DELIMITER);
                        return new CustomDiceConfig.LabelAndDiceExpression(split[1].trim(), split[0].trim());
                    }
                    return new CustomDiceConfig.LabelAndDiceExpression(s.trim(), s.trim());
                })
                .filter(s -> !s.getDiceExpression().isEmpty())
                .filter(s -> !s.getLabel().isEmpty())
                .filter(lv -> diceParserHelper.validExpression(lv.getDiceExpression()))
                .filter(s -> createButtonCustomId(s.getDiceExpression(), channelId).length() <= 100) //limit for the ids are 100 characters and we need also some characters for the type...
                .filter(s -> s.getLabel().length() <= 80) //https://discord.com/developers/docs/interactions/message-components#buttons
                .distinct()
                .limit(25)
                .collect(Collectors.toList()), channelId);
    }

    @Override
    protected Optional<EmbedDefinition> getAnswer(State state, CustomDiceConfig config) {
        String label = config.getLabelAndExpression().stream()
                .filter(ld -> !ld.getDiceExpression().equals(ld.getLabel()))
                .filter(ld -> ld.getDiceExpression().equals(state.getButtonValue()))
                .map(CustomDiceConfig.LabelAndDiceExpression::getLabel)
                .findFirst().orElse(null);
        return Optional.of(diceParserHelper.roll(state.getButtonValue(), label));
    }

    @Override
    protected Optional<MessageDefinition> createNewButtonMessageWithState(State state, CustomDiceConfig config) {
        return Optional.of(createNewButtonMessage(config));
    }

    @Override
    public MessageDefinition createNewButtonMessage(CustomDiceConfig config) {
        return MessageDefinition.builder()
                .content(BUTTON_MESSAGE)
                .componentRowDefinitions(createButtonLayout(config))
                .build();
    }

    private List<ComponentRowDefinition> createButtonLayout(CustomDiceConfig config) {
        List<ButtonDefinition> buttons = config.getLabelAndExpression().stream()
                .map(d -> ButtonDefinition.builder()
                        .id(createButtonCustomId(d.getDiceExpression(), config.getAnswerTargetChannelId()))
                        .label(d.getLabel())
                        .build())
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    String createButtonCustomId(String diceExpression, Long answerTargetChannelId) {
        Preconditions.checkArgument(!diceExpression.contains(BotConstants.CONFIG_DELIMITER));

        return String.join(BotConstants.CONFIG_DELIMITER,
                COMMAND_NAME,
                diceExpression,
                Optional.ofNullable(answerTargetChannelId).map(Object::toString).orElse(""));
    }

    @Override
    protected CustomDiceConfig getConfigFromEvent(IButtonEventAdaptor event) {
        String[] split = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);
        Long answerTargetChannelId = getOptionalLongFromArray(split, 2);
        return new CustomDiceConfig(event.getAllButtonIds().stream()
                .map(lv -> new CustomDiceConfig.LabelAndDiceExpression(lv.getLabel(), lv.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX)[1]))
                .collect(Collectors.toList()), answerTargetChannelId);
    }

    @Override
    protected State getStateFromEvent(IButtonEventAdaptor event) {
        return new State(event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX)[1]);
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

}
