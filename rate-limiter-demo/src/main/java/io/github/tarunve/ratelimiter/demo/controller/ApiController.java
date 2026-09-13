package io.github.tarunve.ratelimiter.demo.controller;

import io.github.tarunve.ratelimiter.annotation.RateLimit;
import io.github.tarunve.ratelimiter.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Demo REST controller showcasing every {@link RateLimit} feature.
 *
 * <p>All rate-limit decisions are visible through standard response headers:
 * <ul>
 *   <li>{@code X-RateLimit-Limit}       — configured maximum</li>
 *   <li>{@code X-RateLimit-Remaining}   — tokens left this window</li>
 *   <li>{@code X-RateLimit-Retry-After} — seconds to wait (on 429)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    // -------------------------------------------------------------------------
    // IP-based rate limiting (default)
    // -------------------------------------------------------------------------

    /**
     * Simple hello endpoint — limited to 10 requests per 10 seconds per IP.
     */
    @RateLimit(limit = 10, duration = 10, durationUnit = TimeUnit.SECONDS)
    @GetMapping("/public/hello")
    public ResponseEntity<Map<String, Object>> hello(HttpServletRequest request) {
        log.info("GET /api/public/hello from {}", request.getRemoteAddr());
        return ResponseEntity.ok(response("Hello from rate-limited API!", request));
    }

    /**
     * Weather endpoint — covered by the global interceptor rule in application.yml
     * (200 req/60s per IP).  No method-level annotation here intentionally.
     */
    @GetMapping("/public/weather")
    public ResponseEntity<Map<String, Object>> weather(HttpServletRequest request) {
        return ResponseEntity.ok(response("Sunny skies ahead!", request));
    }

    /**
     * Heavy computation endpoint — very tight limit: 3 requests per minute per IP.
     */
    @RateLimit(limit = 3, duration = 1, durationUnit = TimeUnit.MINUTES)
    @GetMapping("/compute")
    public ResponseEntity<Map<String, Object>> compute(HttpServletRequest request) {
        return ResponseEntity.ok(response("Result: 42", request));
    }

    // -------------------------------------------------------------------------
    // User-based rate limiting
    // -------------------------------------------------------------------------

    /**
     * Order creation — limited per authenticated user (5 orders / minute).
     * Falls back to IP if unauthenticated.
     */
    @RateLimit(limit = 5, duration = 1, durationUnit = TimeUnit.MINUTES,
               keyType = RateLimit.KeyType.USER)
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response("Order created", request));
    }

    // -------------------------------------------------------------------------
    // Custom SpEL key (per tenant)
    // -------------------------------------------------------------------------

    /**
     * Tenant-scoped data endpoint — rate limited per {@code X-Tenant-Id} header
     * using a SpEL expression.  100 requests per 60 seconds per tenant.
     */
    @RateLimit(
        limit    = 100,
        duration = 60,
        keyType  = RateLimit.KeyType.CUSTOM,
        key      = "#request.getHeader('X-Tenant-Id') ?: 'default-tenant'"
    )
    @GetMapping("/tenant/data")
    public ResponseEntity<Map<String, Object>> tenantData(HttpServletRequest request) {
        String tenantId = request.getHeader("X-Tenant-Id");
        Map<String, Object> body = response("Tenant data", request);
        body.put("tenantId", tenantId != null ? tenantId : "default-tenant");
        return ResponseEntity.ok(body);
    }

    // -------------------------------------------------------------------------
    // Exception handler — converts RateLimitExceededException to 429 JSON body
    // -------------------------------------------------------------------------

    /**
     * Handles {@link RateLimitExceededException} thrown by the AOP aspect and
     * returns a structured JSON 429 response with a {@code Retry-After} header.
     *
     * <p>Note: {@code @ResponseStatus(429)} on the exception class is enough for
     * Spring MVC to return 429, but adding this handler gives a richer JSON body.
     */
    @RestControllerAdvice
    static class RateLimitExceptionHandler {

        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
                RateLimitExceededException ex) {

            log.warn("Rate limit exceeded: {}", ex.getMessage());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status",     429);
            body.put("error",      "Too Many Requests");
            body.put("message",    ex.getMessage());
            body.put("retryAfter", ex.getRetryAfterSeconds());
            body.put("timestamp",  Instant.now().toString());

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                    .body(body);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> response(String message, HttpServletRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("message",   message);
        map.put("timestamp", Instant.now().toString());
        map.put("clientIp",  request.getRemoteAddr());
        return map;
    }
}
