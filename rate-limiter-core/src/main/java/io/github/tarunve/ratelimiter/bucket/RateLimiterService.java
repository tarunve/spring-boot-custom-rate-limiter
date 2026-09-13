package io.github.tarunve.ratelimiter.bucket;

import io.github.tarunve.ratelimiter.model.RateLimitResult;

/**
 * Strategy interface for rate limiting.
 *
 * <p>Implementations decide whether a request identified by {@code key}
 * is within the configured limits.  Two built-in implementations are
 * provided:
 * <ul>
 *   <li>{@link InMemoryRateLimiterService} — local JVM, Bucket4j token bucket</li>
 *   <li>{@link RedisRateLimiterService} — distributed, Bucket4j + Redis</li>
 * </ul>
 */
public interface RateLimiterService {

    /**
     * Attempt to consume one token from the bucket identified by {@code key}.
     *
     * <p>If the bucket for {@code key} does not exist yet it is created on the
     * fly using {@code limit} tokens capacity and a refill of {@code limit}
     * tokens every {@code durationNanos} nanoseconds.
     *
     * @param key           unique rate-limit key (IP, user, custom)
     * @param limit         maximum number of tokens (= requests) per window
     * @param durationNanos window length in nanoseconds
     * @return a {@link RateLimitResult} carrying the decision and remaining tokens
     */
    RateLimitResult tryConsume(String key, int limit, long durationNanos);

    /**
     * Remove the bucket for the given key if it exists.
     * Useful for testing and for explicit quota resets.
     *
     * @param key the rate-limit key to evict
     */
    default void resetBucket(String key) {
        // no-op by default; implementations may override
    }

    /**
     * Name of this implementation, used in log messages and metrics tags.
     *
     * @return a short, human-readable identifier such as {@code "in-memory"} or {@code "redis"}
     */
    String backendName();
}
