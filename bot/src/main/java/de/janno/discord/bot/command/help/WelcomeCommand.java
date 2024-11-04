package de.janno.discord.bot.command.help;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
public class WelcomeCommand extends ComponentCommandImpl<RollConfig, StateData> {

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
    public @NonNull String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    public @NonNull MessageDataDTO createEmptyMessageData(@NonNull UUID configUUID, @Nullable Long guildId, long channelId, long messageId) {
        //there should be no saved message data for the welcome message, the created new button messages will create their own message data upon first interaction
        return new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), Mapper.NO_PERSISTED_STATE, null);
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
    protected @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessage(@NonNull UUID configId, @NonNull RollConfig config, @Nullable State<StateData> state, @Nullable Long guildId, long channelId, long userId) {
        if (state == null) {
            return Optional.empty();
        }

        BotMetrics.incrementButtonMetricCounter(COMMAND_NAME);
        return getPresetIdFromButton(state.getButtonValue())
                .flatMap(id -> rpgSystemCommandPreset.createMessage(id, uuidSupplier.get(), guildId, channelId, -1, config.getConfigLocale()));

    }


    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(RollConfig config, State<StateData> state, long channelId, long userId) {
        return Optional.empty();
    }

    @Override
    protected Optional<ConfigAndState<RollConfig, StateData>> createNewConfigAndStateIfMissing(String buttonValue) {
        return Optional.of(new ConfigAndState<>(uuidSupplier.get(), new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), Locale.ENGLISH, null, null), new State<>(buttonValue, StateData.empty())));
    }

}
