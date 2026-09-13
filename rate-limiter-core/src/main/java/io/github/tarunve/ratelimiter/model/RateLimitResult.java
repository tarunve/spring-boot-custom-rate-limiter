package io.github.tarunve.ratelimiter.model;

/**
 * Encapsulates the outcome of a single rate-limit check.
 *
 * <p>Returned by {@code RateLimiterService#tryConsume} and used by the aspect
 * to decide whether to allow or reject the request and to populate
 * {@code X-RateLimit-*} response headers.
 */
public final class RateLimitResult {

    private final boolean allowed;
    private final long remainingTokens;
    private final long nanosToWaitForRefill;

    private RateLimitResult(boolean allowed, long remainingTokens, long nanosToWaitForRefill) {
        this.allowed = allowed;
        this.remainingTokens = remainingTokens;
        this.nanosToWaitForRefill = nanosToWaitForRefill;
    }

    /**
     * Factory: request was allowed.
     *
     * @param remainingTokens tokens left in the bucket after this consumption
     */
    public static RateLimitResult allowed(long remainingTokens) {
        return new RateLimitResult(true, remainingTokens, 0L);
    }

    /**
     * Factory: request was denied.
     *
     * @param nanosToWaitForRefill nanoseconds until a token becomes available again
     */
    public static RateLimitResult denied(long nanosToWaitForRefill) {
        return new RateLimitResult(false, 0L, nanosToWaitForRefill);
    }

    /** {@code true} if the request should be allowed to proceed. */
    public boolean isAllowed() {
        return allowed;
    }

    /**
     * Number of tokens remaining in the bucket after this request.
     * Meaningful only when {@link #isAllowed()} is {@code true}.
     */
    public long getRemainingTokens() {
        return remainingTokens;
    }

    /**
     * Nanoseconds until the next token will be available.
     * Meaningful only when {@link #isAllowed()} is {@code false}.
     * Use this to populate the {@code Retry-After} header.
     */
    public long getNanosToWaitForRefill() {
        return nanosToWaitForRefill;
    }

    /** Convenience: seconds to wait (ceiling) for {@code Retry-After} header. */
    public long getRetryAfterSeconds() {
        return (nanosToWaitForRefill + 999_999_999L) / 1_000_000_000L;
    }

    @Override
    public String toString() {
        return "RateLimitResult{allowed=" + allowed
                + ", remainingTokens=" + remainingTokens
                + ", nanosToWaitForRefill=" + nanosToWaitForRefill + '}';
    }
}
