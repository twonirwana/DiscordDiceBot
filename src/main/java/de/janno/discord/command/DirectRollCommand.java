package de.janno.discord.command;

import de.janno.discord.dice.DiceParserHelper;
import de.janno.discord.dice.DiceResult;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import static de.janno.discord.DiscordMessageUtils.createEmbedMessageWithReference;
import static io.micrometer.core.instrument.Metrics.globalRegistry;

@Slf4j
public class DirectRollCommand implements ISlashCommand {
    private static final String ACTION_EXPRESSION = "expression";

    @Override
    public String getName() {
        return "direct_roll";
    }

    @Override
    public ApplicationCommandRequest getApplicationCommand() {
        return ApplicationCommandRequest.builder()
                .name(getName())
                .description("direct roll of dice expression")
                .defaultPermission(true)
                .addOption(ApplicationCommandOptionData.builder()
                        .name(ACTION_EXPRESSION)
                        .required(true)
                        .description("dice expression, e.g. '3d6'")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .build())
                .build();
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ChatInputInteractionEvent event) {
        if (event.getOption(ACTION_EXPRESSION).isPresent()) {
            ApplicationCommandInteractionOption options = event.getOption(ACTION_EXPRESSION).get();
            String diceExpression = options.getValue()
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow();
            globalRegistry.counter(getName() + ".roll." + diceExpression).increment();
            globalRegistry.counter(getName() + ".roll").increment();
            DiceResult result = DiceParserHelper.rollWithDiceParser(diceExpression);

            log.info("Roll {}: {} in channel {}", getName(), result.getResultTitle(), event.getInteraction().getChannelId().asLong());
            return event.reply("...")
                    .onErrorResume(t -> {
                        log.error("Error on replay to slash command", t);
                        return Mono.empty();
                    })
                    .then(event.getInteraction().getChannel().ofType(TextChannel.class)
                            .flatMap(channel -> channel.createMessage(createEmbedMessageWithReference(result.getResultTitle(), result.getResultDetails(), event.getInteraction().getMember().orElseThrow()))
                            ))
                    .then();

        }

        return Mono.empty();
    }
}
