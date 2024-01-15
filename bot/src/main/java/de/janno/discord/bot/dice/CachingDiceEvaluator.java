package de.janno.discord.bot.dice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.janno.evaluator.dice.DiceEvaluator;
import de.janno.evaluator.dice.ExpressionException;
import de.janno.evaluator.dice.Roller;
import de.janno.evaluator.dice.random.NumberSupplier;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

@Slf4j
public class CachingDiceEvaluator {

    private final LoadingCache<String, RollerOrError> diceRollerCache;
    private final DiceEvaluator diceEvaluator;

    public CachingDiceEvaluator(NumberSupplier numberSupplier, int maxNumberOfDice, int cacheSize) {
        diceEvaluator = new DiceEvaluator(numberSupplier, maxNumberOfDice);
        diceRollerCache = CacheBuilder.newBuilder()
                .maximumSize(cacheSize)
                .recordStats()
                .build(new CacheLoader<>() {
                    @Override
                    public @NonNull RollerOrError load(@NonNull String expression) {
                        try {
                            log.debug("create roller for: {}", expression.replace("\n"," "));
                            Roller roller = diceEvaluator.buildRollSupplier(expression);
                            roller.roll();
                            return new RollerOrError(expression, roller, true, null);
                        } catch (ExpressionException | ArithmeticException e) {
                            return new RollerOrError(expression, null, false, e.getMessage());
                        }
                    }
                });
        Gauge.builder("diceEvaluator.cache", diceRollerCache::size).tags(Tags.of("stats", "size")).register(globalRegistry);
        Gauge.builder("diceEvaluator.cache", () -> diceRollerCache.stats().requestCount()).tags(Tags.of("stats", "requests")).register(globalRegistry);
        Gauge.builder("diceEvaluator.cache", () -> diceRollerCache.stats().hitCount()).tags(Tags.of("stats", "hit")).register(globalRegistry);
        Gauge.builder("diceEvaluator.cache", () -> diceRollerCache.stats().missCount()).tags(Tags.of("stats", "miss")).register(globalRegistry);
        Gauge.builder("diceEvaluator.cache", () -> diceRollerCache.stats().totalLoadTime()).tags(Tags.of("stats", "totalLoadTime")).register(globalRegistry);
        Gauge.builder("diceEvaluator.cache", () -> diceRollerCache.stats().averageLoadPenalty()).tags(Tags.of("stats", "averageLoadTime")).register(globalRegistry);
    }

    public @NonNull RollerOrError get(@NonNull String expression) {
        return diceRollerCache.getUnchecked(expression);
    }

    @VisibleForTesting
    public long getCacheSize() {
        return diceRollerCache.size();
    }
}
