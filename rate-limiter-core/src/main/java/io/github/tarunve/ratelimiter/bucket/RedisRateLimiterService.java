package io.github.tarunve.ratelimiter.bucket;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.tarunve.ratelimiter.model.RateLimitResult;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
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
 * Bucket state is stored as a compact byte array under a composite key in Redis,
 * supporting atomic compare-and-swap operations without Lua scripts on every request.
 *
 * <p>If Redis is unavailable, the service logs a warning and fails open
 * (allows all requests) so that a Redis outage does not take down the application.
 */
@Slf4j
public class RedisRateLimiterService implements RateLimiterService {

    private static final String KEY_PREFIX = "rl:";

    private final ProxyManager<byte[]> proxyManager;

    /**
     * Create a {@code RedisRateLimiterService} from an existing
     * {@link RedisConnectionFactory}. Only {@link LettuceConnectionFactory}
     * is supported.
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
        // Use byte[]-to-byte[] codec so ProxyManager<byte[]> is satisfied
        StatefulRedisConnection<byte[], byte[]> connection =
                redisClient.connect(ByteArrayCodec.INSTANCE);
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
            log.warn("Redis error during rate-limit check for key '{}!'; allowing request (fail-open)", key, e);
            return RateLimitResult.allowed(Long.MAX_VALUE);
        }
    }

    @Override
    public void resetBucket(String key) {
        log.debug("resetBucket called for key '{}' — bucket will expire naturally via Redis TTL", key);
    }

    @Override
    public String backendName() {
        return "redis";
    }

    private static BucketConfiguration buildConfiguration(int limit, long durationNanos) {
        return BucketConfiguration.builder()
                .addLimit(io.github.bucket4j.Bandwidth.builder()
                        .capacity(limit)
                        .refillGreedy(limit, Duration.ofNanos(durationNanos))
                        .build())
                .build();
    }

    private static byte[] toRedisKey(String key, int limit, long durationNanos) {
        String composite = KEY_PREFIX + key + "|" + limit + "|" + durationNanos;
        return composite.getBytes(StandardCharsets.UTF_8);
    }
}
