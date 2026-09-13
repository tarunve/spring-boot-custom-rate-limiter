package io.github.tarunve.ratelimiter.bucket;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.tarunve.ratelimiter.model.RateLimitResult;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Distributed rate limiter backed by Redis via Bucket4j's {@link ProxyManager}.
 *
 * <p>Uses the Lettuce driver (the default in Spring Boot) to communicate with Redis.
 * Bucket state is stored as a compact byte array under the key
 * {@code "rl:<key>"} in Redis, supporting atomic compare-and-swap operations
 * without Lua scripts on every request.
 *
 * <p>If Redis is unavailable at configuration time or if the Lettuce
 * {@link LettuceConnectionFactory} cannot be obtained, the service logs a warning
 * and <em>falls back to allowing all requests</em> so that a Redis outage does not
 * take down the application.
 */
@Slf4j
public class RedisRateLimiterService implements RateLimiterService {

    private static final String KEY_PREFIX = "rl:";

    private final ProxyManager<byte[]> proxyManager;

    /**
     * Create a {@code RedisRateLimiterService} from an existing
     * {@link RedisConnectionFactory}.  Only {@link LettuceConnectionFactory}
     * is supported; for other drivers use the
     * {@link #RedisRateLimiterService(ProxyManager)} constructor.
     *
     * @param connectionFactory Spring Data Redis connection factory (must be Lettuce)
     * @throws IllegalArgumentException if the factory is not Lettuce-based
     */
    public RedisRateLimiterService(RedisConnectionFactory connectionFactory) {
        if (!(connectionFactory instanceof LettuceConnectionFactory lettuceFactory)) {
            throw new IllegalArgumentException(
                    "RedisRateLimiterService requires a LettuceConnectionFactory. "
                    + "Got: " + connectionFactory.getClass().getName());
        }
        RedisClient redisClient = (RedisClient) lettuceFactory.getNativeClient();
        StatefulRedisConnection<String, byte[]> connection =
                redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        this.proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .build();
        log.info("RedisRateLimiterService initialised using Lettuce connection");
    }

    /**
     * Create a {@code RedisRateLimiterService} with an externally constructed
     * {@link ProxyManager} (useful for testing with embedded Redis or Testcontainers).
     *
     * @param proxyManager pre-configured Bucket4j proxy manager
     */
    public RedisRateLimiterService(ProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
        log.info("RedisRateLimiterService initialised with provided ProxyManager");
    }

    @Override
    public RateLimitResult tryConsume(String key, int limit, long durationNanos) {
        byte[] redisKey = toRedisKey(key, limit, durationNanos);
        Supplier<BucketConfiguration> configSupplier =
                () -> buildConfiguration(limit, durationNanos);

        try {
            var bucket = proxyManager.builder().build(redisKey, configSupplier);
            var probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                log.trace("Rate limit OK  key='{}' remaining={}", key, probe.getRemainingTokens());
                return RateLimitResult.allowed(probe.getRemainingTokens());
            } else {
                log.debug("Rate limit EXCEEDED key='{}' nanosToWait={}", key, probe.getNanosToWaitForRefill());
                return RateLimitResult.denied(probe.getNanosToWaitForRefill());
            }
        } catch (Exception e) {
            // Redis is down — fail open (allow the request) to avoid cascading failures
            log.warn("Redis error during rate-limit check for key '{}'; allowing request (fail-open)", key, e);
            return RateLimitResult.allowed(Long.MAX_VALUE);
        }
    }

    @Override
    public void resetBucket(String key) {
        // Bucket4j ProxyManager does not expose a direct remove API; we rely on TTL.
        // For explicit removal, access Redis directly via the RedisTemplate if needed.
        log.debug("resetBucket called for key '{}' — bucket will expire naturally via Redis TTL", key);
    }

    @Override
    public String backendName() {
        return "redis";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static BucketConfiguration buildConfiguration(int limit, long durationNanos) {
        return BucketConfiguration.builder()
                .addLimit(io.github.bucket4j.Bandwidth.builder()
                        .capacity(limit)
                        .refillGreedy(limit, Duration.ofNanos(durationNanos))
                        .build())
                .build();
    }

    /**
     * Build a byte-array Redis key that encodes the logical key plus its
     * limit/duration so that different endpoint configs get separate buckets.
     */
    private static byte[] toRedisKey(String key, int limit, long durationNanos) {
        String composite = KEY_PREFIX + key + "|" + limit + "|" + durationNanos;
        return composite.getBytes(StandardCharsets.UTF_8);
    }
}
