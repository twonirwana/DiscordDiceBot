package de.janno.discord.bot.command.fate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.EmptyData;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.BotConstants;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
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
public class FateCommand extends AbstractCommand<FateConfig, EmptyData> {

    private static final String COMMAND_NAME = "fate";
    private static final String ACTION_MODIFIER_OPTION = "type";
    private static final String ACTION_MODIFIER_OPTION_SIMPLE = "simple";
    private static final String ACTION_MODIFIER_OPTION_MODIFIER = "with_modifier";
    private static final String ROLL_BUTTON_ID = "roll";
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
    protected Optional<ConfigAndState<FateConfig, EmptyData>> getMessageDataAndUpdateWithButtonValue(long channelId, long messageId, String buttonValue) {
        final Optional<MessageDataDTO> messageDataDTO = messageDataDAO.getDataForMessage(channelId, messageId);
        if (messageDataDTO.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ConfigAndState<>(messageDataDTO.get().getConfigUUID(),
                Mapper.deserializeObject(messageDataDTO.get().getConfig(), FateConfig.class),
                new State<>(buttonValue, new EmptyData())));
    }

    @Override
    protected MessageDataDTO createMessageDataForNewMessage(@NonNull UUID configUUID,
                                                            long channelId,
                                                            long messageId,
                                                            @NonNull FateConfig config,
                                                            @Nullable State<EmptyData> state) {
        return new MessageDataDTO(configUUID, channelId, messageId, getCommandId(),
                "FateConfig", Mapper.serializedObject(config),
                Mapper.NO_PERSISTED_STATE, null);
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
    protected EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Buttons for Fate/Fudge dice. There are two types, the simple produces one button that rolls four dice and " +
                        "provides the result together with the sum. The type with_modifier produces multiple buttons for modifier -4 to +10" +
                        " that roll four dice and add the modifier of the button.")
                .field(new EmbedDefinition.Field("Example", "'/fate start type:with_modifier' or '/fate start type:simple'", false))
                .build();
    }

    @Override
    protected List<CommandDefinitionOption> getStartOptions() {
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
                        .build(),
                ANSWER_TARGET_CHANNEL_COMMAND_OPTION);
    }

    @Override
    protected FateConfig getConfigFromStartOptions(CommandInteractionOption options) {
        return new FateConfig(getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null),
                options.getStringSubOptionWithName(ACTION_MODIFIER_OPTION).orElse(ACTION_MODIFIER_OPTION_SIMPLE));
    }

    @Override
    protected Optional<EmbedDefinition> getAnswer(State<EmptyData> state, FateConfig config) {
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

            String title = String.format("4dF%s = %d", modifierString, resultWithModifier);
            String details = DiceUtils.convertFateNumberToString(rollResult);
            return Optional.of(new EmbedDefinition(title, details, ImmutableList.of()));
        } else {
            String title = String.format("4dF = %d", DiceUtils.fateResult(rollResult));
            String details = DiceUtils.convertFateNumberToString(rollResult);
            return Optional.of(new EmbedDefinition(title, details, ImmutableList.of()));
        }
    }

    @Override
    protected Optional<MessageDefinition> createNewButtonMessageWithState(State<EmptyData> state, FateConfig config) {
        return Optional.of(createNewButtonMessage(config));
    }

    @Override
    public MessageDefinition createNewButtonMessage(FateConfig config) {
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
                                    ButtonDefinition.builder().id(createButtonCustomId("-4")).label("-4").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("-3")).label("-3").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("-2")).label("-2").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("-1")).label("-1").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("0")).label("0").build()
                            )
                    ).build(),
                    ComponentRowDefinition.builder().buttonDefinitions(
                            ImmutableList.of(
                                    ButtonDefinition.builder().id(createButtonCustomId("1")).label("+1").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("2")).label("+2").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("3")).label("+3").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("4")).label("+4").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("5")).label("+5").build()
                            )
                    ).build(),
                    ComponentRowDefinition.builder().buttonDefinitions(
                            ImmutableList.of(
                                    ButtonDefinition.builder().id(createButtonCustomId("6")).label("+6").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("7")).label("+7").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("8")).label("+8").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("9")).label("+9").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("10")).label("+10").build()
                            )
                    ).build());
        } else {
            return ImmutableList.of(
                    ComponentRowDefinition.builder().buttonDefinition(
                            ButtonDefinition.builder().id(createButtonCustomId(ROLL_BUTTON_ID)).label("Roll 4dF").build()
                    ).build());
        }
    }

    @Override
    protected FateConfig getConfigFromEvent(IButtonEventAdaptor event) {
        String[] split = event.getCustomId().split(BotConstants.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
        return new FateConfig(getOptionalLongFromArray(split, 3), split[2]);
    }

    @Override
    protected State<EmptyData> getStateFromEvent(IButtonEventAdaptor event) {
        String buttonValue = event.getCustomId().split(BotConstants.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX)[1];
        return new State<>(buttonValue, new EmptyData());
    }

}
