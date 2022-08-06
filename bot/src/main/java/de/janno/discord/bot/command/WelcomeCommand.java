package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesCommand;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesConfig;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.fate.FateCommand;
import de.janno.discord.bot.command.fate.FateConfig;
import de.janno.discord.bot.command.poolTarget.PoolTargetCommand;
import de.janno.discord.bot.command.poolTarget.PoolTargetConfig;
import de.janno.discord.connector.api.BotConstants;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class WelcomeCommand extends AbstractCommand<Config, State> {

    private static final String COMMAND_NAME = "welcome";
    private static final String FATE_BUTTON_ID = "fate";
    private static final String DND5_BUTTON_ID = "dnd5";
    private static final String NWOD_BUTTON_ID = "nWoD";
    private static final String OWOD_BUTTON_ID = "oWoD";
    private static final String SHADOWRUN_BUTTON_ID = "shadowrun";
    private static final String COIN_BUTTON_ID = "coin";

    public WelcomeCommand() {
        super(new ButtonMessageCache(COMMAND_NAME));
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Displays the welcome message";
    }

    @Override
    protected EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder().description("Displays the welcome message").build();
    }

    @Override
    protected Optional<MessageDefinition> createNewButtonMessageWithState(State state, Config config) {
        BotMetrics.incrementButtonMetricCounter(COMMAND_NAME, state.toShortString());
        return switch (state.getButtonValue()) {
            case FATE_BUTTON_ID -> Optional.of(
                    new FateCommand().createNewButtonMessage(new FateConfig(null, "with_modifier"))
            );
            case DND5_BUTTON_ID -> Optional.of(
                    new CustomDiceCommand().createNewButtonMessage(new CustomDiceConfig(null, ImmutableList.of(
                            new CustomDiceConfig.LabelAndDiceExpression("D4", "1d4"),
                            new CustomDiceConfig.LabelAndDiceExpression("D6", "1d6"),
                            new CustomDiceConfig.LabelAndDiceExpression("D8", "1d8"),
                            new CustomDiceConfig.LabelAndDiceExpression("D10", "1d10"),
                            new CustomDiceConfig.LabelAndDiceExpression("D12", "1d12"),
                            new CustomDiceConfig.LabelAndDiceExpression("D20", "1d20"),
                            new CustomDiceConfig.LabelAndDiceExpression("D100", "1d100"),
                            new CustomDiceConfig.LabelAndDiceExpression("D20 Advantage", "2d20k1"),
                            new CustomDiceConfig.LabelAndDiceExpression("D20 Disadvantage", "2d20L1"),
                            new CustomDiceConfig.LabelAndDiceExpression("2D4", "2d4"),
                            new CustomDiceConfig.LabelAndDiceExpression("2D6", "2d6"),
                            new CustomDiceConfig.LabelAndDiceExpression("2D8", "2d8"),
                            new CustomDiceConfig.LabelAndDiceExpression("2D10", "2d10"),
                            new CustomDiceConfig.LabelAndDiceExpression("2D12", "2d12"),
                            new CustomDiceConfig.LabelAndDiceExpression("2D20", "2d20")
                    )))
            );
            case NWOD_BUTTON_ID -> Optional.of(
                    new CountSuccessesCommand().createNewButtonMessage(new CountSuccessesConfig(null, 10, 8, "no_glitch", 15))
            );
            case OWOD_BUTTON_ID -> Optional.of(
                    new PoolTargetCommand().createNewButtonMessage(new PoolTargetConfig(null, 10, 15, ImmutableSet.of(10), ImmutableSet.of(1), "ask"))
            );
            case SHADOWRUN_BUTTON_ID -> Optional.of(
                    new CountSuccessesCommand().createNewButtonMessage(new CountSuccessesConfig(null, 6, 5, "glitch:half_dice_one", 20))
            );
            case COIN_BUTTON_ID -> Optional.of(
                    new CustomDiceCommand().createNewButtonMessage(new CustomDiceConfig(null, ImmutableList.of(
                            new CustomDiceConfig.LabelAndDiceExpression("Coin Toss \uD83E\uDE99", "1d2=2?Head \uD83D\uDE00:Tail \uD83E\uDD85"))))
            );
            default -> Optional.empty();
        };
    }

    @Override
    protected boolean shouldKeepExistingButtonMessage(IButtonEventAdaptor event) {
        return true;
    }

    @Override
    protected Optional<EmbedDefinition> getAnswer(State state, Config config) {
        return Optional.empty();
    }


    public MessageDefinition getWelcomeMessage() {
        return createNewButtonMessage(null);
    }

    @Override
    public MessageDefinition createNewButtonMessage(Config config) {
        return MessageDefinition.builder()
                .content("""
                        Welcome to the Button Dice Bot,
                        use one of the example buttons below to start one of the RPG dice systems or use the slash command to configure your own custom dice system (see https://github.com/twonirwana/DiscordDiceBot for details or the slash command `/help`).\s
                        You can also use the slash command `/r` to directly roll dice with.
                        For help or feature request come to the support discord server: https://discord.gg/e43BsqKpFr""")
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .buttonDefinitions(
                                ImmutableList.of(
                                        ButtonDefinition.builder()
                                                .id(String.join(BotConstants.CONFIG_DELIMITER, COMMAND_NAME, FATE_BUTTON_ID))
                                                .label("Fate")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(String.join(BotConstants.CONFIG_DELIMITER, COMMAND_NAME, DND5_BUTTON_ID))
                                                .label("D&D5e")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(String.join(BotConstants.CONFIG_DELIMITER, COMMAND_NAME, NWOD_BUTTON_ID))
                                                .label("nWoD")
                                                .build()
                                )
                        )
                        .build())
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .buttonDefinitions(
                                ImmutableList.of(
                                        ButtonDefinition.builder()
                                                .id(String.join(BotConstants.CONFIG_DELIMITER, COMMAND_NAME, OWOD_BUTTON_ID))
                                                .label("oWoD")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(String.join(BotConstants.CONFIG_DELIMITER, COMMAND_NAME, SHADOWRUN_BUTTON_ID))
                                                .label("Shadowrun")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(String.join(BotConstants.CONFIG_DELIMITER, COMMAND_NAME, COIN_BUTTON_ID))
                                                .label("Coin Toss \uD83E\uDE99")
                                                .build()
                                )
                        )
                        .build())
                .build();
    }

    @Override
    protected Config getConfigFromStartOptions(CommandInteractionOption options) {
        return new Config(null);
    }

    @Override
    protected Config getConfigFromEvent(IButtonEventAdaptor event) {
        return new Config(null);
    }

    @Override
    protected State getStateFromEvent(IButtonEventAdaptor event) {
        String buttonId = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX)[1];
        return new State(buttonId);
    }
}
