package de.janno.discord.bot.command.hidden;

import com.google.common.base.Stopwatch;
import de.janno.discord.bot.I18n;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.ComponentInteractEventHandler;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HiddenAnswerHandler implements ComponentInteractEventHandler {

    private static final String COMMAND_ID = "hidden_answer";
    private static final String REVEAL_BUTTON_ID = "reveal";


    public static EmbedOrMessageDefinition applyToAnswer(EmbedOrMessageDefinition input, Locale locale) {

        return input.toBuilder()
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .buttonDefinition(ButtonDefinition.builder()
                                .id(BottomCustomIdUtils.createButtonCustomIdWithoutConfigId(COMMAND_ID, REVEAL_BUTTON_ID))
                                .label(I18n.getMessage("h.button.reveal.label", locale)).build())
                        .build()
                )
                .build();

    }


    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull ButtonEventAdaptor event) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        //only one button so we don't check the button id
        return Flux.merge(1,
                        event.acknowledgeAndRemoveButtons(), //ephemeral message cant be deleted
                        event.createResultMessageWithReference(event.getMessageDefinitionOfEventMessageWithoutButtons(), null)
                                .doOnSuccess(v ->
                                        log.info("{}:-> {} in {}ms",
                                                event.getRequester().toLogString(),
                                                REVEAL_BUTTON_ID,
                                                stopwatch.elapsed(TimeUnit.MILLISECONDS)
                                        ))
                )
                .parallel()
                .then();
    }


    @Override
    public @NonNull String getCommandId() {
        return COMMAND_ID;
    }
}
