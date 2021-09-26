package de.janno.discord.persistance;

import reactor.core.publisher.FluxSink;

import java.util.function.Consumer;

public class FluxSinkConsumer<T> implements Consumer<T> {

    private Consumer<T> fluxConsumerAdapter;

    public void registerToSink(FluxSink<Object> sink) {
        fluxConsumerAdapter = t -> {
            if (t != null) {
                sink.next(t);
            }
        };
    }

    @Override
    public void accept(T t) {
        fluxConsumerAdapter.accept(t);
    }
}
