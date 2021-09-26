package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import de.janno.discord.persistance.IPersistable;
import de.janno.discord.persistance.SerializedChannelConfig;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.ApplicationCommandOptionType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class CountSuccessesCommand extends AbstractCommand<DiceSideTargetNumberConfig> implements IPersistable {

    private static final String COMMAND_NAME = "count_successes";
    private static final String BUTTON_MESSAGE = "Click a button to roll the number of dice";
    private static final String ACTION_SIDE_OPTION = "dice_sides";
    private static final String ACTION_TARGET_OPTION = "target_number";
    private static final int MAX_NUMBER_OF_DICE = 100;

    public CountSuccessesCommand(Snowflake botUserId) {
        super(new ConfigRegistry<>(COMMAND_NAME, DiceSideTargetNumberConfig.class), botUserId);
    }

    @Override
    protected String getCommandDescription() {
        return "Register the x sided Dice with the target number y system in the channel.";
    }

    @Override
    public List<SerializedChannelConfig> getChannelConfig() {
        return configRegistry.getAllChannelConfig();
    }

    @Override
    public void setChannelConfig(List<SerializedChannelConfig> channelConfigs) {
        configRegistry.setAllChannelConfig(channelConfigs);
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected List<ApplicationCommandOptionData> getStartOptions() {
        return ImmutableList.of(ApplicationCommandOptionData.builder()
                        .name(ACTION_SIDE_OPTION)
                        .required(true)
                        .description("Dice side")
                        .type(ApplicationCommandOptionType.INTEGER.getValue())
                        .build(),
                ApplicationCommandOptionData.builder()
                        .name(ACTION_TARGET_OPTION)
                        .required(true)
                        .description("Target number")
                        .type(ApplicationCommandOptionType.INTEGER.getValue())
                        .build());
    }

    @Override
    protected DiceResult rollDice(Snowflake channelId, String buttonId) {
        DiceSideTargetNumberConfig diceConfig = configRegistry.getConfigForChannelOrDefault(channelId, new DiceSideTargetNumberConfig(6, 6));
        int numberOfDice = Math.min(Integer.parseInt(buttonId), MAX_NUMBER_OF_DICE);
        List<Integer> rollResult = DiceUtils.rollDiceOfType(numberOfDice, diceConfig.getDiceSide());
        int numberOf6s = DiceUtils.numberOfDiceResultsGreaterEqual(rollResult, diceConfig.targetNumber);
        String details = "Target: " + diceConfig.getTargetNumber() + " = " + DiceUtils.makeGreaterEqualTargetValuesBold(rollResult, diceConfig.targetNumber);
        String title = String.format("%dd%d = %d", numberOfDice, diceConfig.getDiceSide(), numberOf6s);
        log.info(String.format("%s %s", title, details));
        return new DiceResult(title, details);
    }


    @Override
    protected String getButtonMessage() {
        return BUTTON_MESSAGE;
    }

    @Override
    protected DiceSideTargetNumberConfig createConfig() {
        return new DiceSideTargetNumberConfig();
    }

    @Override
    protected DiceSideTargetNumberConfig setConfigValuesFromStartOptions(ApplicationCommandInteractionOption options, DiceSideTargetNumberConfig config) {
        Integer sideValue = options.getOption(ACTION_SIDE_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(Long::intValue)
                .orElse(null);
        Integer targetValue = options.getOption(ACTION_TARGET_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(Long::intValue)
                .orElse(null);
        config.setDiceSide(sideValue);
        config.setTargetNumber(targetValue);
        return config;
    }
}
