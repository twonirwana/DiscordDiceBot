package de.janno.discord.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.dice.DiceParserHelper;
import de.janno.discord.dice.DiceResult;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
//TODO give buttons names
public class CustomDiceCommand extends AbstractCommand {
    //test with /custom_dice start 1_button:1d1 2_button:2d2 3_button:3d3 4_button:4d4 5_button:5d5 6_button:6d6 7_button:7d7 8_button:8d8 9_button:9d9 10_button:10d10 11_button:11d11 12_button:12d12 13_button:13d13 14_button:14d14 15_button:15d15 16_button:16d16 17_button:17d17 18_button:18d18 19_button:19d19 20_button:20d20 21_button:21d21 22_button:22d22 23_button:23d23 24_button:24d24 25_button:25d25

    private static final String COMMAND_NAME = "custom_dice";
    private static final List<String> DICE_COMMAND_OPTIONS_IDS = IntStream.range(1, 26).mapToObj(i -> i + "_button").collect(Collectors.toList());
    private final DiceParserHelper diceParserHelper;

    public CustomDiceCommand() {
        this(new DiceParserHelper());
    }

    @VisibleForTesting
    public CustomDiceCommand(DiceParserHelper diceParserHelper) {
        super(new ActiveButtonsCache(COMMAND_NAME));
        this.diceParserHelper = diceParserHelper;
    }

    @Override
    protected String getCommandDescription() {
        return "Configure a custom set of dice buttons";
    }

    @Override
    protected String getButtonMessage(String buttonValue, List<String> config) {
        return "Click on a button to roll the dice";
    }

    @Override
    protected List<ApplicationCommandOptionData> getStartOptions() {
        return DICE_COMMAND_OPTIONS_IDS.stream()
                .map(id -> ApplicationCommandOptionData.builder()
                        .name(id)
                        .description("xdy for a set of x dice with y sides, e.g. '3d6'")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    protected EmbedCreateSpec getHelpMessage() {
        return EmbedCreateSpec.builder()
                .description("Creates up to 25 buttons with custom dice expression e.g. '/custom_dice start 1_button:3d6 2_button:10d10 3_button:3d20'. \n" +
                        "```\n" +
                        "      Name     |   Syntax  |  Example  \n" +
                        "---------------------------------------\n" +
                        "Single Die     |'d'        |'d6'       \n" +
                        "---------------------------------------\n" +
                        "Multiple Dice  |'d'        |'3d20'     \n" +
                        "---------------------------------------\n" +
                        "Keep Dice      |'dk'       |'3d6k2'    \n" +
                        "---------------------------------------\n" +
                        "Keep Low Dice  |'dl'       |'3d6l2'    \n" +
                        "---------------------------------------\n" +
                        "Multiply Die   |'dX'       |'d10X'     \n" +
                        " --------------------------------------\n" +
                        "Multiply Dice  |'dX'       |'2d10X'    \n" +
                        "---------------------------------------\n" +
                        "Fudge Dice     |'dF'       |'dF'       \n" +
                        "---------------------------------------\n" +
                        "Multiple Fudge |'dF'       |'3dF'      \n" +
                        " Dice          |           |           \n" +
                        " --------------------------------------\n" +
                        "Weighted Fudge |'dF.'      |'dF.1'     \n" +
                        " Die           |           |           \n" +
                        " --------------------------------------\n" +
                        "Weighted       |'dF.'      |'2dF.1'    \n" +
                        " Fudge Dice    |           |           \n" +
                        "---------------------------------------\n" +
                        "Exploding Dice |'d!'       |'4d6!'     \n" +
                        "---------------------------------------\n" +
                        "Exploding Dice |'d!>'      |'3d6!>5'   \n" +
                        " (Target)      |           |           \n" +
                        "---------------------------------------\n" +
                        "Compounding    |'d!!'      |'3d6!!'    \n" +
                        " Dice          |           |           \n" +
                        "---------------------------------------\n" +
                        "Compounding    |'d!!>'     |'3d6!!>5'  \n" +
                        " Dice (Target) |           |           \n" +
                        "---------------------------------------\n" +
                        "Target Pool    |'d[>,<,=]' |'3d6=6'    \n" +
                        " Dice          |           |           \n" +
                        "---------------------------------------\n" +
                        "Target Pool    |'()[>,<,=]'|'(4d8-2)>6'\n" +
                        "Dice Expression|           |           \n" +
                        "---------------------------------------\n" +
                        "Multiple Rolls |'x[]'      |`3x[3d6]`  \n" +
                        "---------------------------------------\n" +
                        "Integer        |''         |'42'       \n" +
                        "---------------------------------------\n" +
                        "Add            |' + '      |'2d6 + 2'  \n" +
                        "---------------------------------------\n" +
                        "Subtract       |' - '      |'2 - 1'    \n" +
                        "---------------------------------------\n" +
                        "Multiply       |' * '      |'1d4*2d6'  \n" +
                        "---------------------------------------\n" +
                        "Divide         |' / '      |'4 / 2'    \n" +
                        "```\n" +
                        "see https://github.com/twonirwana/DiscordDiceBot/blob/main/README.md for more details"
                )

                .build();
    }

    @Override
    protected String getStartOptionsValidationMessage(ApplicationCommandInteractionOption options) {

        List<String> allDiceExpressions = DICE_COMMAND_OPTIONS_IDS.stream()
                .flatMap(id -> options.getOption(id).stream())
                .flatMap(a -> a.getValue().stream())
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(Object::toString)
                .distinct()
                .collect(Collectors.toList());

        return diceParserHelper.validateDiceExpressions(allDiceExpressions, "/custom_dice help");
    }

    @Override
    protected List<String> getConfigValuesFromStartOptions(ApplicationCommandInteractionOption options) {
        return DICE_COMMAND_OPTIONS_IDS.stream()
                .flatMap(id -> options.getOption(id).stream())
                .flatMap(a -> a.getValue().stream())
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(Object::toString)
                .filter(diceParserHelper::validExpression)
                .filter(s -> s.length() <= 80) //limit for the ids are 100 characters and we need also some characters for the type...
                .distinct()
                .limit(25)
                .collect(Collectors.toList());
    }

    @Override
    protected List<DiceResult> getDiceResult(String buttonValue, List<String> config) {
        return diceParserHelper.roll(buttonValue);
    }

    @Override
    protected List<LayoutComponent> getButtonLayout(String buttonValue, List<String> config) {
        List<Button> buttons = config.stream()
                .map(d -> Button.primary(createButtonCustomId(COMMAND_NAME, d, ImmutableList.of()), d))
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream()
                .map(ActionRow::of)
                .collect(Collectors.toList());
    }

    @Override
    protected List<String> getConfigFromEvent(ComponentInteractionEvent event) {
        return event.getInteraction().getMessage()
                .map(s -> s.getComponents().stream()
                        .flatMap(lc -> lc.getChildren().stream())
                        .map(l -> l.getData().customId())
                        .map(c -> c.toOptional().orElse(null))
                        .filter(Objects::nonNull)
                        .map(id -> id.substring(COMMAND_NAME.length() + 1))
                        .collect(Collectors.toList())
                )
                .orElse(ImmutableList.of());
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }
}
