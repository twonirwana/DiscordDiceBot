package de.janno.discord.bot.command.help;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
public class WelcomeCommandMockTest {

    PersistenceManager persistenceManager;
    private Expect expect;

    @BeforeEach
    void setup() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void slashStartWelcome_firstButton() {
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0);

        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand);

        WelcomeCommand underTest = new WelcomeCommand(persistenceManager, rpgSystemCommandPreset, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000001"), Locale.ENGLISH).block();

        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "reply: commandString",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Welcome to the Button Dice Bot,\nuse one of the example buttons below to start one of the RPG dice systems, use `/quickstart system` to select one of many RPG presets or use the slash command to configure your own custom dice system (see https://github.com/twonirwana/DiscordDiceBot for details or the slash command `/help`).\nYou can also use the slash command `/r` to directly roll dice with.\nFor help or feature request come to the support discord server: https://discord.gg/e43BsqKpFr, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dungeon & Dragons 5e, id=welcome\u001Ednd5_image\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false), ButtonDefinition(label=Dungeon & Dragons 5e without Dice Images, id=welcome\u001Ednd5\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false), ButtonDefinition(label=Fate, id=welcome\u001Efate_image\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false), ButtonDefinition(label=Coin Toss, id=welcome\u001Ecoin\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=nWod / Chronicles of Darkness, id=welcome\u001EnWoD\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false), ButtonDefinition(label=oWod / Storyteller System, id=welcome\u001EoWoD\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false), ButtonDefinition(label=Shadowrun, id=welcome\u001Eshadowrun\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false), ButtonDefinition(label=Dice Calculator, id=welcome\u001Edice_calculator\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );

        Optional<ButtonEventAdaptorMock> buttonEventAdaptorMock = slashEvent.getFirstButtonEventMockOfLastButtonMessage();
        assertThat(buttonEventAdaptorMock).isPresent();
        underTest.handleComponentInteractEvent(buttonEventAdaptorMock.get()).block();
        assertThat(buttonEventAdaptorMock.get().getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Welcome to the Button Dice Bot,\nuse one of the example buttons below to start one of the RPG dice systems, use `/quickstart system` to select one of many RPG presets or use the slash command to configure your own custom dice system (see https://github.com/twonirwana/DiscordDiceBot for details or the slash command `/help`).\nYou can also use the slash command `/r` to directly roll dice with.\nFor help or feature request come to the support discord server: https://discord.gg/e43BsqKpFr, buttonValues=dnd5_image,dnd5,fate_image,coin,nWoD,oWoD,shadowrun,dice_calculator",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=`/custom_dice start buttons: D20;2d20k1@D20 Advantage;2d20L1@D20 Disadvantage;;1d4;1d6;1d8;1d10;1d12;1d100;2d4=@2d4;2d6=@2d6;2d8=@2d8;2d10=@2d10;2d12=@2d12;2d20=@2d20 answer_format: without_expression dice_image_style: polyhedral_RdD dice_image_color: default`, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE)",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=D20, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=D20 Advantage, id=custom_dice2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=D20 Disadvantage, id=custom_dice3_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d4, id=custom_dice4_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=1d6, id=custom_dice5_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=1d8, id=custom_dice6_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=1d10, id=custom_dice7_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=1d12, id=custom_dice8_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d100, id=custom_dice9_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d4, id=custom_dice10_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d6, id=custom_dice11_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d8, id=custom_dice12_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d10, id=custom_dice13_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=2d12, id=custom_dice14_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d20, id=custom_dice15_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"

        );
    }

    @Test
    void slashStartWelcome_invalidId() {
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0);

        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand);

        WelcomeCommand underTest = new WelcomeCommand(persistenceManager, rpgSystemCommandPreset, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000001"), Locale.ENGLISH).block();

        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "reply: commandString",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Welcome to the Button Dice Bot,\nuse one of the example buttons below to start one of the RPG dice systems, use `/quickstart system` to select one of many RPG presets or use the slash command to configure your own custom dice system (see https://github.com/twonirwana/DiscordDiceBot for details or the slash command `/help`).\nYou can also use the slash command `/r` to directly roll dice with.\nFor help or feature request come to the support discord server: https://discord.gg/e43BsqKpFr, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dungeon & Dragons 5e, id=welcome\u001Ednd5_image\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false), ButtonDefinition(label=Dungeon & Dragons 5e without Dice Images, id=welcome\u001Ednd5\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false), ButtonDefinition(label=Fate, id=welcome\u001Efate_image\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false), ButtonDefinition(label=Coin Toss, id=welcome\u001Ecoin\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=nWod / Chronicles of Darkness, id=welcome\u001EnWoD\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false), ButtonDefinition(label=oWod / Storyteller System, id=welcome\u001EoWoD\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false), ButtonDefinition(label=Shadowrun, id=welcome\u001Eshadowrun\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false), ButtonDefinition(label=Dice Calculator, id=welcome\u001Edice_calculator\u001E00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );

        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("welcome", "fate", new AtomicLong(0));
        underTest.handleComponentInteractEvent(buttonEvent).block();
        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Welcome to the Button Dice Bot,\nuse one of the example buttons below to start one of the RPG dice systems, use `/quickstart system` to select one of many RPG presets or use the slash command to configure your own custom dice system (see https://github.com/twonirwana/DiscordDiceBot for details or the slash command `/help`).\nYou can also use the slash command `/r` to directly roll dice with.\nFor help or feature request come to the support discord server: https://discord.gg/e43BsqKpFr, buttonValues=dnd5_image,dnd5,fate_image,coin,nWoD,oWoD,shadowrun,dice_calculator"
        );
    }


}
