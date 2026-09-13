package io.github.tarunve.ratelimiter.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the distributed rate limiter.
 *
 * <p>Bind via {@code application.yml} / {@code application.properties}:
 * <pre>{@code
 * rate-limiter:
 *   enabled: true
 *   backend: IN_MEMORY          # IN_MEMORY or REDIS
 *   default-limit: 100
 *   default-duration: 60        # seconds
 *   global-rules:
 *     - pattern: /api/public/**
 *       limit: 200
 *       duration: 60
 *     - pattern: /api/admin/**
 *       limit: 20
 *       duration: 60
 * }</pre>
 */
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimitProperties {

    /** Master switch.  Set to {@code false} to disable all rate limiting. */
    private boolean enabled = true;

    /**
     * Default token-bucket capacity (requests per window).
     * Used when no {@code @RateLimit} annotation is present and a global rule
     * matched the request path.
     */
    private int defaultLimit = 100;

    /**
     * Default window duration in <em>seconds</em>.
     * Used together with {@link #defaultLimit} for unannotated endpoints.
     */
    private long defaultDuration = 60;

    /** Storage backend to use. Defaults to {@link Backend#IN_MEMORY}. */
    private Backend backend = Backend.IN_MEMORY;

    /**
     * Optional list of global path-based rate-limit rules applied by the
     * {@code RateLimitInterceptor}.  Rules are evaluated in order; the first
     * matching pattern wins.
     */
    private List<GlobalRule> globalRules = new ArrayList<>();

    // -------------------------------------------------------------------------

    /**
     * Selects the rate-limit storage backend.
     */
    public enum Backend {
        /** Local JVM — suitable for single-instance deployments or testing. */
        IN_MEMORY,
        /**
         * Redis — suitable for distributed / multi-instance deployments.
         * Requires {@code spring-boot-starter-data-redis} on the classpath and a
         * configured {@code RedisConnectionFactory}.
         */
        REDIS
    }

    // -------------------------------------------------------------------------

    /**
     * A single global rate-limit rule applied to URL patterns before reaching
     * any controller.
     */
    @Data
    @NoArgsConstructor
    public static class GlobalRule {

        /**
         * Ant-style URL pattern (e.g., {@code /api/public/**},
         * {@code /api/v1/search}).
         */
        private String pattern;

        /**
         * Maximum requests allowed within the {@link #duration} window.
         * Falls back to {@link RateLimitProperties#getDefaultLimit()} when {@code 0}.
         */
        private int limit = 0;

        /**
         * Window duration in seconds.
         * Falls back to {@link RateLimitProperties#getDefaultDuration()} when {@code 0}.
         */
        private long duration = 0;

        /**
         * Effective limit — returns the rule-level limit if set, otherwise
         * the supplied default.
         */
        public int effectiveLimit(int defaultLimit) {
            return limit > 0 ? limit : defaultLimit;
        }

        /**
         * Effective duration — returns the rule-level duration if set, otherwise
         * the supplied default.
         */
        public long effectiveDuration(long defaultDuration) {
            return duration > 0 ? duration : defaultDuration;
        }
    }
}
