# DiscordDiceBot

This is dice rolling bot for Discord. The bot needs to be configured in each channel and then provides a message with a
button selection. Upon clicking on a button the bot will post the result and move the message with the buttons to the
bottom of the channel. This improves usability, especially for touchscreen users.

Add to Discord channel by following this link:
[Bot invite link](https://discord.com/api/oauth2/authorize?client_id=812381127943782502&permissions=2048&scope=bot%20applications.commands)

The bot need permission to create application commands (which are needed to control the bot) and send message (for the
dice buttons and results).

:warning: **If you can't see the bots messages**: The link preview in the discord setting must be activated to see the
roll results

The bot has currently three systems:

## Custom dice buttons

Use the slash command: `custom_dice start` and add up to 15 custom buttons, each with its own dice expression. For
example `/custom_dice start 1_button:3d6 2_button:10d10 3_button:3d20` produces the three buttons as follows:

![custom_dice_buttons.png](custom_dice_buttons.png)

and on clicking on a button provides the results of the button dice and result of the expression (default it is the sum
of all dice):

![custom_dice_result.png](custom_dice_result.png)

### Dice Expression Notation

Each button can be set with dice expression with the following notation.

| Name | Notation | Example | Description |
|------|----------|---------|-------------|
|  |  |  |  |
| Single Die | `d<numberOfFaces>` | `d6` | roll one, six-sided die |
| Multiple Dice | `<numberOfDice>d<numberOfFaces>` | `3d20` | roll three, twenty-sided dice |
| Keep Dice | `<numberOfDice>d<numberOfFaces>k<numberOfDiceKept>` | `3d6k2` | keeps the the highest values out of three, six-sided dice |
| Multiply Die | `d<numberOfFaces>X` | `d10X` | multiplies the result of `d10 * d10` |
| Multiply Dice | `<numberOfDice>d<numberOfFaces>X` | `2d10X` | multiplies the result of `2d10 * 2d10` |
| Fudge Dice | `dF` | `dF` | rolls a single "fudge" die (a six sided die, 1/3 chance of `-1`, 1/3 chance of `0`, and 1/3 chance of `1`) |
| Multiple Fudge Dice | `<numberOfDice>dF` | `3dF` | rolls multiple fudge dice |
| Weighted Fudge Die | `dF.<weight>` | `dF.1` | A weighted fudge die with 1/6 chance of a `1`, `2/3` chance of a `0` and 1/6 chance of a `-1` |
| Multiple Weighted Fudge Dice | `<numberOfDice>dF.<weight>` | `2dF.1` | multiple weighted fudge dice. |
| Exploding Dice | `<numberOfDice>d<numberOfFaces>!` | `4d6!` | any time the max value of a die is rolled, that die is re-rolled and added to the total |
| Exploding Dice (Target) | `<numberOfDice>d<numberOfFaces>!><target>` | `3d6!>5` | Same as exploding dice, but re-roll on values greater than or equal to the target (note, less than works too) |
| Compounding Dice | `<numberOfDice>d<numberOfFaces>!!` | `3d6!!` | similar to exploding dice, but ALL dice are re-rolled | 
| Compounding Dice (Target) | `<numberOfDice>d<numberOfFaces>!!><target>` | `3d6!!>5` | similar as exploding dice (target), but all dice are re-rolled and added. |
| Target Pool Dice | `<numberOfDice>d<numberOfFaces>[>,<,=]<target>` | `3d6=6` | counts the number of dice that match the target (NOTE: greater & less than also match equals, i.e `>=` and `<=`) | 
| Target Pool Dice (Expression) | `(<expression>)[>,<,=]<target>` | `(4d8-2)>6` | A target pool roll, but where the expression is evaluated to the target. |
| Integer | `<int>` | `42` | typically used in math operations, i.e. `2d4+2` |
| Math | `<left> <operation> <right>` |
| Add | `<left> + <right>`  | `2d6 + 2` | |
| Subtract | `<left> - <right>` | `2 - 1` | |
| Multiply | `<left> * <right>` | `1d4 * 2d6` | |
| Divide | `<left> / <right>` | `4 / 2` | |

see https://github.com/diceroll-dev/dice-parser for more details.

## Count success in a pool

Use the slash command: `/count_successes start`. You need to provide the sides of the dice, the target number, optional
a glitch system as parameter and the number of buttons. For example `/count_successes start dice_sides:10 target_number:
7` creates 15 buttons for 10 sided dice that roll against the target of 7. By clicking on a button a number of dice will
be rolled and the count of the dice with results equal or approve the target number returned.

### Glitch Option

The optional parameter `half_dice_one` will mark the result as glitch if more than half of the dice show 1. The default
is that no glitch system will be used.

### Number of Dice Option

The optional parameter `max_dice` will change the max number of dice (and thereby the number of buttons). The default
value is 15, which will be used if the parameter is not set, and the max number of dice is 25.

### Example

This is a system that can be used for example for the new Word of Darkness (`/count_successes start dice_sides:10
target_number:7`) or Shadowrun (`/count_successes start dice_sides:6 target_number:5 glitch:half_dice_one max_dice:20` ).

![img.png](count_success_buttons.png)

![img.png](count_success_result.png)

## Fate

Use the slash command: `/fate start type:with_modifier` or `/fate start type:simple` to get buttons for Fate. There are
two types simple and with modifier:

### Simple

![fate_example.png](fate_example.png)

### With modifier buttons

![fate_with_modifier.png](fate_with_modifier.png)

Please let me know if another system is needed.
