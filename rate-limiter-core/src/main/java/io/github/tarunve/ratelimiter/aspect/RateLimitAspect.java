package io.github.tarunve.ratelimiter.aspect;

import io.github.tarunve.ratelimiter.annotation.RateLimit;
import io.github.tarunve.ratelimiter.bucket.RateLimiterService;
import io.github.tarunve.ratelimiter.exception.RateLimitExceededException;
import io.github.tarunve.ratelimiter.key.RateLimitKeyResolver;
import io.github.tarunve.ratelimiter.model.RateLimitResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * AOP aspect that intercepts methods annotated with {@link RateLimit} and
 * enforces token-bucket rate limiting.
 *
 * <p><b>Processing flow</b></p>
 * <ol>
 *   <li>Retrieve the current {@link HttpServletRequest} and {@link HttpServletResponse}
 *       from {@link RequestContextHolder}.</li>
 *   <li>Resolve the rate-limit key via {@link RateLimitKeyResolver}.</li>
 *   <li>Convert the annotation's {@code duration} + {@code durationUnit} to nanoseconds
 *       and call {@link RateLimiterService#tryConsume}.</li>
 *   <li>If allowed, add {@code X-RateLimit-Limit} and {@code X-RateLimit-Remaining}
 *       headers and proceed.</li>
 *   <li>If denied, add {@code X-RateLimit-Retry-After} (seconds) and throw
 *       {@link RateLimitExceededException} which maps to HTTP 429.</li>
 * </ol>
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class RateLimitAspect {

    // HTTP response header names
    static final String HEADER_LIMIT         = "X-RateLimit-Limit";
    static final String HEADER_REMAINING     = "X-RateLimit-Remaining";
    static final String HEADER_RETRY_AFTER   = "X-RateLimit-Retry-After";

    private final RateLimiterService rateLimiterService;
    private final RateLimitKeyResolver keyResolver;

    // -------------------------------------------------------------------------
    // Advice
    // -------------------------------------------------------------------------

    /**
     * Around advice — wraps every method annotated with {@link RateLimit}.
     */
    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            // Not a web request (e.g., called from a test or scheduler) — skip limiting
            log.debug("RateLimitAspect: no request context, skipping rate limit check");
            return joinPoint.proceed();
        }

        HttpServletRequest  request  = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        String key          = keyResolver.resolve(rateLimit, joinPoint, request);
        long   durationNanos = rateLimit.durationUnit().toNanos(rateLimit.duration());

        log.debug("Rate limit check: key='{}' limit={} durationNanos={} backend={}",
                key, rateLimit.limit(), durationNanos, rateLimiterService.backendName());

        RateLimitResult result = rateLimiterService.tryConsume(key, rateLimit.limit(), durationNanos);

        if (result.isAllowed()) {
            addAllowHeaders(response, rateLimit.limit(), result.getRemainingTokens());
            return joinPoint.proceed();
        } else {
            addDenyHeaders(response, result);
            throw new RateLimitExceededException(key, result.getNanosToWaitForRefill());
        }
    }

    // -------------------------------------------------------------------------
    // Header helpers
    // -------------------------------------------------------------------------

    private void addAllowHeaders(HttpServletResponse response, int limit, long remaining) {
        if (response == null) return;
        response.setHeader(HEADER_LIMIT,     String.valueOf(limit));
        response.setHeader(HEADER_REMAINING, String.valueOf(remaining));
    }

    private void addDenyHeaders(HttpServletResponse response, RateLimitResult result) {
        if (response == null) return;
        response.setHeader(HEADER_REMAINING,   "0");
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(result.getRetryAfterSeconds()));
    }
}
