package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesCommand;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesConfig;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.fate.FateCommand;
import de.janno.discord.bot.command.fate.FateConfig;
import de.janno.discord.bot.command.poolTarget.PoolTargetCommand;
import de.janno.discord.bot.command.poolTarget.PoolTargetConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class WelcomeCommand extends AbstractCommand<Config, StateData> {

    private static final String COMMAND_NAME = "welcome";
    private static final String FATE_BUTTON_ID = "fate";
    private static final String DND5_BUTTON_ID = "dnd5";
    private static final String NWOD_BUTTON_ID = "nWoD";
    private static final String OWOD_BUTTON_ID = "oWoD";
    private static final String SHADOWRUN_BUTTON_ID = "shadowrun";
    private static final String COIN_BUTTON_ID = "coin";
    private static final String DICE_CALCULATOR_ID = "dice_calculator";
    private final static FateConfig FATE_CONFIG = new FateConfig(null, "with_modifier", AnswerFormatType.full);
    private final static CountSuccessesConfig NWOD_CONFIG = new CountSuccessesConfig(null, 10, 8, "no_glitch", 15, 1, Set.of(10), Set.of(), AnswerFormatType.full);
    private final static CountSuccessesConfig SHADOWRUN_CONFIG = new CountSuccessesConfig(null, 6, 5, "half_dice_one", 20, 1, Set.of(), Set.of(), AnswerFormatType.full);
    private final static PoolTargetConfig OWOD_CONFIG = new PoolTargetConfig(null, 10, 15, ImmutableSet.of(10), ImmutableSet.of(1), "ask", AnswerFormatType.full);
    private final static CustomDiceConfig COIN_CONFIG = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Coin Toss \uD83E\uDE99", "1d[Head \uD83D\uDE00/Tail \uD83E\uDD85]")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression);
    private final static CustomDiceConfig DND5_CONFIG = new CustomDiceConfig(null, ImmutableList.of(
            new ButtonIdLabelAndDiceExpression("1_button", "D4", "1d4"),
            new ButtonIdLabelAndDiceExpression("2_button", "D6", "1d6"),
            new ButtonIdLabelAndDiceExpression("3_button", "D8", "1d8"),
            new ButtonIdLabelAndDiceExpression("4_button", "D10", "1d10"),
            new ButtonIdLabelAndDiceExpression("5_button", "D12", "1d12"),
            new ButtonIdLabelAndDiceExpression("6_button", "D20", "1d20"),
            new ButtonIdLabelAndDiceExpression("7_button", "D100", "1d100"),
            new ButtonIdLabelAndDiceExpression("8_button", "D20 Advantage", "2d20k1="),
            new ButtonIdLabelAndDiceExpression("9_button", "D20 Disadvantage", "2d20L1="),
            new ButtonIdLabelAndDiceExpression("10_button", "2D4", "2d4="),
            new ButtonIdLabelAndDiceExpression("11_button", "2D6", "2d6="),
            new ButtonIdLabelAndDiceExpression("12_button", "2D8", "2d8="),
            new ButtonIdLabelAndDiceExpression("13_button", "2D10", "2d10="),
            new ButtonIdLabelAndDiceExpression("14_button", "2D12", "2d12="),
            new ButtonIdLabelAndDiceExpression("15_button", "2D20", "2d20=")
    ), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);

    private final SumCustomSetConfig DICE_CALCULATOR_CONFIG = new SumCustomSetConfig(null, List.of(
            new ButtonIdLabelAndDiceExpression("1_button", "7", "7"), new ButtonIdLabelAndDiceExpression("2_button", "8", "8"), new ButtonIdLabelAndDiceExpression("3_button", "9", "9"), new ButtonIdLabelAndDiceExpression("5_button", "+", "+"), new ButtonIdLabelAndDiceExpression("6_button", "-", "-"),
            new ButtonIdLabelAndDiceExpression("7_button", "4", "4"), new ButtonIdLabelAndDiceExpression("8_button", "5", "5"), new ButtonIdLabelAndDiceExpression("9_button", "6", "6"), new ButtonIdLabelAndDiceExpression("10_button", "d", "d"), new ButtonIdLabelAndDiceExpression("11_button", "k", "k"),
            new ButtonIdLabelAndDiceExpression("12_button", "1", "1"), new ButtonIdLabelAndDiceExpression("13_button", "2", "2"), new ButtonIdLabelAndDiceExpression("14_button", "3", "3"), new ButtonIdLabelAndDiceExpression("15_button", "0", "0"), new ButtonIdLabelAndDiceExpression("16_button", "l", "l")
    ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full);

    public WelcomeCommand(MessageDataDAO messageDataDAO) {
        super(messageDataDAO);
    }

    @Override
    protected Optional<ConfigAndState<Config, StateData>> getMessageDataAndUpdateWithButtonValue(long channelId,
                                                                                                 long messageId,
                                                                                                 @NonNull String buttonValue,
                                                                                                 @NonNull String invokingUserName) {
        return Optional.of(new ConfigAndState<>(UUID.randomUUID(), new Config(null, AnswerFormatType.full), new State<>(buttonValue, StateData.empty())));
    }

    @Override
    public Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID,
                                                                   long guildId,
                                                                   long channelId,
                                                                   long messageId,
                                                                   @NonNull Config config,
                                                                   @Nullable State<StateData> state) {
        if (state == null) {
            return Optional.empty();
        }
        return switch (state.getButtonValue()) {
            case FATE_BUTTON_ID ->
                    new FateCommand(messageDataDAO).createMessageDataForNewMessage(configUUID, guildId, channelId, messageId, FATE_CONFIG, null);
            case DND5_BUTTON_ID ->
                    new CustomDiceCommand(messageDataDAO).createMessageDataForNewMessage(configUUID, guildId, channelId, messageId, DND5_CONFIG, null);
            case NWOD_BUTTON_ID ->
                    new CountSuccessesCommand(messageDataDAO).createMessageDataForNewMessage(configUUID, guildId, channelId, messageId, NWOD_CONFIG, null);
            case OWOD_BUTTON_ID ->
                    new PoolTargetCommand(messageDataDAO).createMessageDataForNewMessage(configUUID, guildId, channelId, messageId, OWOD_CONFIG, null);
            case SHADOWRUN_BUTTON_ID ->
                    new CountSuccessesCommand(messageDataDAO).createMessageDataForNewMessage(configUUID, guildId, channelId, messageId, SHADOWRUN_CONFIG, null);
            case COIN_BUTTON_ID ->
                    new CustomDiceCommand(messageDataDAO).createMessageDataForNewMessage(configUUID, guildId, channelId, messageId, COIN_CONFIG, null);
            case DICE_CALCULATOR_ID ->
                    new SumCustomSetCommand(messageDataDAO).createMessageDataForNewMessage(configUUID, guildId, channelId, messageId, DICE_CALCULATOR_CONFIG, null);
            default -> Optional.empty();
        };
    }

    @Override
    public String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Displays the welcome message";
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage() {
        return EmbedOrMessageDefinition.builder().descriptionOrContent("Displays the welcome message")
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server", "https://discord.gg/e43BsqKpFr", false))
                .build();
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(Config config, State<StateData> state) {
        BotMetrics.incrementButtonMetricCounter(COMMAND_NAME, "[" + state.getButtonValue() + "]");
        return switch (state.getButtonValue()) {
            case FATE_BUTTON_ID -> Optional.of(new FateCommand(messageDataDAO).createNewButtonMessage(FATE_CONFIG));
            case DND5_BUTTON_ID ->
                    Optional.of(new CustomDiceCommand(messageDataDAO).createNewButtonMessage(DND5_CONFIG));
            case NWOD_BUTTON_ID ->
                    Optional.of(new CountSuccessesCommand(messageDataDAO).createNewButtonMessage(NWOD_CONFIG));
            case OWOD_BUTTON_ID ->
                    Optional.of(new PoolTargetCommand(messageDataDAO).createNewButtonMessage(OWOD_CONFIG));
            case SHADOWRUN_BUTTON_ID -> Optional.of(
                    new CountSuccessesCommand(messageDataDAO).createNewButtonMessage(SHADOWRUN_CONFIG)
            );
            case COIN_BUTTON_ID -> Optional.of(
                    new CustomDiceCommand(messageDataDAO).createNewButtonMessage(COIN_CONFIG)
            );
            case DICE_CALCULATOR_ID -> Optional.of(
                    new SumCustomSetCommand(messageDataDAO).createNewButtonMessage(DICE_CALCULATOR_CONFIG)
            );
            default -> Optional.empty();
        };
    }

    @Override
    protected boolean shouldKeepExistingButtonMessage(@NonNull ButtonEventAdaptor event) {
        return true;
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(Config config, State<StateData> state) {
        return Optional.empty();
    }


    public MessageDefinition getWelcomeMessage() {
        return createNewButtonMessage(null);
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(Config config) {
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
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), FATE_BUTTON_ID))
                                                .label("Fate")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), DND5_BUTTON_ID))
                                                .label("D&D5e")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), NWOD_BUTTON_ID))
                                                .label("nWoD")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), DICE_CALCULATOR_ID))
                                                .label("Dice Calculator")
                                                .build()
                                )
                        )
                        .build())
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .buttonDefinitions(
                                ImmutableList.of(
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), OWOD_BUTTON_ID))
                                                .label("oWoD")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), SHADOWRUN_BUTTON_ID))
                                                .label("Shadowrun")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), COIN_BUTTON_ID))
                                                .label("Coin Toss \uD83E\uDE99")
                                                .build()
                                )
                        )
                        .build())
                .build();
    }

    @Override
    protected @NonNull Config getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        return new Config(null, AnswerFormatType.full);
    }

    @Override
    protected @NonNull Config getConfigFromEvent(@NonNull ButtonEventAdaptor event) {
        return new Config(null, AnswerFormatType.full);
    }

    @Override
    protected @NonNull State<StateData> getStateFromEvent(@NonNull ButtonEventAdaptor event) {
        String buttonId = BottomCustomIdUtils.getButtonValueFromLegacyCustomId(event.getCustomId());
        return new State<>(buttonId, StateData.empty());
    }
}
