package io.github.tarunve.ratelimiter.bucket;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.tarunve.ratelimiter.model.RateLimitResult;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process, thread-safe rate limiter backed by Bucket4j token buckets.
 *
 * <p>A separate {@link Bucket} is maintained per key in a
 * {@link ConcurrentHashMap}.  Buckets are created lazily on first access and
 * are never evicted — suitable for a bounded set of keys (IPs, users).
 * For production deployments with an unbounded key space, prefer the
 * {@link RedisRateLimiterService} or add a Caffeine/Guava cache in front of
 * this class.
 *
 * <p>The token-bucket parameters are encoded into the map key so that the
 * same client key with different limits (e.g., two endpoints) gets distinct
 * buckets.
 */
@Slf4j
public class InMemoryRateLimiterService implements RateLimiterService {

    /**
     * key → bucket.  The composite map key includes the limit+duration so that
     * the same logical key used at two different endpoints gets independent buckets.
     */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryConsume(String key, int limit, long durationNanos) {
        Bucket bucket = buckets.computeIfAbsent(
                compositeKey(key, limit, durationNanos),
                k -> buildBucket(limit, durationNanos));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            log.trace("Rate limit OK  key='{}' remaining={}", key, probe.getRemainingTokens());
            return RateLimitResult.allowed(probe.getRemainingTokens());
        } else {
            log.debug("Rate limit EXCEEDED key='{}' nanosToWait={}", key, probe.getNanosToWaitForRefill());
            return RateLimitResult.denied(probe.getNanosToWaitForRefill());
        }
    }

    @Override
    public void resetBucket(String key) {
        // Remove all entries that start with this key prefix (any limit/duration combination)
        buckets.keySet().removeIf(k -> k.startsWith(key + "|"));
        log.debug("Bucket reset for key prefix '{}'", key);
    }

    @Override
    public String backendName() {
        return "in-memory";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Build a token-bucket with the given capacity and a greedy refill that
     * restores {@code limit} tokens every {@code durationNanos} nanoseconds.
     */
    private static Bucket buildBucket(int limit, long durationNanos) {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(limit)
                .refillGreedy(limit, Duration.ofNanos(durationNanos))
                .build();
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    /**
     * Encode limit and duration into the map key so that the same logical key
     * at different endpoints gets independent buckets.
     */
    private static String compositeKey(String key, int limit, long durationNanos) {
        return key + "|" + limit + "|" + durationNanos;
    }
}
