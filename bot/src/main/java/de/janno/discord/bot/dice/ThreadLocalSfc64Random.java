package de.janno.discord.bot.dice;

import org.jetbrains.annotations.NotNull;

import java.lang.ThreadLocal;
import java.util.random.RandomGenerator;

/***
 * A thread-local Sfc64Random generator implemented with a random seed.
 * Objects can be created, but won't be distinct generators.
 */
public class ThreadLocalSfc64Random implements RandomGenerator {
    private static final ThreadLocal<Sfc64Random> localRandom = new ThreadLocal<>();

    /***
     * Gets this thread's local Sfc64Random. If it doesn't exist, creates a new one.
     * @return This thread's local Sfc64Random.
     */
    @NotNull
    private Sfc64Random getLocalRandom() {
        final Sfc64Random currentLocalRandom = localRandom.get();

        if (currentLocalRandom == null) {
            final Sfc64Random newLocalRandom = new Sfc64Random();
            localRandom.set(newLocalRandom);
            return newLocalRandom;
        }

        return currentLocalRandom;
    }

    // Wrapper functions.
    // Other functions will of course work, but these are functions that
    // are implemented more efficiently by Sfc64Random.
    public long nextLong() {
        return getLocalRandom().nextLong();
    }

    public long nextLong(long bound) {
        return getLocalRandom().nextLong(bound);
    }

    public long nextLong(long origin, long bound) {
        return getLocalRandom().nextLong(origin, bound);
    }

    public int nextInt(int bound) {
        return getLocalRandom().nextInt(bound);
    }

    public int nextInt(int origin, int bound) {
        return getLocalRandom().nextInt(origin, bound);
    }

    public ThreadLocalSfc64Random() {
        // Might as well create the random generator, as this thread will use it at some point.
        getLocalRandom();
    }
}
