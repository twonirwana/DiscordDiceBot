package de.janno.discord.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.janno.discord.cache.ActiveButtonsCache;
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
        super(new ActiveButtonsCache(COMMAND_NAME));
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
                            Button.primary(createButtonCustomId(COMMAND_NAME, "-4", config, state), "-4"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "-3", config, state), "-3"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "-2", config, state), "-2"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "-1", config, state), "-1"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "0", config, state), "0")
                    ),
                    ActionRow.of(

                            Button.primary(createButtonCustomId(COMMAND_NAME, "1", config, state), "+1"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "2", config, state), "+2"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "3", config, state), "+3"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "4", config, state), "+4"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "5", config, state), "+5")
                    ),
                    ActionRow.of(
                            Button.primary(createButtonCustomId(COMMAND_NAME, "6", config, state), "+6"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "7", config, state), "+7"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "8", config, state), "+8"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "9", config, state), "+9"),
                            Button.primary(createButtonCustomId(COMMAND_NAME, "10", config, state), "+10")
                    ));
        } else {
            return ImmutableList.of(
                    ActionRow.of(
                            Button.primary(createButtonCustomId(COMMAND_NAME, "roll", config, state), "Roll 4dF")
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

    @Override
    protected String createButtonCustomId(String system, String value, Config config, State state) {

        Preconditions.checkArgument(!system.contains(CONFIG_DELIMITER));
        Preconditions.checkArgument(!value.contains(CONFIG_DELIMITER));
        Preconditions.checkArgument(!config.getType().contains(CONFIG_DELIMITER));

        return String.join(CONFIG_DELIMITER,
                system,
                value,
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
