package de.janno.discord.bot.dice;

import de.janno.evaluator.dice.NumberSupplier;

/** An implementation of NumberSupplier using ThreadLocalSfc64Random.
 * The interesting pieces are implemented elsewhere.
 */
public class Sfc64RandomNumberSupplier implements NumberSupplier {
    static final ThreadLocalSfc64Random randomSource = new ThreadLocalSfc64Random();

    public int get(int minExcl, int maxIncl) {
        return randomSource.nextInt(minExcl + 1, maxIncl);
    }

}
