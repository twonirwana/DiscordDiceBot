package de.janno.discord.bot.command.fate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class FateCommand extends AbstractCommand<FateConfig, StateData> {

    static final String ACTION_MODIFIER_OPTION = "type";
    private static final String COMMAND_NAME = "fate";
    private static final String ACTION_MODIFIER_OPTION_SIMPLE = "simple";
    private static final String ACTION_MODIFIER_OPTION_MODIFIER = "with_modifier";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String CONFIG_TYPE_ID = "FateConfig";
    private final DiceUtils diceUtils;

    @VisibleForTesting
    public FateCommand(PersistenceManager persistenceManager, DiceUtils diceUtils) {
        super(persistenceManager);
        this.diceUtils = diceUtils;
    }

    public FateCommand(PersistenceManager persistenceManager) {
        this(persistenceManager, new DiceUtils());
    }


    @Override
    protected ConfigAndState<FateConfig, StateData> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                           @NonNull MessageDataDTO messageDataDTO,
                                                                                           @NonNull String buttonValue,
                                                                                           @NonNull String invokingUserName) {
        return deserializeAndUpdateState(messageConfigDTO, buttonValue);
    }

    @VisibleForTesting
    ConfigAndState<FateConfig, StateData> deserializeAndUpdateState(@NonNull MessageConfigDTO messageConfigDTO, @NonNull String buttonValue) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());

        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(),
                Mapper.deserializeObject(messageConfigDTO.getConfig(), FateConfig.class),
                new State<>(buttonValue, StateData.empty()));
    }

    @Override
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, long guildId, long channelId, @NonNull FateConfig config) {
        return Optional.of(new MessageConfigDTO(configUUID, guildId, channelId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    public @NonNull String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure Fate dice";
    }

    @Override
    protected boolean supportsResultImages() {
        return false;
    }

    private String createButtonMessage(FateConfig config) {
        if (ACTION_MODIFIER_OPTION_MODIFIER.equals(config.getType())) {
            return "Click a button to roll four fate dice and add the value of the button";
        }
        return "Click a button to roll four fate dice";
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage() {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Buttons for Fate/Fudge dice. There are two types, the simple produces one button that rolls four dice and " +
                        "provides the result together with the sum. The type with_modifier produces multiple buttons for modifier -4 to +10" +
                        " that roll four dice and add the modifier of the button.")
                .field(new EmbedOrMessageDefinition.Field("Example", "`/fate start type:with_modifier` or `/fate start type:simple`", false))
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server for Help and News", "https://discord.gg/e43BsqKpFr", false))
                .build();
    }

    @Override
    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of(CommandDefinitionOption.builder()
                .name(ACTION_MODIFIER_OPTION)
                .required(true)
                .description("Show modifier buttons")
                .type(CommandDefinitionOption.Type.STRING)
                .choice(CommandDefinitionOptionChoice.builder()
                        .name(ACTION_MODIFIER_OPTION_SIMPLE)
                        .value(ACTION_MODIFIER_OPTION_SIMPLE)
                        .build())
                .choice(CommandDefinitionOptionChoice.builder()
                        .name(ACTION_MODIFIER_OPTION_MODIFIER)
                        .value(ACTION_MODIFIER_OPTION_MODIFIER)
                        .build())
                .build());
    }

    @Override
    protected @NonNull FateConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        Long answerTargetChannelId = BaseCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null);
        AnswerFormatType answerType = BaseCommandOptions.getAnswerTypeFromStartCommandOption(options).orElse(defaultAnswerFormat());
        return new FateConfig(answerTargetChannelId,
                options.getStringSubOptionWithName(ACTION_MODIFIER_OPTION).orElse(ACTION_MODIFIER_OPTION_SIMPLE),
                answerType,
                null,
                new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()));
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(FateConfig config, State<StateData> state, long channelId, long userId) {
        List<Integer> rollResult = diceUtils.rollFate();

        if (ACTION_MODIFIER_OPTION_MODIFIER.equals(config.getType())) {
            int modifier = Integer.parseInt(state.getButtonValue());
            String modifierString = "";
            if (modifier > 0) {
                modifierString = " +" + modifier;
            } else if (modifier < 0) {
                modifierString = " " + modifier;
            }
            int resultWithModifier = DiceUtils.fateResult(rollResult) + modifier;

            String details = DiceUtils.convertFateNumberToString(rollResult);
            return Optional.of(RollAnswer.builder()
                    .answerFormatType(config.getAnswerFormatType())
                    .expression(String.format("4dF%s", modifierString))
                    .result(String.valueOf(resultWithModifier))
                    .rollDetails(details)
                    .build());
        } else {
            String details = DiceUtils.convertFateNumberToString(rollResult);
            return Optional.of(RollAnswer.builder()
                    .answerFormatType(config.getAnswerFormatType())
                    .expression("4dF")
                    .result(String.valueOf(DiceUtils.fateResult(rollResult)))
                    .rollDetails(details)
                    .build());
        }
    }

    @Override
    protected @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessageWithState(UUID configUUID, FateConfig config, State<StateData> state, long guildId, long channelId) {
        return Optional.of(createNewButtonMessage(configUUID, config));
    }

    @Override
    public @NonNull EmbedOrMessageDefinition createNewButtonMessage(UUID configUUID, FateConfig config) {
        return EmbedOrMessageDefinition.builder()
                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                .descriptionOrContent(createButtonMessage(config))
                .componentRowDefinitions(createButtonLayout(configUUID, config))
                .build();
    }

    private List<ComponentRowDefinition> createButtonLayout(UUID configUUID, FateConfig config) {
        if (ACTION_MODIFIER_OPTION_MODIFIER.equals(config.getType())) {
            return ImmutableList.of(
                    ComponentRowDefinition.builder().buttonDefinitions(
                            ImmutableList.of(
                                    //              ID,  label
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-4", configUUID)).label("-4").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-3", configUUID)).label("-3").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-2", configUUID)).label("-2").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-1", configUUID)).label("-1").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "0", configUUID)).label("0").build()
                            )
                    ).build(),
                    ComponentRowDefinition.builder().buttonDefinitions(
                            ImmutableList.of(
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "1", configUUID)).label("+1").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "2", configUUID)).label("+2").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "3", configUUID)).label("+3").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "4", configUUID)).label("+4").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "5", configUUID)).label("+5").build()
                            )
                    ).build(),
                    ComponentRowDefinition.builder().buttonDefinitions(
                            ImmutableList.of(
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "6", configUUID)).label("+6").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "7", configUUID)).label("+7").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "8", configUUID)).label("+8").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "9", configUUID)).label("+9").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "10", configUUID)).label("+10").build()
                            )
                    ).build());
        } else {
            return ImmutableList.of(
                    ComponentRowDefinition.builder().buttonDefinition(
                            ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ROLL_BUTTON_ID, configUUID)).label("Roll 4dF").build()
                    ).build());
        }
    }

}
