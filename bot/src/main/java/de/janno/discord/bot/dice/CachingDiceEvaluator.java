package de.janno.discord.bot.dice;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.janno.evaluator.dice.DiceEvaluator;
import de.janno.evaluator.dice.ExpressionException;
import de.janno.evaluator.dice.Roller;
import de.janno.evaluator.dice.random.NumberSupplier;
import io.avaje.config.Config;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiFunction;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

@Slf4j
public class CachingDiceEvaluator implements ErrorCatchingDiceEvaluator {

    private final NumberSupplier numberSupplier;
    private LoadingCache<String, RollerOrError> diceRollerCache;
    private DiceEvaluator diceEvaluator;

    @VisibleForTesting
    public CachingDiceEvaluator(BiFunction<Integer, Integer, Integer> numberSupplier) {
        this((minExcl, maxIncl, dieId) -> numberSupplier.apply(minExcl, maxIncl));
    }

    public CachingDiceEvaluator(NumberSupplier numberSupplier) {
        this.numberSupplier = numberSupplier;

        diceEvaluator = createDiceEvaluator();
        Config.onChange(e -> {
            diceEvaluator = createDiceEvaluator();
            log.info("recreate dice evaluator");
        }, "diceEvaluator.maxNumberOfDice", "diceEvaluator.maxNumberOfElements", "diceEvaluator.keepChildrenRolls");

        diceRollerCache = createCache();
        Config.onChange(e -> {
            diceRollerCache = createCache();
            log.info("recreate dice cache");
        }, "diceEvaluator.cacheSize");

        Gauge.builder("diceEvaluator.cache", diceRollerCache::size).tags(Tags.of("stats", "size")).register(globalRegistry);
        Gauge.builder("diceEvaluator.cache", () -> diceRollerCache.stats().requestCount()).tags(Tags.of("stats", "requests")).register(globalRegistry);
        Gauge.builder("diceEvaluator.cache", () -> diceRollerCache.stats().hitCount()).tags(Tags.of("stats", "hit")).register(globalRegistry);
        Gauge.builder("diceEvaluator.cache", () -> diceRollerCache.stats().missCount()).tags(Tags.of("stats", "miss")).register(globalRegistry);
        Gauge.builder("diceEvaluator.cache", () -> diceRollerCache.stats().totalLoadTime()).tags(Tags.of("stats", "totalLoadTime")).register(globalRegistry);
        Gauge.builder("diceEvaluator.cache", () -> diceRollerCache.stats().averageLoadPenalty()).tags(Tags.of("stats", "averageLoadTime")).register(globalRegistry);
    }

    private DiceEvaluator createDiceEvaluator() {
        return new DiceEvaluator(numberSupplier, Config.getInt("diceEvaluator.maxNumberOfDice", 1000),
                Config.getInt("diceEvaluator.maxNumberOfElements", 10_000),
                Config.getBool("diceEvaluator.keepChildrenRolls", false));
    }

    private LoadingCache<String, RollerOrError> createCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(Config.getInt("diceEvaluator.cacheSize", 10_000))
                .recordStats()
                .build(new CacheLoader<>() {
                    @Override
                    public @NonNull RollerOrError load(@NonNull String expression) {
                        try {
                            log.trace("create roller for: {}", expression.replace("\n", " "));
                            Roller roller = diceEvaluator.buildRollSupplier(expression);
                            roller.roll();
                            return new RollerOrError(expression, roller, true, null);
                        } catch (ExpressionException e) {
                            String errorLocation = DiceEvaluatorAdapter.getErrorLocationString(expression, e.getExpressionPosition());
                            //todo full expression or errorLocation
                            return new RollerOrError(errorLocation, null, false, e.getMessage());
                        }
                    }
                });
    }

    public @NonNull RollerOrError get(@NonNull String expression) {
        return diceRollerCache.getUnchecked(expression);
    }

    @VisibleForTesting
    public long getCacheSize() {
        return diceRollerCache.size();
    }
}
