package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ComponentInteractEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static de.janno.discord.dice.DiceUtils.MINUS;

@Slf4j
public class FateCommand extends AbstractCommand {

    private static final String COMMAND_NAME = "fate";
    private static final String BUTTON_MESSAGE = "Click a button to roll four fate dice";

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
    protected List<String> getConfigValuesFromStartOptions(ApplicationCommandInteractionOption options) {
        return ImmutableList.of();
    }

    @Override
    protected List<String> getConfigFromEvent(ComponentInteractEvent event) {
        return ImmutableList.of();
    }

    @Override
    protected String getValueFromEvent(ComponentInteractEvent event) {
        return event.getCustomId().split(CONFIG_DELIMITER)[1];
    }

    @Override
    protected boolean matchingButtonCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    protected DiceResult rollDice(Snowflake channelId, String buttonValue, List<String> config) {
        List<Integer> rollResult = DiceUtils.rollFate();

        String title = String.format("4dF = %d", DiceUtils.fateResult(rollResult));
        String details = DiceUtils.convertFateNumberToString(rollResult);
        log.info(String.format("%s %s", title, details)
                .replace("▢", "0")
                .replace("＋", "+")
                .replace(MINUS, "-")
        );
        return new DiceResult(title, details);
    }

    @Override
    protected List<LayoutComponent> getButtonLayout(List<String> config) {
        return ImmutableList.of(
                ActionRow.of(
                        Button.primary(createButtonCustomId(COMMAND_NAME, "roll", config), "Roll 4dF")
                ));
    }
}
