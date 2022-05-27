package com.spring.rest.config;

import com.spring.rest.model.UserApiKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class UserApiKeyConfigTest {

    private UserApiKeyConfig mockUserApiKeyConfig;

    @BeforeEach
    public void setup() {
        mockUserApiKeyConfig = new UserApiKeyConfig();
        mockUserApiKeyConfig.setRefreshTimeInterval(60);
        mockUserApiKeyConfig.setRateLimits(List.of(getRateLimitObject()));
        mockUserApiKeyConfig.setDefaultRateLimit(10);
    }

    private RateLimitConfig getRateLimitObject() {
        RateLimitConfig rateLimitConfig = new RateLimitConfig();
        rateLimitConfig.setUser("User1");
        rateLimitConfig.setUri("/test");
        rateLimitConfig.setLimit(10);
        return rateLimitConfig;
    }

    @Test
    public void testNonNullRateLimits() {
        Assertions.assertNotNull(mockUserApiKeyConfig.getRateLimits());
        Assertions.assertEquals(1, mockUserApiKeyConfig.getRateLimits().size());
    }

    @Test
    public void testRateLimitObject() {
        RateLimitConfig rateLimitConfig = getRateLimitObject();
        Assertions.assertEquals("User1", rateLimitConfig.getUser());
        Assertions.assertNotEquals("/testing", rateLimitConfig.getUri());
        Assertions.assertEquals(10, rateLimitConfig.getLimit());
    }

    @Test
    public void testDefaultRateLimit() {
        Assertions.assertEquals(10, mockUserApiKeyConfig.getDefaultRateLimit());
        Assertions.assertEquals(60, mockUserApiKeyConfig.getRefreshTimeInterval());
    }

    @Test
    public void testRateLimitMap(){
        Map<UserApiKey, Integer> mp = mockUserApiKeyConfig.getRateLimitsMap();
        Assertions.assertEquals(10, mp.get(new UserApiKey("User1", "/test")));
    }
}
