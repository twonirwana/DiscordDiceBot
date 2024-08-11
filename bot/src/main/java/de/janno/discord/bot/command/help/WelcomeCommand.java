package de.janno.discord.bot.command.help;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.DiscordConnector;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class WelcomeCommand extends AbstractCommand<RollConfig, StateData> {

    private static final String COMMAND_NAME = "welcome";
    private static final String CONFIG_TYPE_ID = "Config";
    private final Supplier<UUID> uuidSupplier;
    private final RpgSystemCommandPreset rpgSystemCommandPreset;

    public WelcomeCommand(PersistenceManager persistenceManager, RpgSystemCommandPreset rpgSystemCommandPreset) {
        this(persistenceManager, rpgSystemCommandPreset, UUID::randomUUID);
    }

    @VisibleForTesting
    public WelcomeCommand(PersistenceManager persistenceManager, RpgSystemCommandPreset rpgSystemCommandPreset, Supplier<UUID> uuidSupplier) {
        super(persistenceManager, uuidSupplier);
        this.uuidSupplier = uuidSupplier;
        this.rpgSystemCommandPreset = rpgSystemCommandPreset;
    }

    @Override
    protected ConfigAndState<RollConfig, StateData> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                           @NonNull MessageDataDTO messageDataDTO,
                                                                                           @NonNull String buttonValue,
                                                                                           @NonNull String invokingUserName) {
        return deserializeAndUpdateState(messageConfigDTO, buttonValue);
    }

    ConfigAndState<RollConfig, StateData> deserializeAndUpdateState(@NonNull MessageConfigDTO messageConfigDTO, @NonNull String buttonValue) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        final RollConfig loadedConfig = Mapper.deserializeObject(messageConfigDTO.getConfig(), RollConfig.class);
        final State<StateData> updatedState = new State<>(buttonValue, StateData.empty());
        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(), loadedConfig, updatedState);
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
    protected boolean supportsAnswerInteraction() {
        return false;
    }

    @Override
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, @Nullable Long guildId, long channelId, @NonNull RollConfig config) {
        return Optional.of(new MessageConfigDTO(configUUID, guildId, channelId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    public @NonNull String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage(Locale userLocale) {
        return EmbedOrMessageDefinition.builder().descriptionOrContent(I18n.getMessage("welcome.help.message", userLocale))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.documentation.field.name", userLocale), I18n.getMessage("help.documentation.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.discord.server.field.name", userLocale), I18n.getMessage("help.discord.server.field.value", userLocale), false))
                .build();
    }

    @Override
    protected @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessageWithState(@NonNull UUID configUUID,
                                                                                          @NonNull RollConfig config,
                                                                                          @Nullable State<StateData> state,
                                                                                          @Nullable Long guildId,
                                                                                          long channelId) {
        if (state == null) {
            return Optional.of(createSlashResponseMessage(configUUID, config, channelId));
        }

        BotMetrics.incrementButtonMetricCounter(COMMAND_NAME);
        UUID newConfigUUID = uuidSupplier.get();

        final Optional<RpgSystemCommandPreset.PresetId> presetId = getPresetIdFromButton(state.getButtonValue());
        return presetId.flatMap(id -> rpgSystemCommandPreset.createMessage(id, newConfigUUID, guildId, channelId, config.getConfigLocale()));

    }

    private Optional<RpgSystemCommandPreset.PresetId> getPresetIdFromButton(String buttonValue) {
        if (RpgSystemCommandPreset.PresetId.isValid(buttonValue)) {
            return Optional.of(RpgSystemCommandPreset.PresetId.valueOf(buttonValue));
        }

        //legacy ids
        return switch (buttonValue) {
            case "fate" -> Optional.of(RpgSystemCommandPreset.PresetId.FATE);
            case "fate_image" -> Optional.of(RpgSystemCommandPreset.PresetId.FATE_IMAGE);
            case "dnd5" -> Optional.of(RpgSystemCommandPreset.PresetId.DND5);
            case "dnd5_image" -> Optional.of(RpgSystemCommandPreset.PresetId.DND5_IMAGE);
            case "nWoD" -> Optional.of(RpgSystemCommandPreset.PresetId.NWOD);
            case "oWoD" -> Optional.of(RpgSystemCommandPreset.PresetId.OWOD);
            case "shadowrun" -> Optional.of(RpgSystemCommandPreset.PresetId.SHADOWRUN_IMAGE);
            case "coin" -> Optional.of(RpgSystemCommandPreset.PresetId.COIN);
            case "dice_calculator" -> Optional.of(RpgSystemCommandPreset.PresetId.DICE_CALCULATOR);
            default -> Optional.empty();
        };
    }

    @Override
    protected boolean shouldKeepExistingButtonMessage(@NonNull ButtonEventAdaptor event) {
        return true;
    }


    @Override
    protected void addFurtherActions(List<Mono<Void>> actions, ButtonEventAdaptor event, RollConfig config, State<StateData> state) {
        Optional<RpgSystemCommandPreset.PresetId> presetId = getPresetIdFromButton(state.getButtonValue());

        if (presetId.isEmpty()) {
            log.warn("{}: Unknown welcome button id: {}",
                    event.getRequester().toLogString(),
                    state.getButtonValue());
        } else {
            BotMetrics.incrementPresetMetricCounter(presetId.get().name());
            String commandString = RpgSystemCommandPreset.getCommandString(presetId.get(), event.getRequester().getUserLocal());
            actions.add(Mono.defer(() -> event.sendMessage(EmbedOrMessageDefinition.builder()
                            .type(EmbedOrMessageDefinition.Type.MESSAGE)
                            .shortedContent("`%s`".formatted(commandString))
                            .build())).ofType(Void.class)
                    .doOnSuccess(v ->
                            log.info("{}: Welcome Button {}",
                                    event.getRequester().toLogString(),
                                    presetId.get().name()
                            )));
        }
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(RollConfig config, State<StateData> state, long channelId, long userId) {
        return Optional.empty();
    }

    public Function<DiscordConnector.WelcomeRequest, EmbedOrMessageDefinition> getWelcomeMessage() {
        return request -> {
            RollConfig config = new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), request.guildLocale());
            UUID configUUID = uuidSupplier.get();
            final Optional<MessageConfigDTO> newMessageConfig = createMessageConfig(configUUID, request.guildId(), request.channelId(), config);
            newMessageConfig.ifPresent(persistenceManager::saveMessageConfig);
            return createSlashResponseMessage(configUUID, config, request.channelId());
        };
    }

    @Override
    protected Optional<ConfigAndState<RollConfig, StateData>> createNewConfigAndStateIfMissing(String buttonValue) {
        return Optional.of(new ConfigAndState<>(uuidSupplier.get(), new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), Locale.ENGLISH), new State<>(buttonValue, StateData.empty())));
    }

    @Override
    public @NonNull EmbedOrMessageDefinition createSlashResponseMessage(@NonNull UUID configUUID, @NonNull RollConfig config, long channelId) {
        return EmbedOrMessageDefinition.builder()
                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                .descriptionOrContent(I18n.getMessage("welcome.message", config.getConfigLocale()))
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .buttonDefinitions(
                                List.of(
                                        createButtonDefinition(RpgSystemCommandPreset.PresetId.DND5_IMAGE, configUUID, config.getConfigLocale()),
                                        createButtonDefinition(RpgSystemCommandPreset.PresetId.DND5, configUUID, config.getConfigLocale()),
                                        createButtonDefinition(RpgSystemCommandPreset.PresetId.FATE_IMAGE, configUUID, config.getConfigLocale()),
                                        createButtonDefinition(RpgSystemCommandPreset.PresetId.COIN, configUUID, config.getConfigLocale())
                                )
                        )
                        .build())
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .buttonDefinitions(
                                List.of(
                                        createButtonDefinition(RpgSystemCommandPreset.PresetId.NWOD, configUUID, config.getConfigLocale()),
                                        createButtonDefinition(RpgSystemCommandPreset.PresetId.OWOD, configUUID, config.getConfigLocale()),
                                        createButtonDefinition(RpgSystemCommandPreset.PresetId.SHADOWRUN_IMAGE, configUUID, config.getConfigLocale()),
                                        createButtonDefinition(RpgSystemCommandPreset.PresetId.DICE_CALCULATOR, configUUID, config.getConfigLocale())
                                )
                        )
                        .build())
                .build();
    }

    private ButtonDefinition createButtonDefinition(RpgSystemCommandPreset.PresetId presetId, UUID configUUID, Locale userLocal) {
        return ButtonDefinition.builder()
                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), presetId.name(), configUUID))
                .label(I18n.getMessage("rpg.system.command.preset.%s.name".formatted(presetId), userLocal))
                .build();
    }

    @Override
    protected @NonNull RollConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options, @NonNull Locale userLocale) {
        return new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale);
    }

}
