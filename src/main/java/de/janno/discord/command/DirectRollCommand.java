package de.janno.discord.command;

import com.codahale.metrics.SharedMetricRegistries;
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
        if (getName().equals(event.getCommandName())) {
            if (event.getOption(ACTION_EXPRESSION).isPresent()) {
                ApplicationCommandInteractionOption options = event.getOption(ACTION_EXPRESSION).get();
                String diceExpression = options.getValue()
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElseThrow();
                SharedMetricRegistries.getDefault().counter(getName() + ".roll." + diceExpression).inc();
                SharedMetricRegistries.getDefault().counter(getName() + ".roll").inc();
                DiceResult result = DiceParserHelper.rollWithDiceParser(diceExpression);

                log.info("Roll {}: {} in channel {}", getName(), result.getResultTitle(), event.getInteraction().getChannelId().asLong());
                return event.reply("...")
                        .onErrorResume(t -> {
                            log.error("Error on replay to slash command", t);
                            return Mono.empty();
                        })
                        .then(event.getInteraction().getChannel().ofType(TextChannel.class)
                                .flatMap(channel -> {
                                            return createEmbedMessageWithReference(channel, result.getResultTitle(), result.getResultDetails(), event.getInteraction().getMember().orElseThrow())
                                                    .retry(3);//not sure way this is needed but sometimes we get Connection reset in the event acknowledge and then here an error
                                        }
                                ))
                        .then();

            }

        }
        return Mono.empty();
    }
}
