package de.janno.discord.bot.command.help;

import com.google.common.base.Strings;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.dice.image.provider.D6Dotted;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


public class RpgSystemCommandPreset {


    private final PersistenceManager persistenceManager;
    private final CustomParameterCommand customParameterCommand;
    private final CustomDiceCommand customDiceCommand;
    private final SumCustomSetCommand sumCustomSetCommand;

    public RpgSystemCommandPreset(PersistenceManager persistenceManager,
                                  CustomParameterCommand customParameterCommand,
                                  CustomDiceCommand customDiceCommand,
                                  SumCustomSetCommand sumCustomSetCommand) {
        this.persistenceManager = persistenceManager;
        this.customParameterCommand = customParameterCommand;
        this.customDiceCommand = customDiceCommand;
        this.sumCustomSetCommand = sumCustomSetCommand;
    }

    private static List<ButtonIdLabelAndDiceExpression> string2ButtonIdLabelAndDiceExpression(String buttons) {
        AtomicLong buttonIdCounter = new AtomicLong(1);
        return Arrays.stream(buttons.split(";"))
                .map(s -> {
                    if (s.contains("@")) {
                        String[] expressionLabel = s.split("@");
                        return new ButtonIdLabelAndDiceExpression(buttonIdCounter.getAndIncrement() + "_button", expressionLabel[1].trim(), expressionLabel[0].trim());
                    }
                    return new ButtonIdLabelAndDiceExpression(buttonIdCounter.getAndIncrement() + "_button", s.trim(), s.trim());

                })
                .collect(Collectors.toList());
    }

    public CommandAndMessageDefinition createMessage(PresetId presetId, UUID newConfigUUID, long guildId, long channelId, Locale userLocale) {
        return switch (presetId) {
            //custom_parameter start expression: replace(4d[＋,▢,−],'＋',1,'▢',0,'−',-1)+{Modifier:-4<=>10}=
            case FATE -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.FATE.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //4d[-1,0,1]+{Modifier:-4<=>10}=
            case FATE_IMAGE -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.FATE_IMAGE.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.fate, DiceImageStyle.fate.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //1d4;1d6;1d8;1d10;1d12;1d20;1d100;2d20k1@D20 Advantage;2d20L1@D20 Disadvantage;2d4=@2d4;2d6=@2d6;2d8=@2d8;2d10=@2d10;2d12=@2d12;2d20=@2d20
            case DND5 -> startPreset(
                    new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.DND5.expression", userLocale)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale)
                    , customDiceCommand, newConfigUUID, guildId, channelId);
            case DND5_IMAGE -> startPreset(
                    new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.DND5_IMAGE.expression", userLocale)),
                            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale)
                    , customDiceCommand, newConfigUUID, guildId, channelId);
            //custom_parameter start expression: {Number of Dice}d!10>=8c
            case NWOD -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.NWOD.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //custom_parameter start expression:val('$diceNumber',{Number of Dice}) val('$target', {Target Number:2<=>10}) val('$reroll', {Reroll on 10:0@No/1@Yes}) val('$roll', if('$reroll'=?0, '$diceNumber'd10,'$diceNumber'd!10)) ('$roll'>='$target'c) - ('$roll'==1c)=
            case OWOD -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.OWOD.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //val('$roll',{number of dice:1<=>20}d6) concat('$roll'>4c, if('$roll'==1c >? '$roll'c/2,' - Glitch!'))
            case SHADOWRUN -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.SHADOWRUN.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //val('$roll',{number of dice:1<=>20}d6) concat('$roll'>4c, if('$roll'==1c >? '$roll'c/2,' - Glitch!'))
            case SHADOWRUN_IMAGE ->
                    startPreset(new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.SHADOWRUN_IMAGE.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, D6Dotted.BLACK_AND_GOLD), userLocale), customParameterCommand, newConfigUUID, guildId, channelId);
            case COIN -> startPreset(
                    new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.COIN.expression", userLocale)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale)
                    , customDiceCommand, newConfigUUID, guildId, channelId);
            //sum_custom_set start buttons: 7;8;9;+;-;4;5;6;d;k;1;2;3;0;l always_sum_result: true
            case DICE_CALCULATOR ->
                    startPreset(new SumCustomSetConfig(null, string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.DICE_CALCULATOR.expression", userLocale)),
                            DiceParserSystem.DICE_EVALUATOR, true, false, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale), sumCustomSetCommand, newConfigUUID, guildId, channelId);
            //1d20@D20;1d6@D6;2d6@2D6;1d4@D4;1d8@D8;6x3d6=@Stats;(3d6=)*10@Gold;1d100@D100;1d10@D10;1d12@D12
            case OSR -> startPreset(
                    new CustomDiceConfig(null,
                            string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.OSR.expression", userLocale)),
                            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , customDiceCommand, newConfigUUID, guildId, channelId);
            //sum_custom_set start buttons:+2d6;+(3d6k2)@Boon;+(3d6l2)@Bane;+1d6;+1;+2;+3;+4;-1;-2;-3;-4
            case TRAVELLER -> startPreset(
                    new SumCustomSetConfig(null,
                            string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.TRAVELLER.expression", userLocale)),
                            DiceParserSystem.DICE_EVALUATOR, true, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , sumCustomSetCommand, newConfigUUID, guildId, channelId);
            //custom_parameter start expression: (d!!{Dice:4@D4/6@D6/8@D8/12@D12/20@D20} + {Type: 0@Regular/1d!!6@Wildcard})k1
            case SAVAGE_WORLDS -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.SAVAGE_WORLDS.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            // custom_dice start buttons: ifE(2d[0/0/0/1/1/3]l1,3,'Success',1,'Partial','Failure')@Zero;ifE(1d[0/0/0/1/1/3],3,'Success',1,'Partial','Failure')@1d6;ifG(2d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@2d6;ifG(3d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@3d6;ifG(4d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@4d6;ifG(5d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@5d6;ifG(6d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@6d6;ifG(7d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@7d6
            case BLADES_IN_THE_DARK -> startPreset(
                    new CustomDiceConfig(null,
                            string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.BLADES_IN_THE_DARK.expression", userLocale)),
                            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale)
                    , customDiceCommand, newConfigUUID, guildId, channelId);
            //7th Edition Call of Cthulhu: custom_dice start buttons:  1d100; 2d100L1@1d100 Advantage; 2d100K1@1d100 Penalty; 1d3; 1d4; 1d6; 1d8; 1d10; 1d12; 1d20; 3d6
            case CALL_OF_CTHULHU_7ED ->
                    startPreset(new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.CALL_OF_CTHULHU_7ED.expression", userLocale)),
                            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale), customDiceCommand, newConfigUUID, guildId, channelId);
            //Exalted 3rd /custom_parameter start expression: val('$1', cancel(double({number of dice}d10,10),1,[7/8/9/10])), ifE(('$1'>=7)c,0,ifG(('$1'<=1)c,0,'Botch'))
            case EXALTED_3ED -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.EXALTED_3ED.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //Vampire 5ed /custom_parameter start expression: val('$r',{regular dice:1<=>16}d10 col 'blue') val('$h',{hunger dice:0<=>5}d10 col 'purple_dark') val('$s',('$r'+'$h')>=6c) val('$rt','$r'==10c) val('$ht','$h'==10c) val('$ho','$h'==1c) val('$2s',((('$rt'+'$ht'=))/2)*2) val('$ts',('$s'+'$2s'=)) concat('successes: ', '$ts', ifE('$ts',0,ifG('$ho',1,' bestial failure' , ''),''), ifE('$rt' mod 2, 1, ifE('$ht' mod 2, 1, ' messy critical', ''), '')) answer_format: without_expression
            case VAMPIRE_5ED -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.VAMPIRE_5ED.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //Hunter 5ed /custom_parameter start expression: val('$r',{Regular D10 Dice:1<=>16}d10 col 'blue') val('$h', {Desperation Dice:0<=>5}d10 col'purple_dark') val('$s',('$r'+'$h')>=6c) val('$rt','$r'==10c) val('$ht','$h'==10c) val('$ho','$h'==1c) val('$2s',((('$rt'+'$ht'=))/2)*2) val('$ts',('$s'+'$2s'=)) concat('successes: ', '$ts', ifE('$ts',0,ifG('$ho',1,' bestial failure' , ''),''), ifE('$rt' mod 2, 1, ifE('$ht' mod 2, 1, ' messy critical', ''), '')) answer_format: without_expression dice_image_style: polyhedral_knots
            case HUNTER_5ED -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.HUNTER_5ED.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //One-Roll Engine /custom_parameter start expression: groupc({Number of Dice:1<=>10}d10+({Number of Extra Die:0@0/10@1/2r10@2/3r10@3/4r10@4})>={Difficulty:1<=>10})
            case ONE_ROLL_ENGINE -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.ONE_ROLL_ENGINE.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //Dungeon Crawl Classics  /sum_custom_set start buttons: 1d4;1d6;1d7;1d8;1d10;1d12;1d14;1d16;1d20;1d24;1d16;1d30;1d100;+1;+2;+3;+4;+5;-1;-2;-3;-4;-5
            case DUNGEON_CRAWL_CLASSICS -> startPreset(
                    new SumCustomSetConfig(null, string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.DUNGEON_CRAWL_CLASSICS.expression", userLocale)),
                            DiceParserSystem.DICE_EVALUATOR, true, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale)
                    , sumCustomSetCommand, newConfigUUID, guildId, channelId);
            // Tiny D6  /custom_dice start buttons: ifG(1d6>=5c,0,'Success','Failure')@Disadvantage; ifG(2d6>=5c,0,'Success','Failure')@Test;ifG(3d6>=5c,0,'Success','Failure')@Advantage answer_format: without_expression
            case TINY_D6 -> startPreset(
                    new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.TINY_D6.expression", userLocale)),
                            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , customDiceCommand, newConfigUUID, guildId, channelId);
            //Cyberpunk Red /custom_parameter start expression: <val('$roll', 1d10) ifE('$roll', 1, '$roll'-1d10, 10, '$roll'+1d10, '$roll')+{ability:0<=>9}+{skill:0<=>9}=>
            case CYBERPUNK_RED -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.CYBERPUNK_RED.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //A Song of Ice and Fire /custom_parameter start expression: {numberOfDice:1<=>15}d6k{keep:1<=>10}
            case ASOIAF -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.ASOIAF.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //City of mist /custom_parameter start expression: 2d6+{modifier:-6<=>6}
            case CITY_OF_MIST -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.CITY_OF_MIST.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //Risus The Anything RPG "Evens Up" optional rule /custom_parameter start expression: val('$r',{numberOfDice:1<=>10}d!6) ('$r'==2c) + ('$r'==4c) + ('$r'==6c)=
            case RISUS -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.RISUS.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //Kids on Brooms: /custom_parameter start expression: (d!!{Dice:4@D4/6@D6/8@D8/12@D12/20@D20/4+d!!4@D4Spell/6+d!!4@D6Spell/8+d!!4@D8Spell/12+d!!4@D12Spell/20+d!!4@D20Spell} {plus or minus:+/-}{Modifier:0<=>15}=)-{Difficulty:1<=>30}=
            case KIDS_ON_BROOMS -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.KIDS_ON_BROOMS.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //Alternate 5e Calculating setup: /sum_custom_set start buttons: d4;d6;d8;d10;d12;d20;d100;1;2;3;4;5;k@Keep Highest;L@Keep Lowest;(2d20k1)@D20 Advantage;(2d20L1)@D20 Disadvantage;-@Minus;+@Plus;4d6k3=@Stats;,@Split;[Muliple dice can be rolled using Number then die type. Plus Minus can be used to add modifiers If adding a modifier to an Advantage or disadvantage roll those buttons must be used. Keep Highest Lowest only work correctly with multiple dice of the same type. If you want to roll dice added together use Plus between each die type. This is a work around to give some guidance. Bot is not supposed to work this way.]@Help always_sum_result: true answer_format: full dice_image_style: polyhedral_alies_v2 dice_image_color: orange_and_silver
            case DND5_CALC -> startPreset(
                    new SumCustomSetConfig(null, string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.DND5_CALC.expression", userLocale)), DiceParserSystem.DICE_EVALUATOR, true, false, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , sumCustomSetCommand, newConfigUUID, guildId, channelId);
            // Rêve de Dragon /custom_dice start buttons: 1d4@D4;1d6@D6;2d6=@2D6;1d7@D7;1d8@D8;val('roll',1d!8 col 'special') val('diceCount','roll' c) 'roll'-'diceCount'+7=@DDR;2d10=@2D10;1d12@D12;val('$r',1d12 col 'special'),if('$r'=?1,'vaisseau','$r'=?2,'sirène','$r'=?3,'faucon','$r'=?4,'couronne','$r'=?5,'dragon','$r'=?6,'épées','$r'=?7,'lyre','$r'=?8,'serpent','$r'=?9,'poisson acrobate','$r'=?10,'araignée','$r'=?11,'roseaux','$r'=?12,'château dormant')@DAS;1d20@D20;1d100@D100 answer_format: without_expression dice_image_style: polyhedral_RdD
            case REVE_DE_DRAGON -> startPreset
                    (new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.REVE_DE_DRAGON.expression", userLocale)),
                                    DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale),
                            customDiceCommand, newConfigUUID, guildId, channelId);
            //Public Access (Carved from Brindlewood): /custom_dice start buttons: 2d6=@Roll;3d6k2=@Advantage;3d6l2=@Disadvantage answer_format: full dice_image_style: polyhedral_3d dice_image_color: red_and_white
            case PUBLIC_ACCESS -> startPreset(
                    new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.PUBLIC_ACCESS.expression", userLocale)),
                            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale)
                    , customDiceCommand, newConfigUUID, guildId, channelId);
            //custom_dice start buttons:asc((0d6 > 4) col 'black' + replace(1d6 > 4, [6], 'Computer!')) @ 0d6 + Computer; asc((1d6 > 4) col 'black' + replace(1d6 > 4, [6], 'Computer!')) @ 1d6 + Computer; asc((2d6 > 4) col 'red' + replace(1d6 > 4, [6], 'Computer!')) @ 2d6 + Computer; asc((3d6 > 4) col 'orange' + replace(1d6 > 4, [6], 'Computer!')) @ 3d6 + Computer; asc((4d6 > 4) col 'yellow' + replace(1d6 > 4, [6], 'Computer!')) @ 4d6 + Computer; asc((5d6 > 4) col 'green' + replace(1d6 > 4, [6], 'Computer!')) @ 5d6 + Computer; asc((6d6 > 4) col 'cyan' + replace(1d6 > 4, [6], 'Computer!')) @ 6d6 + Computer; asc((7d6 > 4) col 'blue' + replace(1d6 > 4, [6], 'Computer!')) @ 7d6 + Computer; asc((8d6 > 4) col 'magenta' + replace(1d6 > 4, [6], 'Computer!')) @ 8d6 + Computer; asc((9d6 > 4) col 'white' + replace(1d6 > 4, [6], 'Computer!')) @ 9d6 + Computer dice_image_style:polyhedral_2d dice_image_color:gray
            case PARANOIA -> startPreset(
                    new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.PARANOIA.expression", userLocale)),
                            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, "grey"), userLocale)
                    , customDiceCommand, newConfigUUID, guildId, channelId);
            //Candela Obscura (https://darringtonpress.com/candela/)
            ///sum_custom_set start buttons: +2d6l1 col 'blue'@None;+1d6@1;+2d6@2;+3d6@3;+4d6@4;+5d6@5;+6d6@6;+1d6 col  'purple_white'@:star2: Add Gilded? always_sum_result: false answer_format: without_expression dice_image_style: polyhedral_knots
            case CANDELA_OBSCURA -> startPreset(
                    new SumCustomSetConfig(null, string2ButtonIdLabelAndDiceExpression(I18n.getMessage("rpg.system.command.preset.CANDELA_OBSCURA.expression", userLocale)), DiceParserSystem.DICE_EVALUATOR, false, true, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()), userLocale)
                    , sumCustomSetCommand, newConfigUUID, guildId, channelId);
            //Prowlers & Paragons Ultimate Edition (https://www.drivethrurpg.com/product/346742/Prowlers--Paragons-Ultimate-Edition)
            ///custom_parameter start expression: val('$r',{number of dice:1<=>12}d6),
            //val('$total',replace('$r', [1/3/5], 0, [2/4], 1, [6], 2)=), '$total'_' successes' dice_image_style: polyhedral_alies_v1
            case PROWLERS_PARAGONS -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.PROWLERS_PARAGONS.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, DiceImageStyle.polyhedral_alies_v1.getDefaultColor()), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
            //Bluebeard's Bride (https://www.drivethrurpg.com/product/224782/Bluebeards-Bride)
            ///custom_parameter start expression: val('$roll',(2d6))
            //val('$mod',{Modifier:-1<=>1})
            //val('$total',('$roll'+'$mod')=) if('$total'>=?10, 'Hit', '$total'<=?6, 'Miss', 'Mitigated Hit') answer_format: without_expression dice_image_style: polyhedral_alies_v2 dice_image_color: blue_and_gold
            case BLUEBEARD_BRIDE -> startPreset(
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.BLUEBEARD_BRIDE.expression", userLocale), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_gold"), userLocale)
                    , customParameterCommand, newConfigUUID, guildId, channelId);
        };
    }

    private <C extends Config> CommandAndMessageDefinition startPreset(C config, AbstractCommand<C, ?> command, UUID newConfigUUID, long guildId, long channelId) {
        String commandString = "/%s %s".formatted(command.getCommandId(), config.toCommandOptionsString());
        command.createMessageConfig(newConfigUUID, guildId, channelId, config).ifPresent(persistenceManager::saveMessageConfig);
        return new CommandAndMessageDefinition(commandString, command.createNewButtonMessage(newConfigUUID, config));
    }

    @AllArgsConstructor
    public enum PresetId {
        DND5_IMAGE,
        DND5,
        DND5_CALC,
        NWOD,
        OWOD,
        SHADOWRUN,
        SHADOWRUN_IMAGE,
        SAVAGE_WORLDS,
        FATE_IMAGE,
        FATE,
        COIN,
        DICE_CALCULATOR,
        OSR,
        TRAVELLER,
        BLADES_IN_THE_DARK,
        CALL_OF_CTHULHU_7ED,
        EXALTED_3ED,
        VAMPIRE_5ED,
        HUNTER_5ED,
        ONE_ROLL_ENGINE,
        DUNGEON_CRAWL_CLASSICS,
        TINY_D6,
        CYBERPUNK_RED,
        ASOIAF,
        CITY_OF_MIST,
        RISUS,
        KIDS_ON_BROOMS,
        REVE_DE_DRAGON,
        PARANOIA,
        PUBLIC_ACCESS,
        CANDELA_OBSCURA,
        PROWLERS_PARAGONS,
        BLUEBEARD_BRIDE;

        public String getName(Locale locale) {
            return I18n.getMessage("rpg.system.command.preset.%s.name".formatted(name()), locale);
        }

        public List<String> getSynonymes(Locale locale) {
            String aliasString = I18n.getMessage("rpg.system.command.preset.%s.aliases".formatted(name()), locale);
            if (Strings.isNullOrEmpty(aliasString)) {
                return List.of();
            }
            if (aliasString.contains(";")) {
                return List.of(aliasString.split(";"));
            }
            return List.of(aliasString);
        }
    }

    @Value
    public static class CommandAndMessageDefinition {
        @NonNull
        String command;
        @NonNull
        EmbedOrMessageDefinition messageDefinition;
    }
}
