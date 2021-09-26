package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.persistance.Trigger;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ComponentInteractEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

import static de.janno.discord.DiscordMessageUtils.*;

@Slf4j
public abstract class AbstractCommand<T> implements ISlashCommand, IComponentInteractEventHandler {

    protected static final String ACTION_STOP = "stop";
    protected static final String ACTION_START = "start";
    protected final ConfigRegistry<T> configRegistry;
    protected final Snowflake botUserId;

    protected AbstractCommand(ConfigRegistry<T> configRegistry, Snowflake botUserId) {
        this.configRegistry = configRegistry;
        this.botUserId = botUserId;
    }

    protected List<ApplicationCommandOptionData> getStartOptions() {
        return ImmutableList.of();
    }

    protected abstract String getCommandDescription();

    @Override
    public ApplicationCommandRequest getApplicationCommand() {
        return ApplicationCommandRequest.builder()
                .name(getName())
                .description(getCommandDescription())
                .addOption(ApplicationCommandOptionData.builder()
                        .name(ACTION_START)
                        .description("Start")
                        .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                        .addAllOptions(getStartOptions())
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name(ACTION_STOP)
                        .description("Stop")
                        .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                        .build()
                )
                .build();
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull ComponentInteractEvent event) {
        if (!configRegistry.channelIsRegistered(event.getInteraction().getChannelId())) {
            return Mono.empty();
        }
        return Mono.just(event)
                .map(InteractionCreateEvent::getInteraction)
                .flatMap(i -> Mono.justOrEmpty(i.getMessage()))
                .filter(m -> botUserId.equals(m.getAuthor().map(User::getId).orElse(null)))
                .filter(m -> getButtonMessage().equals(m.getContent())) //otherwise we react to other buttons Todo use list of existing button ids, not the message but only if the buttonIds are persisted
                .flatMap(buttonMessage -> event
                        .acknowledge() //don't edit the message, we use it to identify the message
                        .onErrorResume(t -> {
                            log.error("Error on acknowledge button event", t);
                            return Mono.empty();
                        })
                        .then(buttonMessage.getChannel()
                                .ofType(TextChannel.class)
                                .flatMap(channel -> {
                                            DiceResult result = rollDice(channel.getId(), event.getCustomId());
                                            return createEmbedMessageWithReference(channel, result.getResultTitle(), result.getResultDetails(), event.getInteraction().getMember().orElseThrow())
                                                    .retry();//not sure way this is needed but sometimes we get Connection reset in the event acknowledge and then here an error
                                        }
                                )
                        ).then(buttonMessage.getChannel()
                                .ofType(TextChannel.class)
                                .flatMap(createButtonMessage(configRegistry, getButtonMessage(), getButtonLayout()))
                                .flatMap(m -> deleteMessage(m.getChannel(), m.getChannelId(), configRegistry, m.getId()))
                        )
                );
    }

    public Mono<Trigger> handleSlashCommandEvent(@NonNull SlashCommandEvent event) {
        if (getName().equals(event.getCommandName())) {
            if (event.getOption(ACTION_STOP).isPresent()) {
                configRegistry.removeChannel(event.getInteraction().getChannelId());
                return event.reply()
                        .withContent("Stop " + getName() + " in channel")
                        .then(event.getInteraction()
                                .getChannel()
                                .ofType(TextChannel.class)
                                .flatMap(tc -> tc.createMessage(MessageCreateSpec.builder() //needed to have a messageId to remove all bot messages before
                                        .content("Removing all buttons in channel")
                                        //todo add messageReference
                                        .build()))
                                .flatMap(m -> deleteAllButtonMessagesOfTheBot(m.getChannel().ofType(TextChannel.class), m.getId(), botUserId, getButtonMessage()).then()))
                        .then(Mono.just(Trigger.SAVE));

            } else if (event.getOption(ACTION_START).isPresent()) {
                ApplicationCommandInteractionOption options = event.getOption(ACTION_START).get();
                T config = configRegistry.getConfigForChannelOrDefault(event.getInteraction().getChannelId(), createConfig());
                config = setConfigValuesFromStartOptions(options, config);
                configRegistry.setChannelConfig(event.getInteraction().getChannelId(), config);
                String startReplay = String.format("Start %s in channel", getName());
                if (config != null) {
                    startReplay = startReplay + ". " + config;
                }
                return event.reply(startReplay)
                        .then(event.getInteraction().getChannel().ofType(TextChannel.class)
                                .flatMap(createButtonMessage(configRegistry, getButtonMessage(), getButtonLayout())))
                        .then(Mono.just(Trigger.SAVE));

            }

        }
        return Mono.empty();
    }

    protected abstract String getButtonMessage();

    protected abstract T createConfig();

    protected abstract T setConfigValuesFromStartOptions(ApplicationCommandInteractionOption options, T config);

    protected abstract DiceResult rollDice(Snowflake channelId, String buttonId);

    protected List<LayoutComponent> getButtonLayout() {
        return ImmutableList.of(
                ActionRow.of(
                        //              ID,  label
                        Button.primary("1", "1"),
                        Button.primary("2", "2"),
                        Button.primary("3", "3"),
                        Button.primary("4", "4"),
                        Button.primary("5", "5")
                ),
                ActionRow.of(
                        Button.primary("6", "6"),
                        Button.primary("7", "7"),
                        Button.primary("8", "8"),
                        Button.primary("9", "9"),
                        Button.primary("10", "10")
                ),
                ActionRow.of(
                        Button.primary("11", "11"),
                        Button.primary("12", "12"),
                        Button.primary("13", "13"),
                        Button.primary("14", "14"),
                        Button.primary("15", "15")
                ));
    }
}
