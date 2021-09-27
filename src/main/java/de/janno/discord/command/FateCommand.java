package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.ApplicationCommandOptionType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static de.janno.discord.dice.DiceUtils.MINUS;

@Slf4j
public class FateCommand extends AbstractCommand {

    private static final String COMMAND_NAME = "fate";
    private static final String BUTTON_MESSAGE = "Click a button to roll four fate dice";
    private static final String ACTION_MODIFIER_OPTION = "type";
    private static final String ACTION_MODIFIER_OPTION_SIMPLE = "simple";
    private static final String ACTION_MODIFIER_OPTION_MODIFIER = "with_modifier";

    public FateCommand(Snowflake botUserId) {
        super(new ActiveButtonsCache(), botUserId);
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected String getCommandDescription() {
        return "Register the Fate dice system with the channel";
    }

    @Override
    protected String getButtonMessage() {
        return BUTTON_MESSAGE;
    }

    @Override
    protected List<ApplicationCommandOptionData> getStartOptions() {
        return ImmutableList.of(ApplicationCommandOptionData.builder()
                .name(ACTION_MODIFIER_OPTION)
                .required(true)
                .description("Show modifier buttons")
                .type(ApplicationCommandOptionType.STRING.getValue())
                .addChoice(ApplicationCommandOptionChoiceData.builder().name(ACTION_MODIFIER_OPTION_SIMPLE).value(ACTION_MODIFIER_OPTION_SIMPLE).build())
                .addChoice(ApplicationCommandOptionChoiceData.builder().name(ACTION_MODIFIER_OPTION_MODIFIER).value(ACTION_MODIFIER_OPTION_MODIFIER).build())
                .build());
    }

    @Override
    protected List<String> getConfigValuesFromStartOptions(ApplicationCommandInteractionOption options) {
        return ImmutableList.of(options.getOption(ACTION_MODIFIER_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(Object::toString)
                .orElse(ACTION_MODIFIER_OPTION_SIMPLE));
    }

    @Override
    protected boolean matchingButtonCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    protected DiceResult rollDice(Snowflake channelId, String buttonValue, List<String> config) {
        List<Integer> rollResult = DiceUtils.rollFate();
        if (ACTION_MODIFIER_OPTION_MODIFIER.equals(config.get(0))) {
            int modifier = Integer.parseInt(buttonValue);
            String modifierString = "";
            if (modifier > 0) {
                modifierString = "+" + modifier;
            } else if (modifier < 0) {
                modifierString = "" + modifier;
            }
            int resultWithModifier = DiceUtils.fateResult(rollResult) + modifier;
            String title = String.format("4dF %s = %d", modifierString, resultWithModifier);
            String details = DiceUtils.convertFateNumberToString(rollResult);
            log.info(String.format("%s: %s", title, details)
                    .replace("▢", "0")
                    .replace("＋", "+")
                    .replace(MINUS, "-")
            );
            return new DiceResult(title, details);
        } else {
            String title = String.format("4dF = %d", DiceUtils.fateResult(rollResult));
            String details = DiceUtils.convertFateNumberToString(rollResult);
            log.info(String.format("%s: %s", title, details)
                    .replace("▢", "0")
                    .replace("＋", "+")
                    .replace(MINUS, "-")
            );
            return new DiceResult(title, details);
        }
    }

    @Override
    protected List<LayoutComponent> getButtonLayout(List<String> config) {
        if (ACTION_MODIFIER_OPTION_MODIFIER.equals(config.get(0))) {
            return ImmutableList.of(
                    ActionRow.of(
                            //              ID,  label
                            Button.primary(createButtonCustomId(COMMAND_NAME, "-4", config), "-4"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "-3", config), "-3"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "-2", config), "-2"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "-1", config), "-1"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "0", config), "0")
                    ),
                    ActionRow.of(

                            Button.primary(createButtonCustomId(COMMAND_NAME, "1", config), "+1"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "2", config), "+2"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "3", config), "+3"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "4", config), "+4"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "5", config), "+5")
                    ),
                    ActionRow.of(
                            Button.primary(createButtonCustomId(COMMAND_NAME, "6", config), "+6"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "7", config), "+7"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "8", config), "+8"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "9", config), "+9"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "10", config), "+10")
                    ));
        } else {
            return ImmutableList.of(
                    ActionRow.of(
                            Button.primary(createButtonCustomId(COMMAND_NAME, "roll", config), "Roll 4dF")
                    ));
        }
    }
}
