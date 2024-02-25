package de.janno.discord.bot.command.help;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
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

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class WelcomeCommand extends AbstractCommand<Config, StateData> {

    private static final String COMMAND_NAME = "welcome";
    private static final String CONFIG_TYPE_ID = "Config";
    private final Supplier<UUID> uuidSupplier;
    private final RpgSystemCommandPreset rpgSystemCommandPreset;

    public WelcomeCommand(PersistenceManager persistenceManager, RpgSystemCommandPreset rpgSystemCommandPreset) {
        this(persistenceManager, rpgSystemCommandPreset, UUID::randomUUID);
    }

    @VisibleForTesting
    public WelcomeCommand(PersistenceManager persistenceManager, RpgSystemCommandPreset rpgSystemCommandPreset, Supplier<UUID> uuidSupplier) {
        super(persistenceManager);
        this.uuidSupplier = uuidSupplier;
        this.rpgSystemCommandPreset = rpgSystemCommandPreset;
    }

    @Override
    protected ConfigAndState<Config, StateData> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                       @NonNull MessageDataDTO messageDataDTO,
                                                                                       @NonNull String buttonValue,
                                                                                       @NonNull String invokingUserName) {
        return deserializeAndUpdateState(messageConfigDTO, buttonValue);
    }

    ConfigAndState<Config, StateData> deserializeAndUpdateState(@NonNull MessageConfigDTO messageConfigDTO, @NonNull String buttonValue) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        final Config loadedConfig = Mapper.deserializeObject(messageConfigDTO.getConfig(), Config.class);
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
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, long guildId, long channelId, @NonNull Config config) {
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
                                                                                          @NonNull Config config,
                                                                                          @NonNull State<StateData> state,
                                                                                          long guildId,
                                                                                          long channelId) {
        BotMetrics.incrementButtonMetricCounter(COMMAND_NAME, "[" + state.getButtonValue() + "]");
        if (ButtonIds.isInvalid(state.getButtonValue())) {
            return Optional.empty();
        }
        UUID newConfigUUID = uuidSupplier.get();

        final RpgSystemCommandPreset.PresetId presetId = getPresetIdFromButton(state.getButtonValue());
        return Optional.of(rpgSystemCommandPreset.createMessage(presetId, newConfigUUID, guildId, channelId, config.getConfigLocale()));

    }

    private RpgSystemCommandPreset.PresetId getPresetIdFromButton(String buttonValue) {
        return switch (ButtonIds.valueOf(buttonValue)) {
            case fate -> RpgSystemCommandPreset.PresetId.FATE;
            case fate_image -> RpgSystemCommandPreset.PresetId.FATE_IMAGE;
            case dnd5 -> RpgSystemCommandPreset.PresetId.DND5;
            case dnd5_image -> RpgSystemCommandPreset.PresetId.DND5_IMAGE;
            case nWoD -> RpgSystemCommandPreset.PresetId.NWOD;
            case oWoD -> RpgSystemCommandPreset.PresetId.OWOD;
            case shadowrun -> RpgSystemCommandPreset.PresetId.SHADOWRUN;
            case coin -> RpgSystemCommandPreset.PresetId.COIN;
            case dice_calculator -> RpgSystemCommandPreset.PresetId.DICE_CALCULATOR;
        };
    }

    @Override
    protected boolean shouldKeepExistingButtonMessage(@NonNull ButtonEventAdaptor event) {
        return true;
    }


    @Override
    protected void addFurtherActions(List<Mono<Void>> actions, ButtonEventAdaptor event, Config config, State<StateData> state) {
        RpgSystemCommandPreset.PresetId presetId = getPresetIdFromButton(state.getButtonValue());
        String commandString = RpgSystemCommandPreset.getCommandString(presetId, event.getRequester().getUserLocal());
        actions.add(Mono.defer(() -> event.createMessageWithoutReference(EmbedOrMessageDefinition.builder()
                        .type(EmbedOrMessageDefinition.Type.MESSAGE)
                        .shortedContent("`%s`".formatted(commandString))
                        .build())).ofType(Void.class)
                .doOnSuccess(v ->
                        log.info("{}: Welcome Button {}",
                                event.getRequester().toLogString(),
                                presetId.name()
                        )));
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(Config config, State<StateData> state, long channelId, long userId) {
        return Optional.empty();
    }

    public Function<DiscordConnector.WelcomeRequest, EmbedOrMessageDefinition> getWelcomeMessage() {
        return request -> {
            Config config = new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), request.guildLocale());
            UUID configUUID = uuidSupplier.get();
            final Optional<MessageConfigDTO> newMessageConfig = createMessageConfig(configUUID, request.guildId(), request.channelId(), config);
            newMessageConfig.ifPresent(persistenceManager::saveMessageConfig);
            return createNewButtonMessage(configUUID, config, request.channelId());
        };
    }

    @Override
    protected Optional<ConfigAndState<Config, StateData>> createNewConfigAndStateIfMissing(String buttonValue) {
        return Optional.of(new ConfigAndState<>(uuidSupplier.get(), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), Locale.ENGLISH), new State<>(buttonValue, StateData.empty())));
    }

    @Override
    public @NonNull EmbedOrMessageDefinition createNewButtonMessage(@NonNull UUID configUUID, @NonNull Config config, long channelId) {
        return EmbedOrMessageDefinition.builder()
                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                .descriptionOrContent(I18n.getMessage("welcome.message", config.getConfigLocale()))
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .buttonDefinitions(
                                ImmutableList.of(
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.fate.name(), configUUID))
                                                .label(I18n.getMessage("rpg.system.command.preset.%s.name".formatted(ButtonIds.fate.name().toUpperCase()), config.getConfigLocale()))
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.fate_image.name(), configUUID))
                                                .label(I18n.getMessage("rpg.system.command.preset.%s.name".formatted(ButtonIds.fate_image.name().toUpperCase()), config.getConfigLocale()))
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.dnd5.name(), configUUID))
                                                .label(I18n.getMessage("rpg.system.command.preset.%s.name".formatted(ButtonIds.dnd5.name().toUpperCase()), config.getConfigLocale()))
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.dnd5_image.name(), configUUID))
                                                .label(I18n.getMessage("rpg.system.command.preset.%s.name".formatted(ButtonIds.dnd5_image.name().toUpperCase()), config.getConfigLocale()))
                                                .build()

                                )
                        )
                        .build())
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .buttonDefinitions(
                                ImmutableList.of(
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.nWoD.name(), configUUID))
                                                .label(I18n.getMessage("rpg.system.command.preset.%s.name".formatted(ButtonIds.nWoD.name().toUpperCase()), config.getConfigLocale()))
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.oWoD.name(), configUUID))
                                                .label(I18n.getMessage("rpg.system.command.preset.%s.name".formatted(ButtonIds.oWoD.name().toUpperCase()), config.getConfigLocale()))
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.shadowrun.name(), configUUID))
                                                .label(I18n.getMessage("rpg.system.command.preset.%s.name".formatted(ButtonIds.shadowrun.name().toUpperCase()), config.getConfigLocale()))
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.coin.name(), configUUID))
                                                .label(I18n.getMessage("rpg.system.command.preset.%s.name".formatted(ButtonIds.coin.name().toUpperCase()), config.getConfigLocale()))
                                                .build(),
                                        ButtonDefinition.builder()
                                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ButtonIds.dice_calculator.name(), configUUID))
                                                .label(I18n.getMessage("rpg.system.command.preset.%s.name".formatted(ButtonIds.dice_calculator.name().toUpperCase()), config.getConfigLocale()))
                                                .build()
                                )
                        )
                        .build())
                .build();
    }

    @Override
    protected @NonNull Config getConfigFromStartOptions(@NonNull CommandInteractionOption options, @NonNull Locale userLocale) {
        return new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale);
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
