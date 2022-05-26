package com.spring.rest.interceptor;

import com.spring.rest.config.UserApiKeyConfig;
import com.spring.rest.model.UserApiKey;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class PerUserRateLimitInterceptor implements HandlerInterceptor {

    private final Map<UserApiKey, Integer> rateLimitMap;
    private static final Map<UserApiKey, Bucket> buckets = new ConcurrentHashMap<>();
    private final int defaultCapacity;
    private static final int NUM_TOKENS = 1;
    private final int refreshInterval;
    private static final String HEADER_X_RATE_LIMIT_REMAINING = "X-Rate-Limit-Remaining";
    private static final String HEADER_X_RATE_LIMIT_RETRY_AFTER_MS = "X-Rate-Limit-Retry-After-Milliseconds";

    @Autowired
    public PerUserRateLimitInterceptor(UserApiKeyConfig userApiKeyConfig){
        this.rateLimitMap = userApiKeyConfig.getRateLimitsMap();
        this.defaultCapacity = userApiKeyConfig.getDefaultRateLimit();
        this.refreshInterval = userApiKeyConfig.getRefreshTimeInterval();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Bucket requestBucket;

        String user = (request.getUserPrincipal() == null) ? "default" : request.getUserPrincipal().getName();
        UserApiKey apiKey = new UserApiKey(user, request.getRequestURI());
        if (rateLimitMap.containsKey(apiKey)) {
            int capacity = rateLimitMap.get(apiKey);
            requestBucket = buckets.computeIfAbsent(apiKey, key -> getBucket(capacity, capacity));
        } else {
            requestBucket = buckets.computeIfAbsent(apiKey, key -> getBucket(defaultCapacity, defaultCapacity));
        }

        ConsumptionProbe probe = requestBucket.tryConsumeAndReturnRemaining(NUM_TOKENS);
        if (probe.isConsumed()) {
            response.addHeader(HEADER_X_RATE_LIMIT_REMAINING, Long.toString(probe.getRemainingTokens()));
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.addHeader(HEADER_X_RATE_LIMIT_RETRY_AFTER_MS, Long.toString(TimeUnit.NANOSECONDS.toMillis(probe.getNanosToWaitForRefill())));
        return false;
    }

    private Bucket getBucket(int capacity, int numTokens) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(numTokens, Duration.ofSeconds(refreshInterval))))
                .build();
    }
}
