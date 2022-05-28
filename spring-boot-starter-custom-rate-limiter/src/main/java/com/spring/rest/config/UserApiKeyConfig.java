package com.spring.rest.config;

import com.spring.rest.model.UserApiKey;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties("custom")
public class UserApiKeyConfig {

    private List<RateLimitConfig> rateLimits;
    private int defaultRateLimit;
    private int refreshTimeInterval;

    public List<RateLimitConfig> getRateLimits(){
        return rateLimits;
    }

    public void setRateLimits(List<RateLimitConfig> listRateLimits){
        this.rateLimits = listRateLimits;
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
        getRateLimits().forEach((rateLimitConf) -> {
            UserApiKey userApiKey = new UserApiKey(rateLimitConf.getUser(), rateLimitConf.getUri());
            resultMap.put(userApiKey, rateLimitConf.getLimit());
        });
        return resultMap;
    }
}

@Data
class RateLimitConfig {
    private String user;
    private String uri;
    private int limit;
}