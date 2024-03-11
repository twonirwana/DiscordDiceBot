This bot is for rolling dice in discord.
The bot needs to be configured with a slash command in a channel and then provides a message with buttons.
Upon clicking on a button the bot will roll the stored dice expression for the button, post the result and move the
message with the buttons to the bottom of the channel.
If the message is pined, then it will only be copied and not moved.
This allows user to roll dice without typing commands and thereby improves usability, especially for touchscreen users.
The bot supports Discord thread (the button message must be added after the thread creation), forum, Text in Voice, and
it is possible to send the answer in a different channel.
It can provide images of the dice roll results and it is possible to configure channel or user specific aliases.

*The bot need the following permission*:

* create application commands (which are needed to control the bot)
* send message and send message in threads (for the dice buttons and results)
* embed links (display of structured answers)
* attach files (add images with the dice result)
* read message history (to detect if a button message was pinned)

# Quickstart

Type `/quickstart system` and the bot will offer a list of ready to play dice sets. Simple select a system out of the
list or keep typing to search and filter in the list.
Some of the ready to play systems are:

`A Song of Ice and Fire`, `Blades in the Dark`, `Bluebeard's Bride`, `Call of Cthulhu 7th Edition`, `Candela Obscura`, `City of Mist`, `Coin Toss`, `Cyberpunk Red`, `Dice Calculator`, `Dungeon & Dragons 5e`, `Dungeon & Dragons 5e Calculator`, `Dungeon & Dragons 5e Calculator 2`, `Dungeon & Dragons 5e with Dice Images`, `Dungeon Crawl Classics`, `EZD6`, `Exalted 3ed`, `Fate`, `Fate with Dice Images`, `Heroes of Cerulea`, `Hunter 5ed`, `Kids on Brooms`, `OSR`, `One-Roll Engine`, `Paranoia: Red Clearance Edition`, `Powered by the Apocalypse`, `Prowlers & Paragons Ultimate Edition`, `Public Access`, `Risus The Anything RPG "Evens Up"`, `Rêve de Dragon`, `Savage Worlds`, `Shadowrun`, `Shadowrun with Dice Images`, `The Expanse`, `The Marvel Multiverse Role-Playing Game`, `The One Ring`, `Tiny D6`, `Traveller`, `Vampire 5ed`, `Year Zero Engine: Alien`, `nWod / Chronicles of Darkness`, `oWod / Storyteller System`

# Custom Dice Buttons

You can create your own custom dice buttons. Almost all RPG systems can be mapped with this bot, please
visit: https://github.com/twonirwana/DiscordDiceBot