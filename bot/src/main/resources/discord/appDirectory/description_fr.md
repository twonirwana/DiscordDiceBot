Ce bot sert à lancer des dés dans discord.
Le bot doit être configuré avec une commande slash dans un canal et fournit ensuite un message avec des boutons.
En cliquant sur un bouton, le bot lancera l'expression de dé stockée pour le bouton, affichera le résultat et déplacera le message avec les boutons vers le bas du canal.
Si le message est en attente, il sera seulement copié et non déplacé.
Cela permet à l'utilisateur de lancer les dés sans avoir à taper de commandes et améliore ainsi la convivialité, en particulier pour les utilisateurs d'écrans tactiles.
Le bot prend en charge les fils de discussion Discord (le message du bouton doit être ajouté après la création du fil de discussion), les forums, Text in Voice, et il est possible d'envoyer la réponse dans un canal différent.
Il peut fournir des images des résultats du jet de dés et il est possible de configurer des alias spécifiques au canal ou à l'utilisateur.

*Le bot a besoin des autorisations suivantes

* créer des commandes d'application (qui sont nécessaires pour contrôler le bot)
* envoyer des messages et Envoyer des messages dans les fils (pour les boutons de dés et les résultats)
* intégrer des liens (affichage des réponses structurées)
* joindre des fichiers (ajouter des images avec le résultat du dé)
* voir les anciens messagess (pour détecter si le message d'un bouton a été épinglé)

# Démarrage rapide

Tapez `/quickstart système` et le bot vous proposera une liste de jeux de dés prêts à jouer. Sélectionnez simplement un système dans la liste ou continuez à taper pour chercher et filtrer dans la liste.

Beaucoup d'autres systèmes peuvent être mappé avec ce bot, veuillez visiter : https://github.com/twonirwana/DiscordDiceBot