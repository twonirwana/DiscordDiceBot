package de.janno.discord.command;

import de.janno.discord.dice.DiceResult;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IDiscordAdapter {
    Mono<Void> createResultMessageWithEventReference(List<DiceResult> diceResults);

}
