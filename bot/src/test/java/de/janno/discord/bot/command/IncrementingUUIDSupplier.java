package de.janno.discord.bot.command;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class IncrementingUUIDSupplier implements Supplier<UUID> {

    private final AtomicInteger counter = new AtomicInteger(0);

    private IncrementingUUIDSupplier() {
    }

    public static Supplier<UUID> create() {
        return new IncrementingUUIDSupplier();
    }

    @Override
    public UUID get() {
        return UUID.fromString("00000000-0000-0000-0001-%012d".formatted(counter.getAndIncrement()));
    }
}
