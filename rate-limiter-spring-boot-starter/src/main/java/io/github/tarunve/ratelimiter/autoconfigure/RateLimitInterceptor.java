package io.github.tarunve.ratelimiter.autoconfigure;

import io.github.tarunve.ratelimiter.bucket.RateLimiterService;
import io.github.tarunve.ratelimiter.config.RateLimitProperties;
import io.github.tarunve.ratelimiter.exception.RateLimitExceededException;
import io.github.tarunve.ratelimiter.model.RateLimitResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC {@link HandlerInterceptor} that applies global rate limits
 * defined in {@link RateLimitProperties#getGlobalRules()}.
 *
 * <p>This interceptor runs <em>before</em> the request reaches any controller.
 * It is complementary to the {@code @RateLimit} aspect — the aspect handles
 * per-method limits while this interceptor handles path-pattern limits that
 * apply uniformly across all matching URLs regardless of the controller.
 *
 * <p>The rate-limit key is built from the client IP ({@code X-Forwarded-For}
 * if present, otherwise {@link HttpServletRequest#getRemoteAddr()}) combined
 * with the matched URL pattern to give per-IP, per-pattern isolation.
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String HEADER_REMAINING   = "X-RateLimit-Remaining";
    private static final String HEADER_RETRY_AFTER = "X-RateLimit-Retry-After";
    private static final String X_FORWARDED_FOR    = "X-Forwarded-For";

    private final RateLimiterService      rateLimiterService;
    private final RateLimitProperties     properties;
    private final AntPathMatcher          pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String requestUri = request.getRequestURI();

        for (RateLimitProperties.GlobalRule rule : properties.getGlobalRules()) {
            if (pathMatcher.match(rule.getPattern(), requestUri)) {
                int  limit    = rule.effectiveLimit(properties.getDefaultLimit());
                long duration = rule.effectiveDuration(properties.getDefaultDuration());
                long durationNanos = duration * 1_000_000_000L;

                String ip  = resolveIp(request);
                String key = "global:" + rule.getPattern() + ":" + ip;

                log.debug("Global rate-limit check: pattern='{}' key='{}' limit={} duration={}s",
                        rule.getPattern(), key, limit, duration);

                RateLimitResult result = rateLimiterService.tryConsume(key, limit, durationNanos);

                if (result.isAllowed()) {
                    response.setHeader(HEADER_REMAINING, String.valueOf(result.getRemainingTokens()));
                    return true; // first matching rule wins
                } else {
                    response.setHeader(HEADER_REMAINING,   "0");
                    response.setHeader(HEADER_RETRY_AFTER, String.valueOf(result.getRetryAfterSeconds()));
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.getWriter().write(
                            "{\"error\":\"Too Many Requests\",\"retryAfter\":"
                            + result.getRetryAfterSeconds() + "}");
                    log.warn("Global rate limit exceeded: pattern='{}' key='{}'",
                            rule.getPattern(), key);
                    return false;
                }
            }
        }
        return true; // no rule matched
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader(X_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
