package com.spring.rest.config;

import com.spring.rest.model.UserApiKey;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties("custom")
public class UserApiKeyConfig {

    private List<RateLimitConfig> rateLimits;
    private int defaultRateLimit;
    private int refreshTimeInterval;

    public Map<UserApiKey, Integer> getRateLimitsMap(){
        Map<UserApiKey, Integer> resultMap = new HashMap<>();
        getRateLimits().forEach((rateLimitConf) -> {
            UserApiKey userApiKey = new UserApiKey(rateLimitConf.getUser(), rateLimitConf.getUri());
            resultMap.put(userApiKey, rateLimitConf.getLimit());
        });
        return resultMap;
    }
}

