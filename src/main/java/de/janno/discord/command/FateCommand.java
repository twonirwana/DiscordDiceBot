package de.janno.discord.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.janno.discord.cache.ButtonMessageCache;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class FateCommand extends AbstractCommand<FateCommand.Config, FateCommand.State> {

    private static final String COMMAND_NAME = "fate";
    private static final String ACTION_MODIFIER_OPTION = "type";
    private static final String ACTION_MODIFIER_OPTION_SIMPLE = "simple";
    private static final String ACTION_MODIFIER_OPTION_MODIFIER = "with_modifier";
    private final DiceUtils diceUtils;

    @VisibleForTesting
    public FateCommand(DiceUtils diceUtils) {
        super(new ButtonMessageCache(COMMAND_NAME));
        this.diceUtils = diceUtils;
    }

    public FateCommand() {
        this(new DiceUtils());
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected String getCommandDescription() {
        return "Configure Fate dice";
    }

    @Override
    protected String getButtonMessage(State state, Config config) {
        if (ACTION_MODIFIER_OPTION_MODIFIER.equals(config.getType())) {
            return "Click a button to roll four fate dice and add the value of the button";
        }
        return "Click a button to roll four fate dice";
    }

    @Override
    protected EmbedCreateSpec getHelpMessage() {
        return EmbedCreateSpec.builder()
                .description("Buttons for Fate/Fudge dice. There are two types, the simple produces one button that rolls four dice and " +
                        "provides the result together with the sum. The type with_modifier produces multiple buttons for modifier -4 to +10" +
                        " that roll four dice and add the modifier of the button.")
                .addField("Example", "'/fate start type:with_modifier' or '/fate start type:simple'", false)
                .build();
    }

    @Override
    protected List<ApplicationCommandOptionData> getStartOptions() {
        return ImmutableList.of(ApplicationCommandOptionData.builder()
                .name(ACTION_MODIFIER_OPTION)
                .required(true)
                .description("Show modifier buttons")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                        .name(ACTION_MODIFIER_OPTION_SIMPLE)
                        .value(ACTION_MODIFIER_OPTION_SIMPLE)
                        .build())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                        .name(ACTION_MODIFIER_OPTION_MODIFIER)
                        .value(ACTION_MODIFIER_OPTION_MODIFIER)
                        .build())
                .build());
    }

    @Override
    protected Config getConfigValuesFromStartOptions(ApplicationCommandInteractionOption options) {
        return new Config(options.getOption(ACTION_MODIFIER_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(Object::toString)
                .orElse(ACTION_MODIFIER_OPTION_SIMPLE));
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    protected List<DiceResult> getDiceResult(State state, Config config) {
        List<Integer> rollResult = diceUtils.rollFate();

        if (ACTION_MODIFIER_OPTION_MODIFIER.equals(config.getType())) {
            int modifier = state.getModifier();
            String modifierString = "";
            if (modifier > 0) {
                modifierString = " +" + modifier;
            } else if (modifier < 0) {
                modifierString = " " + modifier;
            }
            int resultWithModifier = DiceUtils.fateResult(rollResult) + modifier;

            String title = String.format("4dF%s = %d", modifierString, resultWithModifier);
            String details = DiceUtils.convertFateNumberToString(rollResult);
            return ImmutableList.of(new DiceResult(title, details));
        } else {
            String title = String.format("4dF = %d", DiceUtils.fateResult(rollResult));
            String details = DiceUtils.convertFateNumberToString(rollResult);
            return ImmutableList.of(new DiceResult(title, details));
        }
    }

    @Override
    protected List<LayoutComponent> getButtonLayout(State state, Config config) {
        if (ACTION_MODIFIER_OPTION_MODIFIER.equals(config.getType())) {
            return ImmutableList.of(
                    ActionRow.of(
                            //              ID,  label
                            Button.primary(createButtonCustomId("-4", config), "-4"),
                            Button.primary(createButtonCustomId("-3", config), "-3"),
                            Button.primary(createButtonCustomId("-2", config), "-2"),
                            Button.primary(createButtonCustomId("-1", config), "-1"),
                            Button.primary(createButtonCustomId("0", config), "0")
                    ),
                    ActionRow.of(

                            Button.primary(createButtonCustomId("1", config), "+1"),
                            Button.primary(createButtonCustomId("2", config), "+2"),
                            Button.primary(createButtonCustomId("3", config), "+3"),
                            Button.primary(createButtonCustomId("4", config), "+4"),
                            Button.primary(createButtonCustomId("5", config), "+5")
                    ),
                    ActionRow.of(
                            Button.primary(createButtonCustomId("6", config), "+6"),
                            Button.primary(createButtonCustomId("7", config), "+7"),
                            Button.primary(createButtonCustomId("8", config), "+8"),
                            Button.primary(createButtonCustomId("9", config), "+9"),
                            Button.primary(createButtonCustomId("10", config), "+10")
                    ));
        } else {
            return ImmutableList.of(
                    ActionRow.of(
                            Button.primary(createButtonCustomId("roll", config), "Roll 4dF")
                    ));
        }
    }

    @Override
    protected Config getConfigFromEvent(IButtonEventAdaptor event) {
        String[] split = event.getCustomId().split(CONFIG_DELIMITER);
        return new Config(split[2]);
    }

    @Override
    protected State getStateFromEvent(IButtonEventAdaptor event) {
        String modifier = event.getCustomId().split(CONFIG_DELIMITER)[1];
        if (modifier.isBlank()) {
            return new State(null);
        }
        return new State(Integer.valueOf(modifier));
    }

    @VisibleForTesting
    String createButtonCustomId(String modifier, Config config) {
        Preconditions.checkArgument(!modifier.contains(CONFIG_DELIMITER));
        Preconditions.checkArgument(!config.getType().contains(CONFIG_DELIMITER));

        return String.join(CONFIG_DELIMITER,
                COMMAND_NAME,
                modifier,
                config.getType());
    }


    @Value
    protected static class Config implements IConfig {

        @NonNull
        String type;

        @Override
        public String toMetricString() {
            return type;
        }
    }

    @Value
    static class State implements IState {
        Integer modifier;
    }
}
