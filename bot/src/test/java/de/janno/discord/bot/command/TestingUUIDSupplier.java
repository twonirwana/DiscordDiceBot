package de.janno.discord.bot.command;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class TestingUUIDSupplier implements Supplier<UUID> {
    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public UUID get() {
        byte[] bytes = new byte[16];
        ByteBuffer.wrap(bytes).putInt(counter.getAndIncrement());
        return UUID.nameUUIDFromBytes(bytes);
    }
}
