package io.github.tarunve.ratelimiter.bucket;

import io.github.tarunve.ratelimiter.model.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InMemoryRateLimiterService}.
 *
 * <p>These tests use the real Bucket4j implementation with short windows so
 * refill behaviour can be verified without slow sleeps where possible.
 * A 50 ms window is used for the refill test, which is acceptable for a
 * unit test suite.
 */
@DisplayName("InMemoryRateLimiterService")
class InMemoryRateLimiterServiceTest {

    private InMemoryRateLimiterService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryRateLimiterService();
    }

    // -------------------------------------------------------------------------
    // Tests: tokens consumed correctly
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("token consumption")
    class TokenConsumption {

        @Test
        @DisplayName("first request on a new key is allowed and decrements remaining tokens by 1")
        void firstRequest_isAllowedAndDecrementsRemaining() {
            int limit = 5;
            long durationNanos = TimeUnit.SECONDS.toNanos(60);

            RateLimitResult result = service.tryConsume("key-1", limit, durationNanos);

            assertThat(result.isAllowed()).isTrue();
            assertThat(result.getRemainingTokens()).isEqualTo(4L); // 5 - 1 = 4
        }

        @Test
        @DisplayName("each successive allowed request decrements remaining tokens")
        void successiveRequests_decrementsRemainingTokens() {
            int limit = 3;
            long durationNanos = TimeUnit.SECONDS.toNanos(60);

            RateLimitResult r1 = service.tryConsume("key-dec", limit, durationNanos);
            RateLimitResult r2 = service.tryConsume("key-dec", limit, durationNanos);
            RateLimitResult r3 = service.tryConsume("key-dec", limit, durationNanos);

            assertThat(r1.isAllowed()).isTrue();
            assertThat(r1.getRemainingTokens()).isEqualTo(2L);

            assertThat(r2.isAllowed()).isTrue();
            assertThat(r2.getRemainingTokens()).isEqualTo(1L);

            assertThat(r3.isAllowed()).isTrue();
            assertThat(r3.getRemainingTokens()).isEqualTo(0L);
        }

        @Test
        @DisplayName("backendName returns 'in-memory'")
        void backendName_returnsInMemory() {
            assertThat(service.backendName()).isEqualTo("in-memory");
        }
    }

    // -------------------------------------------------------------------------
    // Tests: rate limit exceeded after N requests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("rate limit exceeded")
    class RateLimitExceeded {

        @Test
        @DisplayName("request immediately after exhausting all tokens is denied")
        void afterExhausting_nextRequestIsDenied() {
            int limit = 2;
            long durationNanos = TimeUnit.SECONDS.toNanos(60);

            // Exhaust the bucket
            service.tryConsume("key-exhaust", limit, durationNanos);
            service.tryConsume("key-exhaust", limit, durationNanos);

            RateLimitResult denied = service.tryConsume("key-exhaust", limit, durationNanos);

            assertThat(denied.isAllowed()).isFalse();
            assertThat(denied.getNanosToWaitForRefill()).isPositive();
        }

        @Test
        @DisplayName("denied result carries a positive nanosToWaitForRefill")
        void denied_nanosToWaitIsPositive() {
            int limit = 1;
            long durationNanos = TimeUnit.SECONDS.toNanos(60);

            service.tryConsume("key-wait", limit, durationNanos); // consumes the only token

            RateLimitResult result = service.tryConsume("key-wait", limit, durationNanos);

            assertThat(result.isAllowed()).isFalse();
            assertThat(result.getNanosToWaitForRefill()).isGreaterThan(0L);
            // Retry-After should be at most the full window (60 s)
            assertThat(result.getRetryAfterSeconds()).isLessThanOrEqualTo(60L);
        }

        @Test
        @DisplayName("exactly N requests succeed; the (N+1)th is denied")
        void exactlyNRequestsSucceed_thenDenied() {
            int limit = 5;
            long durationNanos = TimeUnit.MINUTES.toNanos(1);
            String key = "key-exact";

            for (int i = 0; i < limit; i++) {
                RateLimitResult r = service.tryConsume(key, limit, durationNanos);
                assertThat(r.isAllowed())
                        .as("Request %d of %d should be allowed", i + 1, limit)
                        .isTrue();
            }

            RateLimitResult overflow = service.tryConsume(key, limit, durationNanos);
            assertThat(overflow.isAllowed()).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Tests: separate buckets per key
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("separate buckets per key")
    class SeparateBuckets {

        @Test
        @DisplayName("two different keys have independent token counts")
        void differentKeys_haveIndependentBuckets() {
            int limit = 2;
            long durationNanos = TimeUnit.SECONDS.toNanos(60);

            // Exhaust key-a
            service.tryConsume("key-a", limit, durationNanos);
            service.tryConsume("key-a", limit, durationNanos);

            // key-b should still have tokens
            RateLimitResult resultB = service.tryConsume("key-b", limit, durationNanos);

            assertThat(resultB.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("same logical key with different limits gets independent buckets")
        void sameKeyDifferentLimits_independentBuckets() {
            long durationNanos = TimeUnit.SECONDS.toNanos(60);
            String key = "shared-key";

            // Exhaust limit=1 bucket
            service.tryConsume(key, 1, durationNanos);
            RateLimitResult deniedWithLimit1 = service.tryConsume(key, 1, durationNanos);

            // Same key but limit=5 bucket — should still be fresh
            RateLimitResult allowedWithLimit5 = service.tryConsume(key, 5, durationNanos);

            assertThat(deniedWithLimit1.isAllowed()).isFalse();
            assertThat(allowedWithLimit5.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("keys for three different IPs each track their own consumption")
        void threeKeys_trackIndependentConsumption() {
            int limit = 3;
            long durationNanos = TimeUnit.SECONDS.toNanos(60);

            // Fully exhaust ip1
            for (int i = 0; i < limit; i++) service.tryConsume("ip1", limit, durationNanos);
            // Partial usage on ip2
            service.tryConsume("ip2", limit, durationNanos);
            // No usage on ip3

            assertThat(service.tryConsume("ip1", limit, durationNanos).isAllowed()).isFalse();
            assertThat(service.tryConsume("ip2", limit, durationNanos).getRemainingTokens()).isEqualTo(1L);
            assertThat(service.tryConsume("ip3", limit, durationNanos).getRemainingTokens()).isEqualTo(2L);
        }
    }

    // -------------------------------------------------------------------------
    // Tests: refill after duration
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("refill after duration")
    class RefillAfterDuration {

        @Test
        @DisplayName("bucket is refilled after the window elapses (greedy refill)")
        void bucket_refilledAfterWindow() throws InterruptedException {
            int limit = 2;
            // Use a very short window so the test does not take long
            long windowMs    = 100L;
            long durationNanos = TimeUnit.MILLISECONDS.toNanos(windowMs);
            String key = "key-refill";

            // Exhaust the bucket
            service.tryConsume(key, limit, durationNanos);
            service.tryConsume(key, limit, durationNanos);
            assertThat(service.tryConsume(key, limit, durationNanos).isAllowed())
                    .as("Should be denied right after exhaustion")
                    .isFalse();

            // Wait for the window to pass — greedy refill restores tokens gradually
            Thread.sleep(windowMs + 50L); // small buffer for scheduler jitter

            RateLimitResult afterRefill = service.tryConsume(key, limit, durationNanos);
            assertThat(afterRefill.isAllowed())
                    .as("Should be allowed after the refill window")
                    .isTrue();
        }

        @Test
        @DisplayName("resetBucket removes all entries for that key prefix")
        void resetBucket_removesEntries() {
            int limit = 1;
            long durationNanos = TimeUnit.SECONDS.toNanos(60);
            String key = "key-reset";

            // Exhaust
            service.tryConsume(key, limit, durationNanos);
            assertThat(service.tryConsume(key, limit, durationNanos).isAllowed()).isFalse();

            // Reset
            service.resetBucket(key);

            // Should be fresh now
            RateLimitResult afterReset = service.tryConsume(key, limit, durationNanos);
            assertThat(afterReset.isAllowed()).isTrue();
        }
    }
}
