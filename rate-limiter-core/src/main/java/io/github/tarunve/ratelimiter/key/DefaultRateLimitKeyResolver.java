package io.github.tarunve.ratelimiter.key;

import io.github.tarunve.ratelimiter.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * Default implementation of {@link RateLimitKeyResolver}.
 *
 * <p>Key resolution strategy depends on the annotation's {@link RateLimit.KeyType}:
 * <ul>
 *   <li>{@link RateLimit.KeyType#IP} — client IP address extracted from
 *       {@code X-Forwarded-For} or {@link HttpServletRequest#getRemoteAddr()}</li>
 *   <li>{@link RateLimit.KeyType#USER} — authenticated principal name from
 *       Spring Security; falls back to IP when no authentication is available</li>
 *   <li>{@link RateLimit.KeyType#CUSTOM} — SpEL expression specified in
 *       {@link RateLimit#key()}; method parameters are available by name and
 *       the current {@link HttpServletRequest} is bound as {@code #request}</li>
 * </ul>
 *
 * <p>The resolved key is prefixed with the fully-qualified method signature so
 * that two different endpoints with the same IP or user do not share a bucket.
 */
@Slf4j
public class DefaultRateLimitKeyResolver implements RateLimitKeyResolver {

    private static final String UNKNOWN_IP = "unknown";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();

    @Override
    public String resolve(RateLimit annotation,
                          ProceedingJoinPoint joinPoint,
                          HttpServletRequest request) {

        String methodKey = buildMethodKey(joinPoint);
        String bucketKey = switch (annotation.keyType()) {
            case IP -> resolveIpKey(request);
            case USER -> resolveUserKey(request);
            case CUSTOM -> resolveCustomKey(annotation, joinPoint, request);
        };

        return methodKey + ":" + bucketKey;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildMethodKey(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        return sig.getDeclaringTypeName() + "#" + sig.getName();
    }

    private String resolveIpKey(HttpServletRequest request) {
        String forwarded = request.getHeader(X_FORWARDED_FOR);
        if (StringUtils.hasText(forwarded)) {
            // X-Forwarded-For may contain a comma-separated chain; take the first
            String ip = forwarded.split(",")[0].trim();
            if (StringUtils.hasText(ip)) {
                return ip;
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : UNKNOWN_IP;
    }

    private String resolveUserKey(HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                return "user:" + auth.getName();
            }
        } catch (Exception e) {
            log.debug("Spring Security not available or authentication failed, falling back to IP key", e);
        }
        // Fall back to IP when not authenticated
        return resolveIpKey(request);
    }

    private String resolveCustomKey(RateLimit annotation,
                                    ProceedingJoinPoint joinPoint,
                                    HttpServletRequest request) {
        String spelExpression = annotation.key();
        if (!StringUtils.hasText(spelExpression)) {
            log.warn("@RateLimit keyType=CUSTOM but key() is empty; falling back to IP");
            return resolveIpKey(request);
        }

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();

            EvaluationContext context = new MethodBasedEvaluationContext(
                    joinPoint.getTarget(), method, args, parameterNameDiscoverer);
            // Expose the request as a named variable for convenience
            context.setVariable("request", request);

            Object result = parser.parseExpression(spelExpression).getValue(context);
            if (result == null || !StringUtils.hasText(result.toString())) {
                log.warn("SpEL expression '{}' evaluated to null/empty; falling back to IP", spelExpression);
                return resolveIpKey(request);
            }
            return "custom:" + result;
        } catch (Exception e) {
            log.warn("Failed to evaluate SpEL expression '{}'; falling back to IP", spelExpression, e);
            return resolveIpKey(request);
        }
    }
}
