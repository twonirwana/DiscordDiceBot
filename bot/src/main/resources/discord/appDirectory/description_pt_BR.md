Este bot serve para lançar dados no Discord.
O bot precisa ser configurado com um comando de barra em um canal e, em seguida, fornece uma mensagem com botões.
Ao clicar em um botão, o bot lançará a expressão de dados armazenada para o botão, publicará o resultado e moverá a
mensagem com os botões para a parte inferior do canal.
Se a mensagem for pined, ela será apenas copiada e não movida.
Isso permite que o usuário jogue os dados sem digitar comandos e, portanto, melhora a usabilidade, especialmente para
usuários de telas sensíveis ao toque.
O bot é compatível com o tópico do Discord (a mensagem do botão deve ser adicionada após a criação do tópico), fórum,
texto em voz, e é possível enviar a resposta em um canal diferente.
Ele pode fornecer imagens dos resultados do lançamento de dados e é possível configurar aliases específicos do canal ou
do usuário.

*O bot precisa das seguintes permissões*:

* criar comandos de aplicativos (necessários para controlar o bot)
* enviar mensagem e enviar mensagem em tópicos (para os botões de dados e resultados)
* inserir links (exibição de respostas estruturadas)
* anexar arquivos (adicionar imagens com o resultado do dado)
* Ver histórico de mensagens (para detectar se uma mensagem de botão foi fixada)

# Início rápido

Digite `/quickstart system` e o bot oferecerá uma lista de conjuntos de dados prontos para jogar. Basta selecionar um
sistema da lista ou continuar digitando para pesquisar e filtrar na lista.
Alguns dos sistemas prontos para uso são:

`A Song of Ice and Fire`,`Blades in the Dark`,`Bluebeard's Bride`,`Call of Cthulhu 7th Edition`,`Candela Obscura`,`City of Mist`,`Coin Toss`,`Cyberpunk Red`,`Dice Calculator`,`Dungeon & Dragons 5e`,`Dungeon & Dragons 5e Calculator`,`Dungeon & Dragons 5e Calculator 2`,`Dungeon & Dragons 5e with Dice Images`,`Dungeon Crawl Classics`,`EZD6`,`Exalted 3ed`,`Fate`,`Fate with Dice Images`,`Heroes of Cerulea`,`Hunter 5ed`,`Kids on Brooms`,`OSR`,`One-Roll Engine`,`Paranoia: Red Clearance Edition`,`Powered by the Apocalypse`,`Prowlers & Paragons Ultimate Edition`,`Public Access`,`Risus The Anything RPG "Evens Up"`,`Rêve de Dragon`,`Savage Worlds`,`Shadowrun`,`Shadowrun with Dice Images`,`The Expanse`,`The Marvel Multiverse Role-Playing Game`,`The One Ring`,`Tiny D6`,`Traveller`,`Vampire 5ed`,`Year Zero Engine: Alien`,`nWod / Chronicles of Darkness`,`oWod / Storyteller System`

# Botões de dados personalizados

Você pode criar seus próprios botões de dados personalizados. Quase todos os sistemas de RPG podem ser mapeados com esse bot.
visite: https://github.com/twonirwana/DiscordDiceBot