package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import de.janno.discord.persistance.IPersistable;
import de.janno.discord.persistance.SerializedChannelConfig;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class FateCommand extends AbstractCommand<Void> implements IPersistable {

    private static final String COMMAND_NAME = "fate";
    private static final String BUTTON_MESSAGE = "Click a button to roll four fate dice";

    public FateCommand(Snowflake botUserId) {
        super(new ConfigRegistry<>(COMMAND_NAME, null), botUserId);
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
    protected String getCommandDescription() {
        return "Register the Fate dice system with the channel";
    }

    @Override
    protected String getButtonMessage() {
        return BUTTON_MESSAGE;
    }

    @Override
    protected Void createConfig() {
        return null;
    }

    @Override
    protected Void setConfigValuesFromStartOptions(ApplicationCommandInteractionOption options, Void config) {
        return null;
    }

    @Override
    protected DiceResult rollDice(Snowflake channelId, String buttonId) {
        List<Integer> rollResult = DiceUtils.rollFate();

        String title = String.format("4dF = %d", DiceUtils.fateResult(rollResult));
        String details = DiceUtils.convertFateNumberToString(rollResult);
        log.info(String.format("%s %s", title, details)
                .replace("▢", "0")
                .replace("＋", "+")
                .replace("─", "-")
        );
        return new DiceResult(title, details);
    }

    @Override
    protected List<LayoutComponent> getButtonLayout() {
        return ImmutableList.of(
                ActionRow.of(
                        Button.primary("roll", "Roll 4dF")
                ));
    }
}
