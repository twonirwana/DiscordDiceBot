Ce bot sert à lancer des dés dans discord.
Le bot doit être configuré avec une commande slash dans un canal et fournit ensuite un message avec des boutons.
En cliquant sur un bouton, le bot lancera l'expression de dé stockée pour le bouton, affichera le résultat et déplacera
le message avec les boutons vers le bas du canal.
Si le message est en attente, il sera seulement copié et non déplacé.
Cela permet à l'utilisateur de lancer les dés sans avoir à taper de commandes et améliore ainsi la convivialité, en
particulier pour les utilisateurs d'écrans tactiles.
Le bot prend en charge les fils de discussion Discord (le message du bouton doit être ajouté après la création du fil de
discussion), les forums, Text in Voice, et il est possible d'envoyer la réponse dans un canal différent.
Il peut fournir des images des résultats du jet de dés et il est possible de configurer des alias spécifiques au canal
ou à l'utilisateur.

*Le bot a besoin des autorisations suivantes

* créer des commandes d'application (qui sont nécessaires pour contrôler le bot)
* envoyer des messages et Envoyer des messages dans les fils (pour les boutons de dés et les résultats)
* intégrer des liens (affichage des réponses structurées)
* joindre des fichiers (ajouter des images avec le résultat du dé)
* voir les anciens messagess (pour détecter si le message d'un bouton a été épinglé)

# Démarrage rapide

Tapez `/quickstart system` et le bot vous proposera une liste de jeux de dés prêts à jouer. Sélectionnez simplement un
système dans la liste ou continuez à taper pour chercher et filtrer dans la liste.
Voici quelques-uns des systèmes prêts à l'emploi :

`A Song of Ice and Fire`, `Blades in the Dark`, `Bluebeard's Bride`, `Call of Cthulhu 7th Edition`, `Candela Obscura`, `City of Mist`, `Coin Toss`, `Cyberpunk Red`, `Dice Calculator`, `Dungeon & Dragons 5e`, `Dungeon & Dragons 5e Calculator`, `Dungeon & Dragons 5e Calculator 2`, `Dungeon & Dragons 5e with Dice Images`, `Dungeon Crawl Classics`, `EZD6`, `Exalted 3ed`, `Fate`, `Fate with Dice Images`, `Heroes of Cerulea`, `Hunter 5ed`, `Kids on Brooms`, `OSR`, `One-Roll Engine`, `Paranoia: Red Clearance Edition`, `Powered by the Apocalypse`, `Prowlers & Paragons Ultimate Edition`, `Public Access`, `Risus The Anything RPG "Evens Up"`, `Rêve de Dragon`, `Savage Worlds`, `Shadowrun`, `Shadowrun with Dice Images`, `The Expanse`, `The Marvel Multiverse Role-Playing Game`, `The One Ring`, `Tiny D6`, `Traveller`, `Vampire 5ed`, `Year Zero Engine: Alien`, `nWod / Chronicles of Darkness`, `oWod / Storyteller System`

# Boutons de dés personnalisés

Vous pouvez créer vos propres boutons de dés. Presque tous les systèmes RPG peuvent être mappés avec ce bot.
visitez : https://github.com/twonirwana/DiscordDiceBot