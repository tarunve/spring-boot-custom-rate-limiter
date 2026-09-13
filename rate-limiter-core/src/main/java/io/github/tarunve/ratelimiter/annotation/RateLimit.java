package io.github.tarunve.ratelimiter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Marks a REST controller method for rate limiting.
 *
 * <p>Example usage:
 * <pre>{@code
 * @RateLimit(limit = 10, duration = 1, durationUnit = TimeUnit.MINUTES)
 * @GetMapping("/api/search")
 * public ResponseEntity<List<Result>> search(...) { ... }
 *
 * // Rate limit per authenticated user
 * @RateLimit(limit = 50, keyType = RateLimit.KeyType.USER)
 * @PostMapping("/api/orders")
 * public ResponseEntity<Order> createOrder(...) { ... }
 *
 * // Custom SpEL key (e.g., per tenant extracted from header)
 * @RateLimit(limit = 200, keyType = RateLimit.KeyType.CUSTOM,
 *            key = "#request.getHeader('X-Tenant-Id')")
 * @GetMapping("/api/data")
 * public ResponseEntity<Data> getData(HttpServletRequest request) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Maximum number of requests allowed within the {@link #duration()} window.
     * Defaults to 100.
     */
    int limit() default 100;

    /**
     * Duration of the rate-limit window. Combined with {@link #durationUnit()}.
     * Defaults to 60 (seconds).
     */
    long duration() default 60;

    /**
     * Time unit for {@link #duration()}. Defaults to {@link TimeUnit#SECONDS}.
     */
    TimeUnit durationUnit() default TimeUnit.SECONDS;

    /**
     * SpEL expression used to compute the rate-limit key when
     * {@link #keyType()} is {@link KeyType#CUSTOM}.
     * The expression has access to the method arguments by name and to the
     * special {@code #request} variable (HttpServletRequest).
     * Ignored for {@link KeyType#IP} and {@link KeyType#USER}.
     */
    String key() default "";

    /**
     * Strategy used to derive the rate-limit key.
     * Defaults to {@link KeyType#IP}.
     */
    KeyType keyType() default KeyType.IP;

    /**
     * Defines the strategy for building the rate-limit key.
     */
    enum KeyType {
        /** Rate limit per client IP address (default). */
        IP,
        /** Rate limit per authenticated principal name. */
        USER,
        /** Rate limit using the SpEL expression in {@link #key()}. */
        CUSTOM
    }
}
