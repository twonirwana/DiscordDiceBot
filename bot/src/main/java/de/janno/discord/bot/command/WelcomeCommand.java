package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesCommand;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesConfig;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.fate.FateCommand;
import de.janno.discord.bot.command.fate.FateConfig;
import de.janno.discord.bot.command.poolTarget.PoolTargetCommand;
import de.janno.discord.bot.command.poolTarget.PoolTargetConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
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

import java.util.*;

@Slf4j
public class WelcomeCommand extends AbstractCommand<Config, StateData> {

    public static final String COMMAND_NAME = "welcome";
    private final static FateConfig FATE_CONFIG = new FateConfig(null, "with_modifier", AnswerFormatType.full, ResultImage.none);
    private final static CountSuccessesConfig NWOD_CONFIG = new CountSuccessesConfig(null, 10, 8, "no_glitch", 15, 1, Set.of(10), Set.of(), AnswerFormatType.full, ResultImage.none);
    private final static CountSuccessesConfig SHADOWRUN_CONFIG = new CountSuccessesConfig(null, 6, 5, "half_dice_one", 20, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);
    private final static PoolTargetConfig OWOD_CONFIG = new PoolTargetConfig(null, 10, 15, ImmutableSet.of(10), ImmutableSet.of(1), "ask", AnswerFormatType.full, ResultImage.none);
    private final static CustomDiceConfig COIN_CONFIG = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Coin Toss \uD83E\uDE99", "1d[Head \uD83D\uDE00/Tail \uD83E\uDD85]")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, ResultImage.none);
    private final static CustomDiceConfig DND5_CONFIG_WITH_IMAGE = new CustomDiceConfig(null, ImmutableList.of(
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
    ), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, ResultImage.polyhedral_3d_red_and_white);
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
    ), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none);
    private final SumCustomSetConfig DICE_CALCULATOR_CONFIG = new SumCustomSetConfig(null, List.of(
            new ButtonIdLabelAndDiceExpression("1_button", "7", "7"), new ButtonIdLabelAndDiceExpression("2_button", "8", "8"), new ButtonIdLabelAndDiceExpression("3_button", "9", "9"), new ButtonIdLabelAndDiceExpression("5_button", "+", "+"), new ButtonIdLabelAndDiceExpression("6_button", "-", "-"),
            new ButtonIdLabelAndDiceExpression("7_button", "4", "4"), new ButtonIdLabelAndDiceExpression("8_button", "5", "5"), new ButtonIdLabelAndDiceExpression("9_button", "6", "6"), new ButtonIdLabelAndDiceExpression("10_button", "d", "d"), new ButtonIdLabelAndDiceExpression("11_button", "k", "k"),
            new ButtonIdLabelAndDiceExpression("12_button", "1", "1"), new ButtonIdLabelAndDiceExpression("13_button", "2", "2"), new ButtonIdLabelAndDiceExpression("14_button", "3", "3"), new ButtonIdLabelAndDiceExpression("15_button", "0", "0"), new ButtonIdLabelAndDiceExpression("16_button", "l", "l")
    ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, ResultImage.none);
    private final CustomParameterConfig FATE_WITH_IMAGE_CONFIG = new CustomParameterConfig(null, "4d[-1,0,1]+{Modifier:-4<=>10}=", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, ResultImage.fate_black);

    private final CachingDiceEvaluator cachingDiceEvaluator;

    public WelcomeCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        super(persistenceManager);
        this.cachingDiceEvaluator = cachingDiceEvaluator;
    }

    @Override
    protected Optional<ConfigAndState<Config, StateData>> getMessageDataAndUpdateWithButtonValue(@Nullable UUID configUUID,
                                                                                                 long channelId,
                                                                                                 long messageId,
                                                                                                 @NonNull String buttonValue,
                                                                                                 @NonNull String invokingUserName) {
        return Optional.of(new ConfigAndState<>(UUID.randomUUID(), new Config(null, AnswerFormatType.full, ResultImage.none), new State<>(buttonValue, StateData.empty())));
    }

    @Override
    protected boolean supportsResultImages() {
        return false;
    }

    @Override
    protected boolean supportsAnswerFormat() {
        return false;
    }

    @Override
    protected boolean supportsTargetChannel() {
        return false;
    }

    @Override
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, long guildId, long channelId, @NonNull Config config) {
        return Optional.empty();
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
                .field(new EmbedOrMessageDefinition.Field("Discord Server for Help and News", "https://discord.gg/e43BsqKpFr", false))
                .build();
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(UUID configUUID, Config config, State<StateData> state, long guildId, long channelId) {
        BotMetrics.incrementButtonMetricCounter(COMMAND_NAME, "[" + state.getButtonValue() + "]");
        if (ButtonIds.isInvalid(state.getButtonValue())) {
            return Optional.empty();
        }
        UUID newConfigUUID = UUID.randomUUID();
        return switch (ButtonIds.valueOf(state.getButtonValue())) {
            case fate -> {
                FateCommand command = new FateCommand(persistenceManager);
                command.createMessageConfig(newConfigUUID, guildId, channelId, FATE_CONFIG).ifPresent(persistenceManager::saveConfig);
                yield Optional.of(command.createNewButtonMessage(newConfigUUID, FATE_CONFIG));
            }
            case fate_image -> {
                CustomParameterCommand command = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
                command.createMessageConfig(newConfigUUID, guildId, channelId, FATE_WITH_IMAGE_CONFIG).ifPresent(persistenceManager::saveConfig);
                yield Optional.of(command.createNewButtonMessage(newConfigUUID, FATE_WITH_IMAGE_CONFIG));
            }
            case dnd5 -> {
                CustomDiceCommand command = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
                command.createMessageConfig(newConfigUUID, guildId, channelId, DND5_CONFIG).ifPresent(persistenceManager::saveConfig);
                yield Optional.of(command.createNewButtonMessage(newConfigUUID, DND5_CONFIG));
            }
            case dnd5_image -> {
                CustomDiceCommand command = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
                command.createMessageConfig(newConfigUUID, guildId, channelId, DND5_CONFIG_WITH_IMAGE).ifPresent(persistenceManager::saveConfig);
                yield Optional.of(command.createNewButtonMessage(newConfigUUID, DND5_CONFIG_WITH_IMAGE));
            }
            case nWoD -> {
                CountSuccessesCommand command = new CountSuccessesCommand(persistenceManager);
                command.createMessageConfig(newConfigUUID, guildId, channelId, NWOD_CONFIG).ifPresent(persistenceManager::saveConfig);
                yield Optional.of(command.createNewButtonMessage(newConfigUUID, NWOD_CONFIG));
            }
            case oWoD -> {
                PoolTargetCommand command = new PoolTargetCommand(persistenceManager);
                command.createMessageConfig(newConfigUUID, guildId, channelId, OWOD_CONFIG).ifPresent(persistenceManager::saveConfig);
                yield Optional.of(command.createNewButtonMessage(newConfigUUID, OWOD_CONFIG));
            }
            case shadowrun -> {
                CountSuccessesCommand command = new CountSuccessesCommand(persistenceManager);
                command.createMessageConfig(newConfigUUID, guildId, channelId, SHADOWRUN_CONFIG).ifPresent(persistenceManager::saveConfig);
                yield Optional.of(command.createNewButtonMessage(newConfigUUID, SHADOWRUN_CONFIG));
            }
            case coin -> {
                CustomDiceCommand command = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
                command.createMessageConfig(newConfigUUID, guildId, channelId, COIN_CONFIG).ifPresent(persistenceManager::saveConfig);
                yield Optional.of(command.createNewButtonMessage(newConfigUUID, COIN_CONFIG));
            }
            case dice_calculator -> {
                SumCustomSetCommand command = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
                command.createMessageConfig(newConfigUUID, guildId, channelId, DICE_CALCULATOR_CONFIG).ifPresent(persistenceManager::saveConfig);
                yield Optional.of(command.createNewButtonMessage(newConfigUUID, DICE_CALCULATOR_CONFIG));
            }
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
        return createNewButtonMessage(UUID.randomUUID(), null);
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(UUID configUUID, Config config) {
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
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.fate.name(), configUUID))
                                                .label("Fate")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.fate_image.name(), configUUID))
                                                .label("Fate with dice images")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.dnd5.name(), configUUID))
                                                .label("D&D5e")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.dnd5_image.name(), configUUID))
                                                .label("D&D5e with dice images")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.nWoD.name(), configUUID))
                                                .label("nWoD")
                                                .build()

                                )
                        )
                        .build())
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .buttonDefinitions(
                                ImmutableList.of(
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.oWoD.name(), configUUID))
                                                .label("oWoD")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.shadowrun.name(), configUUID))
                                                .label("Shadowrun")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.coin.name(), configUUID))
                                                .label("Coin Toss \uD83E\uDE99")
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.dice_calculator.name(), configUUID))
                                                .label("Dice Calculator")
                                                .build()
                                )
                        )
                        .build())
                .build();
    }

    @Override
    protected @NonNull Config getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        return new Config(null, AnswerFormatType.full, ResultImage.none);
    }

    private enum ButtonIds {
        fate,
        fate_image,
        dnd5,
        dnd5_image,
        nWoD,
        oWoD,
        shadowrun,
        coin,
        dice_calculator;

        public static boolean isInvalid(String in) {
            return Arrays.stream(ButtonIds.values()).noneMatch(s -> s.name().equals(in));
        }
    }
}
