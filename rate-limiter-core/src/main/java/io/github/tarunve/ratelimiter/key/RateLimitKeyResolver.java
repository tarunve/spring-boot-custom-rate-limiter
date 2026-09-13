package io.github.tarunve.ratelimiter.key;

import io.github.tarunve.ratelimiter.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Strategy interface for computing the rate-limit key for an incoming request.
 *
 * <p>The key uniquely identifies the "bucket" that will be charged.  Different
 * implementations or strategies produce different granularities of rate limiting
 * (per IP, per user, per custom expression, etc.).
 *
 * <p>Register a custom bean of this type to override the default key-resolution
 * behaviour globally.
 */
public interface RateLimitKeyResolver {

    /**
     * Compute the rate-limit key.
     *
     * @param annotation the {@link RateLimit} annotation found on the target method
     * @param joinPoint  the AOP join point (gives access to method arguments)
     * @param request    the current HTTP servlet request
     * @return a non-null, non-empty string key
     */
    String resolve(RateLimit annotation, ProceedingJoinPoint joinPoint, HttpServletRequest request);
}
