package de.janno.discord.bot.command.help;

import com.google.common.base.Strings;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.command.channelConfig.AliasConfig;
import de.janno.discord.bot.command.channelConfig.AliasHelper;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.dice.image.provider.D6Dotted;
import de.janno.discord.bot.dice.image.provider.D6MarvelV2;
import de.janno.discord.bot.dice.image.provider.PolyhedralSvgWithColor;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;


import java.util.*;

@RequiredArgsConstructor
public class RpgSystemCommandPreset {
    private final PersistenceManager persistenceManager;
    private final CustomParameterCommand customParameterCommand;
    private final CustomDiceCommand customDiceCommand;
    private final SumCustomSetCommand sumCustomSetCommand;
    private final ChannelConfigCommand channelConfigCommand;

    public static Config createConfig(PresetId presetId, Locale userLocale) {
        return switch (presetId) {
            //custom_parameter start expression: replace(4d[＋,▢,−],'＋',1,'▢',0,'−',-1)+{Modifier:-4<=>10}=
            case FATE ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.FATE.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.FATE.name", userLocale), CustomParameterConfig.InputType.button);
            //4d[-1,0,1]+{Modifier:-4<=>10}=
            case FATE_IMAGE ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.FATE_IMAGE.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.fate, DiceImageStyle.fate.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.FATE_IMAGE.name", userLocale), CustomParameterConfig.InputType.button);
            //1d4;1d6;1d8;1d10;1d12;1d20;1d100;2d20k1@D20 Advantage;2d20L1@D20 Disadvantage;2d4=@2d4;2d6=@2d6;2d8=@2d8;2d10=@2d10;2d12=@2d12;2d20=@2d20
            case DND5 ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.DND5.expression", userLocale)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.DND5.name", userLocale));
            case DND5_IMAGE ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.DND5_IMAGE.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.DND5_IMAGE.name", userLocale));
            //custom_parameter start expression: {Number of Dice}d!10>=8c
            case NWOD ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.NWOD.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.NWOD.name", userLocale), CustomParameterConfig.InputType.button);
            //custom_parameter start expression:val('$diceNumber',{Number of Dice}) val('$target', {Target Number:2<=>10}) val('$reroll', {Reroll on 10:0@No/1@Yes}) val('$roll', if('$reroll'=?0, '$diceNumber'd10,'$diceNumber'd!10)) ('$roll'>='$target'c) - ('$roll'==1c)=
            case OWOD ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.OWOD.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.OWOD.name", userLocale), CustomParameterConfig.InputType.button);
            //val('$roll',{number of dice:1<=>20}d6) concat('$roll'>4c, if('$roll'==1c >? '$roll'c/2,' - Glitch!'))
            case SHADOWRUN ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.SHADOWRUN.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.SHADOWRUN.name", userLocale), CustomParameterConfig.InputType.button);
            //val('$roll',{number of dice:1<=>20}d6) concat('$roll'>4c, if('$roll'==1c >? '$roll'c/2,' - Glitch!'))
            case SHADOWRUN_IMAGE ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.SHADOWRUN_IMAGE.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, D6Dotted.BLACK_AND_GOLD), userLocale, null, I18n.getMessage("rpg.system.command.preset.SHADOWRUN_IMAGE.name", userLocale), CustomParameterConfig.InputType.button);
            case COIN ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.COIN.expression", userLocale)), AnswerFormatType.only_result, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.COIN.name", userLocale));
            //sum_custom_set start buttons: 7;8;9;+;-;4;5;6;d;k;1;2;3;0;l always_sum_result: true
            case DICE_CALCULATOR -> new SumCustomSetConfig(null,
                    ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.DICE_CALCULATOR.expression", userLocale)),
                    true, false, false, null, null, AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.DICE_CALCULATOR.name", userLocale));
            //1d20@D20;1d6@D6;2d6@2D6;1d4@D4;1d8@D8;6x3d6=@Stats;(3d6=)*10@Gold;1d100@D100;1d10@D10;1d12@D12
            case OSR ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.OSR.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.OSR.name", userLocale));
            //sum_custom_set start buttons:+2d6;+(3d6k2)@Boon;+(3d6l2)@Bane;+1d6;+1;+2;+3;+4;-1;-2;-3;-4
            case TRAVELLER -> new SumCustomSetConfig(null,
                    ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.TRAVELLER.expression", userLocale)),
                    true, true, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.TRAVELLER.name", userLocale));
            //custom_parameter start expression: (d!!{Dice:4@D4/6@D6/8@D8/12@D12/20@D20} + {Type: 0@Regular/1d!!6@Wildcard})k1
            case SAVAGE_WORLDS ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.SAVAGE_WORLDS.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.SAVAGE_WORLDS.name", userLocale), CustomParameterConfig.InputType.button);
            // val('$diceRoll',{Number of Dice:2d6L1@No Dice/1d6@1 Dice/2d6@2 Dice/3d6@3 Dice/4d6@4 Dice/5d6@5 Dice/6d6@6 Dice/7d6@7 Dice}) val('$sixes','$diceRoll'==6c) val('$partials','$diceRoll'>3<6c)  if('$sixes'>?1,'Critical Success - You do it with increased effect.', '$sixes'=?1,'Success - You do it.','$partials' >? 0,'Partial Success - You do it but suffer severe harm, a serious complication or have reduced effect.','Failure - You suffer severe harm, a serious complication occurs, or you lose this opportunity for action.'
            case BLADES_IN_THE_DARK ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.BLADES_IN_THE_DARK.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.BLADES_IN_THE_DARK.name", userLocale), CustomParameterConfig.InputType.button);
            case BLADES_IN_THE_DARK_IMAGE ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.BLADES_IN_THE_DARK.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.BLADES_IN_THE_DARK_IMAGE.name", userLocale), CustomParameterConfig.InputType.button);
            case BLADES_IN_THE_DARK_DETAIL ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.BLADES_IN_THE_DARK_DETAIL.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.BLADES_IN_THE_DARK_DETAIL.name", userLocale), CustomParameterConfig.InputType.button);
            //7th Edition Call of Cthulhu: custom_dice start buttons:  1d100; 2d100L1@1d100 Advantage; 2d100K1@1d100 Penalty; 1d3; 1d4; 1d6; 1d8; 1d10; 1d12; 1d20; 3d6
            case CALL_OF_CTHULHU_7ED ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.CALL_OF_CTHULHU_7ED.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.CALL_OF_CTHULHU_7ED.name", userLocale));
            //Exalted 3rd /custom_parameter start expression: val('$1', cancel(double({number of dice}d10,10),1,[7/8/9/10])), ifE(('$1'>=7)c,0,ifG(('$1'<=1)c,0,'Botch'))
            case EXALTED_3ED ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.EXALTED_3ED.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.EXALTED_3ED.name", userLocale), CustomParameterConfig.InputType.button);
            //Vampire 5ed /custom_parameter start expression: val('$r',{regular dice:1<=>16}d10 col 'blue') val('$h',{hunger dice:0<=>5}d10 col 'purple_dark') val('$s',('$r'+'$h')>=6c) val('$rt','$r'==10c) val('$ht','$h'==10c) val('$ho','$h'==1c) val('$2s',((('$rt'+'$ht'=))/2)*2) val('$ts',('$s'+'$2s'=)) concat('successes: ', '$ts', ifE('$ts',0,ifG('$ho',1,' bestial failure' , ''),''), ifE('$rt' mod 2, 1, ifE('$ht' mod 2, 1, ' messy critical', ''), '')) answer_format: without_expression
            case VAMPIRE_5ED ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.VAMPIRE_5ED.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.reroll, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.VAMPIRE_5ED.name", userLocale), CustomParameterConfig.InputType.button);
            //Hunter 5ed /custom_parameter start expression: val('$r',{Regular D10 Dice:1<=>16}d10 col 'blue') val('$h', {Desperation Dice:0<=>5}d10 col'purple_dark') val('$s',('$r'+'$h')>=6c) val('$rt','$r'==10c) val('$ht','$h'==10c) val('$ho','$h'==1c) val('$2s',((('$rt'+'$ht'=))/2)*2) val('$ts',('$s'+'$2s'=)) concat('successes: ', '$ts', ifE('$ts',0,ifG('$ho',1,' bestial failure' , ''),''), ifE('$rt' mod 2, 1, ifE('$ht' mod 2, 1, ' messy critical', ''), '')) answer_format: without_expression dice_image_style: polyhedral_knots
            case HUNTER_5ED ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.HUNTER_5ED.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.reroll, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.HUNTER_5ED.name", userLocale), CustomParameterConfig.InputType.button);
            //One-Roll Engine /custom_parameter start expression: groupc({Number of Dice:1<=>10}d10+({Number of Extra Die:0@0/10@1/2r10@2/3r10@3/4r10@4})>={Difficulty:1<=>10})
            case ONE_ROLL_ENGINE ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.ONE_ROLL_ENGINE.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.ONE_ROLL_ENGINE.name", userLocale), CustomParameterConfig.InputType.button);
            //Dungeon Crawl Classics  /sum_custom_set start buttons: 1d4;1d6;1d7;1d8;1d10;1d12;1d14;1d16;1d20;1d24;1d16;1d30;1d100;+1;+2;+3;+4;+5;-1;-2;-3;-4;-5
            case DUNGEON_CRAWL_CLASSICS -> new SumCustomSetConfig(null,
                    ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.DUNGEON_CRAWL_CLASSICS.expression", userLocale)),
                    true, true, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.DUNGEON_CRAWL_CLASSICS.name", userLocale));
            // Tiny D6  /custom_dice start buttons: ifG(1d6>=5c,0,'Success','Failure')@Disadvantage; ifG(2d6>=5c,0,'Success','Failure')@Test;ifG(3d6>=5c,0,'Success','Failure')@Advantage answer_format: without_expression
            case TINY_D6 ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.TINY_D6.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.TINY_D6.name", userLocale));
            //Cyberpunk Red val('$roll', 1d10), if('$roll' =? 1, '$roll'-1d10, '$roll' =?10, '$roll'+1d10, '$roll')+{ability:0<=>9}+{skill:0<=>9}=
            case CYBERPUNK_RED ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.CYBERPUNK_RED.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.CYBERPUNK_RED.name", userLocale), CustomParameterConfig.InputType.button);
            //A Song of Ice and Fire /custom_parameter start expression: {numberOfDice:1<=>15}d6k{keep:1<=>10}
            case ASOIAF ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.ASOIAF.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.ASOIAF.name", userLocale), CustomParameterConfig.InputType.button);
            //City of mist /custom_parameter start expression: 2d6+{modifier:-6<=>6}
            case CITY_OF_MIST ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.CITY_OF_MIST.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.CITY_OF_MIST.name", userLocale), CustomParameterConfig.InputType.button);
            //Risus The Anything RPG "Evens Up" optional rule /custom_parameter start expression: val('$r',{numberOfDice:1<=>10}d!6) ('$r'==2c) + ('$r'==4c) + ('$r'==6c)=
            case RISUS ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.RISUS.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.RISUS.name", userLocale), CustomParameterConfig.InputType.button);
            //Kids on Brooms: /custom_parameter start expression: (d!!{Dice:4@D4/6@D6/8@D8/12@D12/20@D20/4+d!!4@D4Spell/6+d!!4@D6Spell/8+d!!4@D8Spell/12+d!!4@D12Spell/20+d!!4@D20Spell} {plus or minus:+/-}{Modifier:0<=>15}=)-{Difficulty:1<=>30}=
            case KIDS_ON_BROOMS ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.KIDS_ON_BROOMS.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.KIDS_ON_BROOMS.name", userLocale), CustomParameterConfig.InputType.button);
            //Alternate 5e Calculating setup: /sum_custom_set start buttons: d4;d6;d8;d10;d12;d20;d100;1;2;3;4;5;k@Keep Highest;L@Keep Lowest;(2d20k1)@D20 Advantage;(2d20L1)@D20 Disadvantage;-@Minus;+@Plus;4d6k3=@Stats;,@Split;[Muliple dice can be rolled using Number then die type. Plus Minus can be used to add modifiers If adding a modifier to an Advantage or disadvantage roll those buttons must be used. Keep Highest Lowest only work correctly with multiple dice of the same type. If you want to roll dice added together use Plus between each die type. This is a work around to give some guidance. Bot is not supposed to work this way.]@Help always_sum_result: true answer_format: full dice_image_style: polyhedral_alies_v2 dice_image_color: orange_and_silver
            case DND5_CALC -> new SumCustomSetConfig(null,
                    ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.DND5_CALC.expression", userLocale)),
                    true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.DND5_CALC.name", userLocale));
            // Rêve de Dragon /custom_dice start buttons: 1d4@D4;1d6@D6;2d6=@2D6;1d7@D7;1d8@D8;val('roll',1d!8 col 'special') val('diceCount','roll' c) 'roll'-'diceCount'+7=@DDR;2d10=@2D10;1d12@D12;val('$r',1d12 col 'special'),if('$r'=?1,'vaisseau','$r'=?2,'sirène','$r'=?3,'faucon','$r'=?4,'couronne','$r'=?5,'dragon','$r'=?6,'épées','$r'=?7,'lyre','$r'=?8,'serpent','$r'=?9,'poisson acrobate','$r'=?10,'araignée','$r'=?11,'roseaux','$r'=?12,'château dormant')@DAS;1d20@D20;1d100@D100 answer_format: without_expression dice_image_style: polyhedral_RdD
            case REVE_DE_DRAGON ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.REVE_DE_DRAGON.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.REVE_DE_DRAGON.name", userLocale));
            //Public Access (Carved from Brindlewood): /custom_dice start buttons: 2d6=@Roll;3d6k2=@Advantage;3d6l2=@Disadvantage answer_format: full dice_image_style: polyhedral_3d dice_image_color: red_and_white
            case PUBLIC_ACCESS ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.PUBLIC_ACCESS.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.PUBLIC_ACCESS.name", userLocale));
            //custom_dice start buttons:asc((0d6 > 4) col 'black' + replace(1d6 > 4, [6], 'Computer!')) @ 0d6 + Computer; asc((1d6 > 4) col 'black' + replace(1d6 > 4, [6], 'Computer!')) @ 1d6 + Computer; asc((2d6 > 4) col 'red' + replace(1d6 > 4, [6], 'Computer!')) @ 2d6 + Computer; asc((3d6 > 4) col 'orange' + replace(1d6 > 4, [6], 'Computer!')) @ 3d6 + Computer; asc((4d6 > 4) col 'yellow' + replace(1d6 > 4, [6], 'Computer!')) @ 4d6 + Computer; asc((5d6 > 4) col 'green' + replace(1d6 > 4, [6], 'Computer!')) @ 5d6 + Computer; asc((6d6 > 4) col 'cyan' + replace(1d6 > 4, [6], 'Computer!')) @ 6d6 + Computer; asc((7d6 > 4) col 'blue' + replace(1d6 > 4, [6], 'Computer!')) @ 7d6 + Computer; asc((8d6 > 4) col 'magenta' + replace(1d6 > 4, [6], 'Computer!')) @ 8d6 + Computer; asc((9d6 > 4) col 'white' + replace(1d6 > 4, [6], 'Computer!')) @ 9d6 + Computer dice_image_style:polyhedral_2d dice_image_color:gray
            case PARANOIA ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.PARANOIA.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, "grey"), userLocale, null, I18n.getMessage("rpg.system.command.preset.PARANOIA.name", userLocale));
            //Candela Obscura (https://darringtonpress.com/candela/)
            ///sum_custom_set start buttons: +2d6l1 col 'blue'@None;+1d6@1;+2d6@2;+3d6@3;+4d6@4;+5d6@5;+6d6@6;+1d6 col  'purple_white'@:star2: Add Gilded? always_sum_result: false answer_format: without_expression dice_image_style: polyhedral_knots
            case CANDELA_OBSCURA -> new SumCustomSetConfig(null,
                    ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.CANDELA_OBSCURA.expression", userLocale)),
                    false, true, false, null, null, AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.CANDELA_OBSCURA.name", userLocale));
            ///sum_custom_set start buttons: val('$r',2d6l1) if('$r'<=?3, 'Miss', '$r' in [5,4], 'Mixed Success', '$r' in [6], 'Full Success')@Action Roll: 0 ;val('$r',1d6) if('$r'<=?3, 'Miss', '$r' in [5,4], 'Mixed Success', '$r' in [6], 'Full Success')@Action Roll: 1; val('$r',2d6k1) if('$r'<=?3, 'Miss', '$r' in [5,4], 'Mixed Success', '$r' in [6], 'Full Success')@Action Roll: 2; val('$r',3d6k1) if('$r'<=?3, 'Miss', '$r' in [5,4], 'Mixed Success', '$r' in [6], 'Full Success')@Action Roll: 3; val('$r',4d6k1) if('$r'<=?3, 'Miss', '$r' in [5,4], 'Mixed Success', '$r' in [6], 'Full Success')@Action Roll: 4; val('$r',5d6k1) if('$r'<=?3, 'Miss', '$r' in [5,4], 'Mixed Success', '$r' in [6], 'Full Success')@Action Roll: 5; val('$r',6d6k1) if('$r'<=?3, 'Miss', '$r' in [5,4], 'Mixed Success', '$r' in [6], 'Full Success')@Action Roll: 6;val('$r',1d6) if('$r'<=?3, 'Miss (Gilded)', '$r' in [5,4], 'Mixed Success (Gilded)', '$r' in [6], 'Full Success (Gilded)')@+1 Gilded; val('$r',2d6k1) if('$r'<=?3, 'Miss (Gilded)', '$r' in [5,4], 'Mixed Success (Gilded)', '$r' in [6], 'Full Success (Gilded)')@+2 Gilded ;val('$r',3d6k1) if('$r'<=?3, 'Miss (Gilded)', '$r' in [5,4], 'Mixed Success (Gilded)', '$r' in [6], 'Full Success (Gilded)')@+3 Gilded answer_format: without_expression dice_image_style: none
            case CANDELA_OBSCURA2 -> new SumCustomSetConfig(null,
                    ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.CANDELA_OBSCURA2.expression", userLocale)),
                    false, true, false, null, null, AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.CANDELA_OBSCURA2.name", userLocale));
            //Prowlers & Paragons Ultimate Edition (https://www.drivethrurpg.com/product/346742/Prowlers--Paragons-Ultimate-Edition)
            ///custom_parameter start expression: val('$r',{number of dice:1<=>12}d6),
            //val('$total',replace('$r', [1/3/5], 0, [2/4], 1, [6], 2)=), '$total'_' successes' dice_image_style: polyhedral_alies_v1
            case PROWLERS_PARAGONS ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.PROWLERS_PARAGONS.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, DiceImageStyle.polyhedral_alies_v1.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.PROWLERS_PARAGONS.name", userLocale), CustomParameterConfig.InputType.button);
            //Bluebeard's Bride (https://www.drivethrurpg.com/product/224782/Bluebeards-Bride)
            ///custom_parameter start expression: val('$roll',(2d6))
            //val('$mod',{Modifier:-1<=>1})
            //val('$total',('$roll'+'$mod')=) if('$total'>=?10, 'Hit', '$total'<=?6, 'Miss', 'Mitigated Hit') answer_format: without_expression dice_image_style: polyhedral_alies_v2 dice_image_color: blue_and_gold
            case BLUEBEARD_BRIDE ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.BLUEBEARD_BRIDE.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_gold"), userLocale, null, I18n.getMessage("rpg.system.command.preset.BLUEBEARD_BRIDE.name", userLocale), CustomParameterConfig.InputType.button);
            case EXPANSE ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.EXPANSE.expression", userLocale)), AnswerFormatType.only_dice, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.expanse, "mars_light"), userLocale, null, I18n.getMessage("rpg.system.command.preset.EXPANSE.name", userLocale));
            case ALIEN ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.ALIEN.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, "blue"), userLocale, null, I18n.getMessage("rpg.system.command.preset.ALIEN.name", userLocale), CustomParameterConfig.InputType.button);
            case HEROES_OF_CERULEA ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.HEROES_OF_CERULEA.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.HEROES_OF_CERULEA.name", userLocale));
            //custom_parameter start expression: val('$fRoll',1d6 col 'white')  val('$mRoll',1d6 col 'red') val('$rRoll',1d6 col 'white') val('$modi',{Modificator:-3<=>9}) if(('$fRoll' + '$mRoll' + '$rRoll'=) =? 3,val('$res','$fRoll' + '$mRoll' + '$rRoll'), val('$res','$fRoll' + replace('$mRoll',1,6) + '$rRoll')) val('$total', '$res' + '$modi'=)  val('$resTotal','$res' _ ' = ' _ '$total')  if('$mRoll'=?1, if(('$fRoll' + '$rRoll'=) =? 12,'$resTotal' _ ' Ultra Fantastic!', ('$fRoll' + '$rRoll'=) =? 2, '$resTotal' _ ' Botch!', '$resTotal' _' Fantastic!'), '$resTotal') answer_format: without_expression dice_image_style: d6_marvel_v2 dice_image_color: white answer_interaction: reroll
            case MARVEL ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.MARVEL.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.reroll, null, new DiceStyleAndColor(DiceImageStyle.d6_marvel_v2, D6MarvelV2.WHITE), userLocale, null, I18n.getMessage("rpg.system.command.preset.MARVEL.name", userLocale), CustomParameterConfig.InputType.button);
            case DND5_CALC2 -> new SumCustomSetConfig(null,
                    ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.DND5_CALC2.expression", userLocale)),
                    true, true, false, null, null, AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), userLocale, null, I18n.getMessage("rpg.system.command.preset.DND5_CALC2.name", userLocale));
            case PBTA ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.PBTA.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.PBTA.name", userLocale));
            ///custom_parameter start expression: val('$w', 0) val('$fate', {Test: replace(1d12,11, 0,12,100)@⬟ Normal /(2r(replace(1d12,11, 0,12,100)))K1@⬟⬟ Favored /(2r(replace(1d12,11, 0,12,100)))L1@⬠⬠ Ill-Favored /200@✶Magical /val('$w', 1) replace(1d12,11, 0,12,100)@▢ Weary /replace(1d12,11, -666,12,100)@👁 Miserable /val('$w', 1) (2r(replace(1d12,11, 0,12,100)))K1@⬟⬟ ▢ /val('$w', 1) (2r(replace(1d12,11, 0,12,100)))L1@⬠⬠ ▢ /(2r(replace(1d12,11, -666,12,100)))K1@⬟⬟ 👁 /(2r(replace(1d12,11, -666,12,100)))L1@⬠⬠ 👁 /val('$w', 1) replace(1d12,11, -666,12,100)@⬟ ▢👁 /val('$w', 1) (2r(replace(1d12,11, -666,12,100)))K1@⬟⬟ ▢👁 /val('$w', 1) (2r(replace(1d12,11, -666,12,100)))L1@⬠⬠ ▢👁} ) val('$s', {Success Dice: 0d6@◇/1d6@◆/2d6@◆◆/3d6@◆◆◆/4d6@◆◆◆◆/5d6@◆◆◆◆◆/6d6@◆◆◆◆◆◆/7d6@7◆/8d6@8◆/9d6@9◆/10d6@10◆} ) val('$TN', {Target Number: 0@Aucun/12/13/14/15/16/17/18/19/20/21/22/23/24} ) concat('\n', if('$w'=?1, val('$s', '$s'>=4)), val('$t', ('$s'>5)c), val('$total', '$fate' + '$s'=), '   ', if('$fate'=?100, '[ᚠ]', '$fate'=?200, '[✶]', '$fate'=?-666, '[👁]', '['_'$total'_']' ), '   ', if('$fate'=?100, '⬟:ᚠ', '$fate'=?200, '', '$fate'=?0||'$fate'=?-666, '⬟:👁', '⬟:'_'$fate'), '   ', if('$fate'=?200||'$TN'=?0||'$total'>=?'$TN', '+++ SUCCESS ! +++', '--- FAILURE ! ---'), '   ', if('$total'>=?'$TN'&&'$t'>?0, '  [t] successes= '_'$t', '') ) answer_format: without_expression dice_image_style: polyhedral_RdD dice_image_color: default answer_interaction: none
            case THE_ONE_RING ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.THE_ONE_RING.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.THE_ONE_RING.name", userLocale), CustomParameterConfig.InputType.button);
            case EZD6 ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.EZD6.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.EZD6.name", userLocale));
            // RebellionUnplugged. /custom_dice start buttons: 3d6l2=@Ability1;val('$3',3d6),(('$3'l1=) + ('$3'k1=))=@Ability2;3d6k2=@Ability3;3d6=@Ability4 answer_format: without_expression dice_image_style: d6_dots dice_image_color: black_and_gold
            case REBELLION_UNPLUGGED ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.REBELLION_UNPLUGGED.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, D6Dotted.BLACK_AND_GOLD), userLocale, null, I18n.getMessage("rpg.system.command.preset.REBELLION_UNPLUGGED.name", userLocale));
            //"Star Wars - West End Games D6 Rules, 2nd Edition REUP".
            ///custom_parameter start expression: val('$dt', {Skill dice:1<=>15}) val('$d', ('$dt'-1=)d6 col'blue') val('$ed',1d!6) val('$b', {+:0<=>2}) if('$ed'=?1, 'Complication! '_'$d'-('$d'k1)+'$b'=_' or '_'$d'+'$ed'+'$b'=, '$d'+'$ed'+'$b'=) dice_image_style: polyhedral_2d
            case STAR_WARS_D6 ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.STAR_WARS_D6.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, DiceImageStyle.polyhedral_2d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.STAR_WARS_D6.name", userLocale), CustomParameterConfig.InputType.button);
            ///sum_custom_set start buttons: +(replace(exp(1d[0,0,1,1,2,2*] col 'white','2*'),'2*',2)=) @White;+(replace(exp(1d[0,0,1,2,3,3*] col 'yellow','3*'),'3*',3)=)@Yellow;+(replace(exp(1d[0,0,2,3,3,4*] col 'red', '4*'),'4*',4)=)@Red;+(replace(exp(1d[0,0,3,3,4,5*] col 'black', '5*'),'5*',5)=)@Black prefix: val('dice', postfix: ) if('dice'==0c>=?2,'dice'= _ ' Miss for Player', 'dice'=) answer_format: without_expression dice_image_style: polyhedral_2d
            case OATHSWORN -> new SumCustomSetConfig(null,
                    ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.OATHSWORN.expression", userLocale)),
                    false, true, true, "val('dice',", ") if('dice'==0c>=?2,'dice'= _ ' Miss for Player', 'dice'=)", AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, DiceImageStyle.polyhedral_2d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.OATHSWORN.name", userLocale));
            ///custom_parameter start expression:  if((2d10<(1d6+1=))c=?0,'failure',(2d10<(1d6+1=))c=?1, 'mixed results', 'total success')
            case IRONSWORN ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.IRONSWORN.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.IRONSWORN.name", userLocale), CustomParameterConfig.InputType.button);

            // /custom_dice start buttons: val('$roll',d20=) if('$roll'=?20, 'Nailed It!', '$roll'  in [19,18,17,16,15,14,13,12,11] 'Success', '$roll'  in [10,9,8,7,6] 'Touch Choice', '$roll'  in [5,4,3,2] 'Failure', 'Cascade Failure')@Core;d20 @Heat Check; val('$roll',d20=) if('$roll'=?20, 'Reactor Overdrive: Your Mech’s reactor goes into overdrive. Your Mech can take any additional action this turn or Push their next roll within 10 minutes for free.', '$roll'  in [19,18,17,16,15,14,13,12,11] 'Reactor Overheat: Your Mech shuts down and gains the Vulnerable Trait. Your Mech will re-activate at the end of your next turn. In addition, your Mech takes an amount of SP damage equal to your current Heat.', '$roll'  in [10,9,8,7,6] 'Module Overload: One of your Mech’s Modules chosen at random or by the Mediator is destroyed.', '$roll'  in [5,4,3,2] 'System Overload: One of your Mech’s Systems chosen at random or by the Mediator is destroyed. ', 'Reactor Overload: Your Mech, Systems, Modules, and all Cargo, are destroyed in an explosive meltdown. See Table For More.')@Reactor Overload; val('$roll',d20=) if('$roll'=?20, 'You salvage the Mech Chassis, a System, and a Module of your choice mounted on it. They have the Damaged Condition. Everything else is considered destroyed.', '$roll'  in [19,18,17,16,15,14,13,12,11] 'You salvage the Mech Chassis, a System, or a Module of your choice mounted on it. They have the Damaged Condition. Everything else is considered destroyed.', '$roll'  in [10,9,8,7,6] 'You salvage a System or Module of your choice mounted on the Mech. It has the Damaged Condition. Everything else is considered destroyed.', '$roll'  in [5,4,3,2] 'You salvage half of the Salvage Value of the Mech Chassis in Scrap of its Tech Level, to a minimum of 1. Everything else is considered destroyed. ', 'The Mech is unsalvageable')@Mech Salvage; val('$roll',d20=) if('$roll'=?20, 'You find a Mech Chassis, System, or Module at the Tech Level of the area. It is in the damaged Condition.', '$roll'  in [19,18,17,16,15,14,13,12,11] 'You find 3 Scrap of the Tech Level of the area.', '$roll'  in [10,9,8,7,6] 'You find 2 Scrap of the Tech Level of the area.', '$roll'  in [5,4,3,2] 'You find 1 Scrap of the Tech Level of the area. ', 'You find nothing in this area')@Area Salvage;
            case SALVAGE_UNION ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.SALVAGE_UNION.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.SALVAGE_UNION.name", userLocale));
            // /custom_dice start buttons: 1d20;2d20;3d20;4d20;5d20;1d[🗡️/⚔️/❌/❌/☢️/☢️]@ 1 Combat Dice;2d[🗡️/⚔️/❌/❌/☢️/☢️]@ 2 Combat Dice;3d[🗡️/⚔️/❌/❌/☢️/☢️]@ 3 Combat Dice;4d[🗡️/⚔️/❌/❌/☢️/☢️]@ 4 Combat Dice;5d[🗡️/⚔️/❌/❌/☢️/☢️]@ 5 Combat Dice;1d[🧠/🧠/❤️/❤️/❤️/❤️/❤️/❤️/🤛/🤛/🤛/🤜/🤜/🤜/🦶/🦶/🦶/🥾/🥾/🥾]@ Limb Dice answer_format: full dice_image_style: polyhedral_2d dice_image_color: cyan'
            case FALLOUT ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.FALLOUT.expression", userLocale)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, PolyhedralSvgWithColor.CYAN), userLocale, null, I18n.getMessage("rpg.system.command.preset.FALLOUT.name", userLocale));

            ///custom_parameter start expression:val('$b',{Base Dice:1<=>6}d6 col 'black_and_gold') val('$s',{Skill Dice:0<=>8}d6 col 'red_and_gold') val('$g',{Gear Dice:0<=>6}d6 col 'blue_and_gold') val('$a',{Artifact Dice:0@none/1d8@Mighty/1d10@Epic/1d12@Legendary} col 'green_and_gold') val('$sa',if('$a'=?12,4,'$a'>=?10,3,'$a'>=?8,2,'$a'>=?6,1,0)) val('$ts',('$b'+'$s'+'$g')==6c) '$ts'+'$sa'= answer_format:without_expression dice_image_style:polyhedral_alies_v2
            case FORBIDDEN_LANDS ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.FORBIDDEN_LANDS.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, DiceImageStyle.polyhedral_alies_v2.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.FORBIDDEN_LANDS.name", userLocale), CustomParameterConfig.InputType.button);

            //4dF:val('$r', 4d[＋,▢,−]) _ '['_ '$r' _ '] = ' _ replace('$r' ,'＋',1,'▢',0,'−',-1)=@4dF
            case FATE_ALIAS ->
                    new AliasConfig(ChannelConfigCommand.parseStringToMultiAliasList(I18n.getMessage("rpg.system.command.preset.FATE_ALIAS.expression", userLocale)), I18n.getMessage("rpg.system.command.preset.FATE_ALIAS.name", userLocale));
            //d20:d20;adv:2d20k1@Advantage;dis:2d20L1@Disadvantage
            case DND5_ALIAS ->
                    new AliasConfig(ChannelConfigCommand.parseStringToMultiAliasList(I18n.getMessage("rpg.system.command.preset.DND5_ALIAS.expression", userLocale)), I18n.getMessage("rpg.system.command.preset.DND5_ALIAS.name", userLocale));
            //w:d!10>=8c
            case NWOD_ALIAS ->
                    new AliasConfig(ChannelConfigCommand.parseStringToMultiAliasList(I18n.getMessage("rpg.system.command.preset.NWOD_ALIAS.expression", userLocale)), I18n.getMessage("rpg.system.command.preset.NWOD_ALIAS.name", userLocale));
            //(?<numberOfDice>\\d+)r(?<target>\\d+)::val('roll',${numberOfDice}d10) ('roll'>=${target}c)-('roll'==1c)=@:${numberOfDice}d10 vs ${target};(?<numberOfDice>\\d+)re(?<target>\\d+)::val('roll',${numberOfDice}d!10) ('roll'>=${target}c)-('roll'==1c)=@:${numberOfDice}d10 vs ${target} with reroll on 10
            case OWOD_ALIAS ->
                    new AliasConfig(ChannelConfigCommand.parseStringToMultiAliasList(I18n.getMessage("rpg.system.command.preset.OWOD_ALIAS.expression", userLocale)), I18n.getMessage("rpg.system.command.preset.OWOD_ALIAS.name", userLocale));
            //(?<numberOfDice>\\d+)sr::val('roll',${numberOfDice}d6) concat('roll'>4c, if('roll'==1c >? 'roll'c/2,' - Glitch!'))@${numberOfDice}d6
            case SHADOWRUN_ALIAS ->
                    new AliasConfig(ChannelConfigCommand.parseStringToMultiAliasList(I18n.getMessage("rpg.system.command.preset.SHADOWRUN_ALIAS.expression", userLocale)), I18n.getMessage("rpg.system.command.preset.SHADOWRUN_ALIAS.name", userLocale));
            //r:d!!;sw(?<sides>\\\\d+)::1d!!${sides} + 1d!!6 k1@d${sides} Wildcard
            case SAVAGE_WORLDS_ALIAS ->
                    new AliasConfig(ChannelConfigCommand.parseStringToMultiAliasList(I18n.getMessage("rpg.system.command.preset.SAVAGE_WORLDS_ALIAS.expression", userLocale)), I18n.getMessage("rpg.system.command.preset.SAVAGE_WORLDS_ALIAS.name", userLocale));
            // (?<numberOfDice>\\\\d+)b::val('diceRoll', if(${numberOfDice}=?0,2d6L1, ${numberOfDice}d6)) val('sixes','diceRoll'==6c) val('partials','diceRoll'>3<6c) if('sixes'>?1,'Critical Success - You do it with increased effect.', 'sixes'=?1,'Success - You do it.','partials' >? 0,'Partial Success - You do it but suffer severe harm, a serious complication or have reduced effect.','Failure - You suffer severe harm, a serious complication occurs, or you lose this opportunity for action.')@${numberOfDice} Dice
            case BLADES_IN_THE_DARK_ALIAS ->
                    new AliasConfig(ChannelConfigCommand.parseStringToMultiAliasList(I18n.getMessage("rpg.system.command.preset.BLADES_IN_THE_DARK_ALIAS.expression", userLocale)), I18n.getMessage("rpg.system.command.preset.BLADES_IN_THE_DARK_ALIAS.name", userLocale));
            case CYBERPUNK_RED_ALIAS ->
                    new AliasConfig(ChannelConfigCommand.parseStringToMultiAliasList(I18n.getMessage("rpg.system.command.preset.CYBERPUNK_RED_ALIAS.expression", userLocale)), I18n.getMessage("rpg.system.command.preset.CYBERPUNK_RED_ALIAS.name", userLocale));
            // Warhammer Age of Sigmar: Soulbound /custom_parameter start expression: val('$Pool',{Pool:1<=>20}d6 col 'blue_and_gold') val('$Success', ('$Pool'>={Difficulty:2<=>6})c) val ('$Complexity', {Complexity:1<=>20}) val('$Test', ('$Success'>='$Complexity')c) val('$Overcast', '$Success' - '$Complexity'=) concat('$Success'_' successes ; '_if('$Test'>?0,'Success ('_'$Overcast'_' overcast)','Failure')) dice_image_style: polyhedral_alies_v2 dice_image_color: blue_and_gold answer_interaction: reroll
            case WARHAMMER_AOS ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.WARHAMMER_AOS.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.reroll, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_gold"), userLocale, null, I18n.getMessage("rpg.system.command.preset.WARHAMMER_AOS.name", userLocale), CustomParameterConfig.InputType.button);
            // Ghostbusters: A Frightfully Cheerful Roleplaying Game First Edition & Spooktacular  /custom_parameter start expression: val('$dice',{Dice Pool:1<=>25}),val('$diff',{Difficulty:5@Easy Job/10@Normal Job/20@Hard Job/30@Impossible Job}) val('$ghost', 1d6 col 'green_and_white'), if('$ghost'=?6,val('$res',('$dice' -1=)d6=), val('$res', ('$dice' -1=)d6+ '$ghost'=)), '$res' _ ' vs ' _ '$diff' _ ': ' _ if('$res' >=? '$diff', 'success', 'failure') _ if('$ghost'=?6, ' and something bad happens', '')
            case GHOSTBUSTERS ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.GHOSTBUSTERS.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.GHOSTBUSTERS.name", userLocale), CustomParameterConfig.InputType.button);
            // Shadowdark /sum_custom_set start buttons: d4;d6;d8;d10;d12;d20;d100;1;2;3;4;5;(2d20k1)@D20 Advantage;(2d20L1)@D20 Disadvantage;;-@Minus;+@Plus;5x3d6=@!Stats;,@Split always_sum_result: true hide_expression_in_answer: false answer_format: full dice_image_style: polyhedral_3d dice_image_color: red_and_white answer_interaction: none
            case SHADOWDARK -> new SumCustomSetConfig(null,
                    ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.SHADOWDARK.expression", userLocale)),
                    true, false, false, null, null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.SHADOWDARK.name", userLocale));
            //val('$p',{Power:-3<=>6}), val('$r',2d6=), val('$e',('$p'+1)k1), val('$rp','$r' + '$p'=), 'Result: ' _ '$rp' _ ' = ' _  if('$r'=?2,'Snake Eye: Miss','$r'=?12,'Boxcar: Strong Hit with Effect ' _'$e',('$r'+'$p'=)<?7,'Miss',('$r'+'$p'=)<?10,'Mixed Hit with Effect '_'$e',('$r'+'$p'=)>=?10, 'Strong Hit with Effect '_'$e')
            case OTHERSCAPE ->
                    new CustomParameterConfig(null, I18n.getMessage("rpg.system.command.preset.OTHERSCAPE.expression", userLocale), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.OTHERSCAPE.name", userLocale), CustomParameterConfig.InputType.button);
            case OTHERSCAPE_ALIAS ->
                    new AliasConfig(ChannelConfigCommand.parseStringToMultiAliasList(I18n.getMessage("rpg.system.command.preset.OTHERSCAPE_ALIAS.expression", userLocale)), I18n.getMessage("rpg.system.command.preset.OTHERSCAPE_ALIAS.name", userLocale));
            ///custom_dice start buttons: val ('$hope', 1d12 col 'yellow_and_white'),val ('$fear', 1d12 col 'purple_and_white'),val('$result','$hope' + '$fear'=), if('$hope' >? '$fear', concat('$result', ' with Hope'), '$hope'<?'$fear', concat('$result', ' with Fear'), '$hope'=?'$fear', 'Critical')@Duality roll;; val ('$hope', 1d12 col 'yellow_and_white'),val ('$fear', 1d12 col 'purple_and_white'),val('$result','$hope' + '$fear' + 1d6 col 'green_and_white'=), if('$hope' >? '$fear', concat('$result', ' with Hope'), '$hope'<?'$fear', concat('$result', ' with Fear'), '$hope'=?'$fear', 'Critical')@With advantage; val ('$hope', 1d12 col 'yellow_and_white'),val ('$fear', 1d12 col 'purple_and_white'),val('$result','$hope' + '$fear' - 1d6 col 'grey_and_white'=), if('$hope' >? '$fear', concat('$result', ' with Hope'), '$hope'<?'$fear', concat('$result', ' with Fear'), '$hope'=?'$fear', 'Critical')@With disadvantage;
            case DAGGERHEART ->
                    new CustomDiceConfig(null, ButtonHelper.parseString(I18n.getMessage("rpg.system.command.preset.DAGGERHEART.expression", userLocale)), AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_RdD.getDefaultColor()), userLocale, null, I18n.getMessage("rpg.system.command.preset.DAGGERHEART.name", userLocale));
        };
    }

    public static String getCommandString(PresetId presetId, Locale locale) {
        Config config = createConfig(presetId, locale);
        if (config instanceof AliasConfig) {
            return "/channel_config alias multi_save aliases:%s scope:all_users_in_this_channel".formatted(config.toCommandOptionsString());
        }
        String commandId = presetId.getCommandId();
        return "/%s start %s".formatted(commandId, config.toCommandOptionsString());
    }

    public static boolean matchRpgPreset(String typed, RpgSystemCommandPreset.PresetId presetId, @NonNull Locale userLocale) {
        if (Strings.isNullOrEmpty(typed)) {
            return true;
        }
        if (presetId.getName(userLocale).toLowerCase().contains(typed.toLowerCase())) {
            return true;
        }
        if (presetId.getName(Locale.ENGLISH).toLowerCase().contains(typed.toLowerCase())) {
            return true;
        }
        if (presetId.getSynonymes(userLocale).stream().anyMatch(n -> n.toLowerCase().contains(typed.toLowerCase()))) {
            return true;
        }
        if (presetId.getSynonymes(Locale.ENGLISH).stream().anyMatch(n -> n.toLowerCase().contains(typed.toLowerCase()))) {
            return true;
        }
        return false;
    }

    public Optional<EmbedOrMessageDefinition> createMessage(PresetId presetId, UUID newConfigUUID, Long guildId, long channelId, long userId, Locale userLocale) {
        Config config = createConfig(presetId, userLocale);
        return switch (config) {
            case CustomDiceConfig customDiceConfig ->
                    startMessagePreset(customDiceConfig, customDiceCommand, newConfigUUID, guildId, channelId, userId);
            case SumCustomSetConfig sumCustomSetConfig ->
                    startMessagePreset(sumCustomSetConfig, sumCustomSetCommand, newConfigUUID, guildId, channelId, userId);
            case CustomParameterConfig customParameterConfig ->
                    startMessagePreset(customParameterConfig, customParameterCommand, newConfigUUID, guildId, channelId, userId);
            case AliasConfig aliasConfig -> saveAlias(aliasConfig, newConfigUUID, guildId, channelId);
            default -> throw new IllegalStateException("Could not create valid config for: " + presetId);
        };
    }

    private <C extends RollConfig> Optional<EmbedOrMessageDefinition> startMessagePreset(C config, AbstractCommand<C, ?> command, UUID newConfigUUID, Long guildId, long channelId, long userId) {
        command.createMessageConfig(newConfigUUID, guildId, channelId, userId, config).ifPresent(persistenceManager::saveMessageConfig);
        return Optional.of(command.createSlashResponseMessage(newConfigUUID, config, channelId));
    }

    private Optional<EmbedOrMessageDefinition> saveAlias(AliasConfig config, UUID newConfigUUID, Long guildId, long channelId) {
        channelConfigCommand.saveAliasesConfig(config.getAliasList(), channelId, guildId, null, () -> newConfigUUID, config.getName());
        return Optional.empty();
    }

    @AllArgsConstructor
    @Getter
    public enum PresetId {
        DND5_IMAGE(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        DND5(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        DND5_ALIAS(ChannelConfigCommand.COMMAND_NAME, AliasHelper.CHANNEL_ALIAS_CONFIG_TYPE_ID),
        DND5_CALC(SumCustomSetCommand.COMMAND_NAME, SumCustomSetCommand.CONFIG_TYPE_ID),
        NWOD(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        OWOD(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        NWOD_ALIAS(ChannelConfigCommand.COMMAND_NAME, AliasHelper.CHANNEL_ALIAS_CONFIG_TYPE_ID),
        OWOD_ALIAS(ChannelConfigCommand.COMMAND_NAME, AliasHelper.CHANNEL_ALIAS_CONFIG_TYPE_ID),
        SHADOWRUN(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        SHADOWRUN_IMAGE(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        SHADOWRUN_ALIAS(ChannelConfigCommand.COMMAND_NAME, AliasHelper.CHANNEL_ALIAS_CONFIG_TYPE_ID),
        SAVAGE_WORLDS(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        SAVAGE_WORLDS_ALIAS(ChannelConfigCommand.COMMAND_NAME, AliasHelper.CHANNEL_ALIAS_CONFIG_TYPE_ID),
        FATE_IMAGE(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        FATE(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        FATE_ALIAS(ChannelConfigCommand.COMMAND_NAME, AliasHelper.CHANNEL_ALIAS_CONFIG_TYPE_ID),
        COIN(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        DICE_CALCULATOR(SumCustomSetCommand.COMMAND_NAME, SumCustomSetCommand.CONFIG_TYPE_ID),
        OSR(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        TRAVELLER(SumCustomSetCommand.COMMAND_NAME, SumCustomSetCommand.CONFIG_TYPE_ID),
        BLADES_IN_THE_DARK(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        BLADES_IN_THE_DARK_ALIAS(ChannelConfigCommand.COMMAND_NAME, AliasHelper.CHANNEL_ALIAS_CONFIG_TYPE_ID),
        BLADES_IN_THE_DARK_IMAGE(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        BLADES_IN_THE_DARK_DETAIL(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        CALL_OF_CTHULHU_7ED(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        EXALTED_3ED(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        VAMPIRE_5ED(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        HUNTER_5ED(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        ONE_ROLL_ENGINE(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        DUNGEON_CRAWL_CLASSICS(SumCustomSetCommand.COMMAND_NAME, SumCustomSetCommand.CONFIG_TYPE_ID),
        TINY_D6(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        CYBERPUNK_RED(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        ASOIAF(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        CITY_OF_MIST(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        RISUS(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        KIDS_ON_BROOMS(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        REVE_DE_DRAGON(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        PARANOIA(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        PUBLIC_ACCESS(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        CANDELA_OBSCURA(SumCustomSetCommand.COMMAND_NAME, SumCustomSetCommand.CONFIG_TYPE_ID),
        CANDELA_OBSCURA2(SumCustomSetCommand.COMMAND_NAME, SumCustomSetCommand.CONFIG_TYPE_ID),
        PROWLERS_PARAGONS(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        BLUEBEARD_BRIDE(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        EXPANSE(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        ALIEN(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        HEROES_OF_CERULEA(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        MARVEL(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        DND5_CALC2(SumCustomSetCommand.COMMAND_NAME, SumCustomSetCommand.CONFIG_TYPE_ID),
        PBTA(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        THE_ONE_RING(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        EZD6(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        REBELLION_UNPLUGGED(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        STAR_WARS_D6(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        OATHSWORN(SumCustomSetCommand.COMMAND_NAME, SumCustomSetCommand.CONFIG_TYPE_ID),
        IRONSWORN(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        SALVAGE_UNION(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        FALLOUT(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),
        FORBIDDEN_LANDS(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        CYBERPUNK_RED_ALIAS(ChannelConfigCommand.COMMAND_NAME, AliasHelper.CHANNEL_ALIAS_CONFIG_TYPE_ID),
        WARHAMMER_AOS(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        SHADOWDARK(SumCustomSetCommand.COMMAND_NAME, SumCustomSetCommand.CONFIG_TYPE_ID),
        GHOSTBUSTERS(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        OTHERSCAPE(CustomParameterCommand.COMMAND_NAME, CustomParameterCommand.CONFIG_TYPE_ID),
        OTHERSCAPE_ALIAS(ChannelConfigCommand.COMMAND_NAME, AliasHelper.CHANNEL_ALIAS_CONFIG_TYPE_ID),
        DAGGERHEART(CustomDiceCommand.COMMAND_NAME, CustomDiceCommand.CONFIG_TYPE_ID),;

        private final String commandId;
        private final String configClassType;

        public static boolean isValid(String in) {
            return Arrays.stream(PresetId.values()).anyMatch(s -> s.name().equals(in));
        }

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
}
