package de.janno.discord.bot.command.help;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
public class WelcomeCommand extends AbstractCommand<Config, StateData> {

    //todo i18n
    public static final String COMMAND_NAME = "welcome";
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
        //todo i18n
        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), Locale.ENGLISH), new State<>(buttonValue, StateData.empty()));
    }

    @Override
    protected @NonNull Optional<MessageConfigDTO> getMessageConfigDTO(@Nullable UUID configId, long channelId, long messageId) {
        return Optional.of(new MessageConfigDTO(uuidSupplier.get(), null, channelId, getCommandId(), "None", "None"));
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
    public @NonNull String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage(Locale userLocale) {
        //todo i18n
        return EmbedOrMessageDefinition.builder().descriptionOrContent("Displays the welcome message")
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server for Help and News", "https://discord.gg/e43BsqKpFr", false))
                .build();
    }

    @Override
    protected @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessageWithState(@NonNull UUID configUUID, @NonNull Config ignore, @NonNull State<StateData> state, long guildId, long channelId) {
        BotMetrics.incrementButtonMetricCounter(COMMAND_NAME, "[" + state.getButtonValue() + "]");
        if (ButtonIds.isInvalid(state.getButtonValue())) {
            return Optional.empty();
        }
        UUID newConfigUUID = uuidSupplier.get();
        log.info("Click on welcome command creation: " + state.getButtonValue());
        //todo i18n
        //change locale
        return switch (ButtonIds.valueOf(state.getButtonValue())) {
            case fate ->
                    Optional.of(rpgSystemCommandPreset.createMessage(RpgSystemCommandPreset.PresetId.FATE, newConfigUUID, guildId, channelId).getMessageDefinition());
            case fate_image ->
                    Optional.of(rpgSystemCommandPreset.createMessage(RpgSystemCommandPreset.PresetId.FATE_IMAGE, newConfigUUID, guildId, channelId).getMessageDefinition());
            case dnd5 ->
                    Optional.of(rpgSystemCommandPreset.createMessage(RpgSystemCommandPreset.PresetId.DND5, newConfigUUID, guildId, channelId).getMessageDefinition());
            case dnd5_image ->
                    Optional.of(rpgSystemCommandPreset.createMessage(RpgSystemCommandPreset.PresetId.DND5_IMAGE, newConfigUUID, guildId, channelId).getMessageDefinition());
            case nWoD ->
                    Optional.of(rpgSystemCommandPreset.createMessage(RpgSystemCommandPreset.PresetId.NWOD, newConfigUUID, guildId, channelId).getMessageDefinition());
            case oWoD ->
                    Optional.of(rpgSystemCommandPreset.createMessage(RpgSystemCommandPreset.PresetId.OWOD, newConfigUUID, guildId, channelId).getMessageDefinition());
            case shadowrun ->
                    Optional.of(rpgSystemCommandPreset.createMessage(RpgSystemCommandPreset.PresetId.SHADOWRUN, newConfigUUID, guildId, channelId).getMessageDefinition());
            case coin ->
                    Optional.of(rpgSystemCommandPreset.createMessage(RpgSystemCommandPreset.PresetId.COIN, newConfigUUID, guildId, channelId).getMessageDefinition());
            case dice_calculator ->
                    Optional.of(rpgSystemCommandPreset.createMessage(RpgSystemCommandPreset.PresetId.DICE_CALCULATOR, newConfigUUID, guildId, channelId).getMessageDefinition());
        };
    }

    @Override
    protected boolean shouldKeepExistingButtonMessage(@NonNull ButtonEventAdaptor event) {
        return true;
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(Config config, State<StateData> state, long channelId, long userId) {
        return Optional.empty();
    }

    public EmbedOrMessageDefinition getWelcomeMessage() {
        //todo null config
        return createNewButtonMessage(uuidSupplier.get(), null);
    }

    @Override
    public @NonNull EmbedOrMessageDefinition createNewButtonMessage(@NonNull UUID configUUID,  Config config) {
        return EmbedOrMessageDefinition.builder()
                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                //todo i18n
                .descriptionOrContent("""
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
