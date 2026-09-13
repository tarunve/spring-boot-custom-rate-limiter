package io.github.tarunve.ratelimiter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a rate limit is exceeded.
 *
 * <p>Annotated with {@code @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)} so
 * Spring MVC automatically returns HTTP 429 when this exception propagates
 * out of a controller method.  The aspect also sets the
 * {@code Retry-After} and {@code X-RateLimit-*} headers before throwing.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {

    private final String rateLimitKey;
    private final long nanosToWait;

    /**
     * @param rateLimitKey  the key that was rate-limited (IP, username, …)
     * @param nanosToWait   nanoseconds until the next token is available
     */
    public RateLimitExceededException(String rateLimitKey, long nanosToWait) {
        super(String.format(
                "Rate limit exceeded for key '%s'. Retry after %d ms.",
                rateLimitKey,
                nanosToWait / 1_000_000));
        this.rateLimitKey = rateLimitKey;
        this.nanosToWait = nanosToWait;
    }

    /** The rate-limit key that triggered the exception. */
    public String getRateLimitKey() {
        return rateLimitKey;
    }

    /**
     * Nanoseconds to wait before the next request will succeed.
     * Convert to seconds with {@code nanosToWait / 1_000_000_000L} for a
     * {@code Retry-After} header value.
     */
    public long getNanosToWait() {
        return nanosToWait;
    }

    /** Convenience: seconds to wait (ceiling), suitable for Retry-After header. */
    public long getRetryAfterSeconds() {
        return (nanosToWait + 999_999_999L) / 1_000_000_000L;
    }
}
