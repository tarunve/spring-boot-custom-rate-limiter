package com.spring.rest.config;

import com.spring.rest.model.UserApiKey;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("custom")
public class UserApiKeyConfig {

    private static final List<RateLimitConfig> rateLimits = new ArrayList<>();
    private int defaultRateLimit;
    private int refreshTimeInterval;

    public List<RateLimitConfig> getRateLimits(){
        return rateLimits;
    }

    public int getDefaultRateLimit(){
        return defaultRateLimit;
    }

    public void setDefaultRateLimit(int defaultRateLimit) {
        this.defaultRateLimit = defaultRateLimit;
    }

    public int getRefreshTimeInterval(){
        return refreshTimeInterval;
    }

    public void setRefreshTimeInterval(int refreshTimeInterval) {
        this.refreshTimeInterval = refreshTimeInterval;
    }

    public Map<UserApiKey, Integer> getRateLimitsMap(){
        Map<UserApiKey, Integer> resultMap = new HashMap<>();
        rateLimits.forEach((rateLimitConf) -> {
            UserApiKey userApiKey = new UserApiKey(rateLimitConf.getUser(), rateLimitConf.getUri());
            resultMap.put(userApiKey, rateLimitConf.getLimit());
        });
        return resultMap;
    }

    @Data
    public static class RateLimitConfig {
        private String user;
        private String uri;
        private int limit;
    }
}