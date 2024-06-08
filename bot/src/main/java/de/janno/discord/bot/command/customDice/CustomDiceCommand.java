package de.janno.discord.bot.command.customDice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.command.channelConfig.AliasHelper;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.dice.DiceSystemAdapter;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class CustomDiceCommand extends AbstractCommand<CustomDiceConfig, StateData> {

    public static final String COMMAND_NAME = "custom_dice";
    static final String BUTTONS_OPTION_NAME = "buttons";
    private static final String CONFIG_TYPE_ID = "CustomDiceConfig";
    private final DiceSystemAdapter diceSystemAdapter;

    public CustomDiceCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        super(persistenceManager);
        this.diceSystemAdapter = new DiceSystemAdapter(cachingDiceEvaluator);
    }

    public static CustomDiceConfig deserializeConfig(@NonNull MessageConfigDTO messageConfigDTO) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        return Mapper.deserializeObject(messageConfigDTO.getConfig(), CustomDiceConfig.class);
    }

    @Override
    protected ConfigAndState<CustomDiceConfig, StateData> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                                 @NonNull MessageDataDTO messageDataDTO,
                                                                                                 @NonNull String buttonValue,
                                                                                                 @NonNull String invokingUserName) {
        return deserializeAndUpdateState(messageConfigDTO, buttonValue);
    }

    @VisibleForTesting
    ConfigAndState<CustomDiceConfig, StateData> deserializeAndUpdateState(@NonNull MessageConfigDTO messageConfigDTO, @NonNull String buttonValue) {
        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(),
                deserializeConfig(messageConfigDTO),
                new State<>(buttonValue, StateData.empty()));
    }

    @Override
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, @Nullable Long guildId, long channelId, @NonNull CustomDiceConfig config) {
        return Optional.of(new MessageConfigDTO(configUUID, guildId, channelId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return List.of(CommandDefinitionOption.builder()
                .name(BUTTONS_OPTION_NAME)
                .nameLocales(I18n.allNoneEnglishMessagesNames("custom_dice.option.buttons.name"))
                .description(I18n.getMessage("custom_dice.option.buttons.description", Locale.ENGLISH))
                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("custom_dice.option.buttons.description"))
                .type(CommandDefinitionOption.Type.STRING)
                .required(true)
                .build());
    }


    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage(Locale userLocale) {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent(I18n.getMessage("custom_dice.help.message", userLocale) + "\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.example.field.name", userLocale), I18n.getMessage("custom_dice.help.example.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.documentation.field.name", userLocale), I18n.getMessage("help.documentation.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.discord.server.field.name", userLocale), I18n.getMessage("help.discord.server.field.value", userLocale), false))
                .build();
    }

    @Override
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options, long channelId, long userId, @NonNull Locale userLocale) {
        Optional<String> validateLayout = ButtonHelper.valdiate(options.getStringSubOptionWithName(BUTTONS_OPTION_NAME).orElseThrow(), userLocale, List.of(), false);
        if (validateLayout.isPresent()) {
            return validateLayout;
        }
        List<String> diceExpressionWithOptionalLabel = getButtonsFromCommandOption(options).stream()
                .map(ButtonIdLabelAndDiceExpression::getDiceExpression)
                .map(e -> AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, e))
                .distinct()
                .collect(Collectors.toList());

        return diceSystemAdapter.validateListOfExpressions(diceExpressionWithOptionalLabel, "/%s %s".formatted(I18n.getMessage("custom_dice.name", userLocale),
                I18n.getMessage("base.option.help", userLocale)), userLocale);
    }

    private List<ButtonIdLabelAndDiceExpression> getButtonsFromCommandOption(@NonNull CommandInteractionOption options) {
        String buttons = options.getStringSubOptionWithName(BUTTONS_OPTION_NAME).orElseThrow();
        return ButtonHelper.parseString(buttons);
    }

    protected @NonNull CustomDiceConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options, @NonNull Locale userLocale) {
        return getConfigOptionStringList(getButtonsFromCommandOption(options),
                BaseCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null),
                BaseCommandOptions.getAnswerTypeFromStartCommandOption(options).orElse(defaultAnswerFormat()),
                BaseCommandOptions.getAnswerInteractionFromStartCommandOption(options),
                BaseCommandOptions.getDiceStyleOptionFromStartCommandOption(options).orElse(DiceImageStyle.polyhedral_3d),
                BaseCommandOptions.getDiceColorOptionFromStartCommandOption(options).orElse(DiceImageStyle.polyhedral_3d.getDefaultColor()),
                userLocale
        );
    }

    @VisibleForTesting
    CustomDiceConfig getConfigOptionStringList(List<ButtonIdLabelAndDiceExpression> buttons,
                                               Long channelId,
                                               AnswerFormatType answerFormatType,
                                               AnswerInteractionType answerInteractionType,
                                               DiceImageStyle diceImageStyle,
                                               String defaultDiceColor,
                                               @NonNull Locale userLocale) {
        return new CustomDiceConfig(channelId,
                buttons,
                answerFormatType,
                answerInteractionType,
                null,
                new DiceStyleAndColor(diceImageStyle, defaultDiceColor),
                userLocale
        );
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(CustomDiceConfig config, State<StateData> state, long channelId, long userId) {
        Optional<ButtonIdLabelAndDiceExpression> selectedButton = Optional.ofNullable(state).map(State::getButtonValue)
                .flatMap(bv -> config.getButtonIdLabelAndDiceExpressions().stream()
                        .filter(bld -> bld.getButtonId().equals(bv))
                        .findFirst()
                );
        if (selectedButton.isEmpty()) {
            return Optional.empty();
        }
        //add the label only if it is different from the expression
        final String label = selectedButton.get().getDiceExpression().equals(selectedButton.get().getLabel()) ? null : selectedButton.get().getLabel();
        final String expression = AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, selectedButton.get().getDiceExpression());
        return Optional.of(diceSystemAdapter.answerRollWithGivenLabel(expression,
                label,
                false,
                config.getAnswerFormatType(),
                config.getDiceStyleAndColor(),
                config.getConfigLocale()));
    }

    @Override
    protected @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessageWithState(@NonNull UUID configUUID,
                                                                                          @NonNull CustomDiceConfig config,
                                                                                          @Nullable State<StateData> state,
                                                                                          @Nullable Long guildId,
                                                                                          long channelId) {
        return Optional.of(createSlashResponseMessage(configUUID, config, channelId));
    }

    @Override
    public @NonNull EmbedOrMessageDefinition createSlashResponseMessage(@NonNull UUID configUUID, @NonNull CustomDiceConfig config, long channelId) {
        return EmbedOrMessageDefinition.builder()
                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                .descriptionOrContent(I18n.getMessage("custom_dice.buttonMessage.message", config.getConfigLocale()))
                .componentRowDefinitions(createButtonLayout(configUUID, config))
                .build();
    }

    private List<ComponentRowDefinition> createButtonLayout(UUID configUUID, CustomDiceConfig config) {
        return ButtonHelper.createButtonLayout(getCommandId(), configUUID, config.getButtonIdLabelAndDiceExpressions());
    }

    @Override
    public @NonNull String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull Optional<String> getConfigWarnMessage(CustomDiceConfig config, Locale userLocale) {
        return Optional.ofNullable(Strings.emptyToNull(config.getButtonIdLabelAndDiceExpressions().stream()
                .map(b -> {
                    String warning = diceSystemAdapter.answerRollWithGivenLabel(b.getDiceExpression(), null, false, config.getAnswerFormatType(),
                            config.getDiceStyleAndColor(), userLocale).getWarning();
                    if (!Strings.isNullOrEmpty(warning)) {
                        return "`%s`: %s".formatted(b.getDiceExpression(), warning);
                    }
                    return null;
                })
                .filter(s -> !Strings.isNullOrEmpty(s))
                .distinct()
                .collect(Collectors.joining(", "))));
    }

}
