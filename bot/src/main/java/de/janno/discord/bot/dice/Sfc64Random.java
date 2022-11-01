package de.janno.discord.bot.dice;

import java.util.random.RandomGenerator;
import java.security.SecureRandom;

/*** An implementation of the SFC64 random number generator by Chris Doty-Humphrey.
 * Based on the implementation by M. E. O'Neill (https://gist.github.com/imneme/f1f7821f07cf76504a97f6537c818083)
 * who in turn derived it from Chris Doty-Humphrey's PractRand (https://pracrand.sourceforge.net/)
 * Not thread-safe, use ThreadLocalSfc64Random for that.
 */
public class Sfc64Random implements RandomGenerator {
    private long state_a;
    private long state_b;
    private long state_c;
    private long counter;

    public long nextLong() {
        final long result = state_a + state_b + counter++;

        state_a = state_b ^ (state_b >>> 11);
        state_b = state_c + (state_c << 3);
        state_c = result + Long.rotateLeft(state_c, 24);

        return result;
    }

    /* Generate a long within a particular unsigned bound.
     * Used internally to implement nextLong(long) and nextLong(long, long).
     * This particular method for generating a random long is also from
     * M. E. O'Neill (https://www.pcg-random.org/posts/bounded-rands.html) and
     * apparently came from Apple. It does not require multiplication or
     * division.
     */
    private long nextLongUnsigned(long bound) {
        if (bound == 0)
            throw new IllegalArgumentException("bound cannot be 0");

        final long adjustedBound = bound - 1;

        // The all 1's bit pattern, shifted appropriately.
        // The |1 is to ensure that the operand to number of leading zeroes
        // is never 0. If a bound of 1 were passed, the mask would be left
        // alone, most likely hanging the application.
        final long mask = ~0 >>> (Long.numberOfLeadingZeros(adjustedBound|1));

        long candidate;
        do {
            candidate = nextLong() & mask;
        } while (Long.compareUnsigned(candidate, adjustedBound) > 0);

        return candidate;
    }

    public long nextLong(long bound) {
        if (bound <= 0)
            throw new IllegalArgumentException("bound must be positive");

        return nextLongUnsigned(bound);
    }

    public long nextLong(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }

        // NB: Target is unsigned.
        final long target = bound - origin;

        if (target == -1L) {
            return nextLong();
        } else {
            return origin + nextLongUnsigned(target + 1L);
        }
    }

    // int functions. There is no need to implement nextInt() in terms of
    // nextLong() as the default implementation is the exact same.
    public int nextInt(int bound) {
        return (int)nextLong(bound);
    }

    public int nextInt(int origin, int bound) {
        return (int)nextLong(origin, bound);
    }

    private void doSeed(long seed_c, long seed_b, long seed_a) {
        state_a = seed_a;
        state_b = seed_b;
        state_c = seed_c;
        counter = 1;

        // Scramble the state a little.
        for (int i = 0; i < 20; i++) {
            nextLong();
        }
    }

    // Both of these simply add seed extender values. For cases when there are less
    // than 3 longs of seed data, PractRand encourages the order C, B, A.
    private void doSeed(long seed_c, long seed_b) {
        doSeed(seed_c, seed_b, 3141592653589793L);
    }

    private void doSeed(long seed) {
        doSeed(seed, 0xDABEEF5EEDC00L);
    }

    public Sfc64Random(long seed, long seed2, long seed3) {
        doSeed(seed, seed2, seed3);
    }

    public Sfc64Random(long seed, long seed2) {
        doSeed(seed, seed2);
    }

    public Sfc64Random(long seed) {
        doSeed(seed);
    }

    public Sfc64Random() {
        final SecureRandom seed_source = new SecureRandom();

        doSeed(seed_source.nextLong(), seed_source.nextLong(), seed_source.nextLong());
    }
}
