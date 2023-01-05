package de.janno.discord.bot.command.fate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class FateCommand extends AbstractCommand<FateConfig, StateData> {

    private static final String COMMAND_NAME = "fate";
    private static final String ACTION_MODIFIER_OPTION = "type";
    private static final String ACTION_MODIFIER_OPTION_SIMPLE = "simple";
    private static final String ACTION_MODIFIER_OPTION_MODIFIER = "with_modifier";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String CONFIG_TYPE_ID = "FateConfig";
    private final DiceUtils diceUtils;

    @VisibleForTesting
    public FateCommand(MessageDataDAO messageDataDAO, DiceUtils diceUtils) {
        super(messageDataDAO);
        this.diceUtils = diceUtils;
    }

    public FateCommand(MessageDataDAO messageDataDAO) {
        this(messageDataDAO, new DiceUtils());
    }

    @Override
    protected Optional<ConfigAndState<FateConfig, StateData>> getMessageDataAndUpdateWithButtonValue(long channelId,
                                                                                                     long messageId,
                                                                                                     @NonNull String buttonValue,
                                                                                                     @NonNull String invokingUserName) {
        final Optional<MessageDataDTO> messageDataDTO = messageDataDAO.getDataForMessage(channelId, messageId);
        return messageDataDTO.map(dataDTO -> deserializeAndUpdateState(dataDTO, buttonValue));
    }

    @VisibleForTesting
    ConfigAndState<FateConfig, StateData> deserializeAndUpdateState(@NonNull MessageDataDTO messageDataDTO, @NonNull String buttonValue) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageDataDTO.getConfigClassId()), "Unknown configClassId: %s", messageDataDTO.getConfigClassId());

        return new ConfigAndState<>(messageDataDTO.getConfigUUID(),
                Mapper.deserializeObject(messageDataDTO.getConfig(), FateConfig.class),
                new State<>(buttonValue, StateData.empty()));
    }


    @Override
    public Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID,
                                                                   long guildId,
                                                                   long channelId,
                                                                   long messageId,
                                                                   @NonNull FateConfig config,
                                                                   @Nullable State<StateData> state) {
        return Optional.of(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(),
                CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    public String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure Fate dice";
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
                .field(new EmbedOrMessageDefinition.Field("Discord Server", "https://discord.gg/e43BsqKpFr", false))
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
        return new FateConfig(getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null),
                options.getStringSubOptionWithName(ACTION_MODIFIER_OPTION).orElse(ACTION_MODIFIER_OPTION_SIMPLE),
                getAnswerTypeFromStartCommandOption(options),
                getResultImageOptionFromStartCommandOption(options));
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(FateConfig config, State<StateData> state) {
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
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(FateConfig config, State<StateData> state) {
        return Optional.of(createNewButtonMessage(config));
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(FateConfig config) {
        return MessageDefinition.builder()
                .content(createButtonMessage(config))
                .componentRowDefinitions(createButtonLayout(config))
                .build();
    }

    private List<ComponentRowDefinition> createButtonLayout(FateConfig config) {
        if (ACTION_MODIFIER_OPTION_MODIFIER.equals(config.getType())) {
            return ImmutableList.of(
                    ComponentRowDefinition.builder().buttonDefinitions(
                            ImmutableList.of(
                                    //              ID,  label
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-4")).label("-4").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-3")).label("-3").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-2")).label("-2").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-1")).label("-1").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "0")).label("0").build()
                            )
                    ).build(),
                    ComponentRowDefinition.builder().buttonDefinitions(
                            ImmutableList.of(
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "1")).label("+1").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "2")).label("+2").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "3")).label("+3").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "4")).label("+4").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "5")).label("+5").build()
                            )
                    ).build(),
                    ComponentRowDefinition.builder().buttonDefinitions(
                            ImmutableList.of(
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "6")).label("+6").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "7")).label("+7").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "8")).label("+8").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "9")).label("+9").build(),
                                    ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "10")).label("+10").build()
                            )
                    ).build());
        } else {
            return ImmutableList.of(
                    ComponentRowDefinition.builder().buttonDefinition(
                            ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ROLL_BUTTON_ID)).label("Roll 4dF").build()
                    ).build());
        }
    }

}
