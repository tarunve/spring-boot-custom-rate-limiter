package com.spring.rest.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spring.rest.config.UserApiKeyConfig.RateLimitConfig;
import com.spring.rest.model.UserApiKey;

@ExtendWith(MockitoExtension.class)
public class UserApiKeyConfigTest {
	
	@Test
	public void testUserApiKeyConfig() throws Exception {
		UserApiKeyConfig mockUserApiKeyConfig = new UserApiKeyConfig();
		mockUserApiKeyConfig.setDefaultRateLimit(3);
		mockUserApiKeyConfig.setRefreshTimeInterval(60);
		
		List<RateLimitConfig> rateLimits = mockUserApiKeyConfig.getRateLimits();
		rateLimits.addAll(getRateLimits());

		assertNotNull(rateLimits);
		assertNotNull(mockUserApiKeyConfig.getRateLimitsMap());
		assertEquals(3, mockUserApiKeyConfig.getDefaultRateLimit());
		assertEquals(60, mockUserApiKeyConfig.getRefreshTimeInterval());
	}
	
	private List<RateLimitConfig> getRateLimits() {
		RateLimitConfig config1 = new RateLimitConfig();
		config1.setUser("User1");
		config1.setUri("/api/one");
		config1.setLimit(5);
		
		RateLimitConfig config2 = new RateLimitConfig();
		config2.setUser("User2");
		config2.setUri("/api/two");
		config2.setLimit(20);
		
		return Arrays.asList(config1, config2);
	}
	
	@Test
	public void testRateLimitConfig() {
		RateLimitConfig config = new RateLimitConfig();
		config.setUser("User1");
		config.setUri("/api/one");
		config.setLimit(5);
		
		assertEquals("User1", config.getUser());
		assertEquals("/api/one", config.getUri());
		assertEquals(5, config.getLimit());
		assertEquals("UserApiKeyConfig.RateLimitConfig(user=User1, uri=/api/one, limit=5)", config.toString());
	}
	
	@Test
	public void testRateLimitConfigEqualsMethod() {
		RateLimitConfig config1 = new RateLimitConfig();
		config1.setUser("User1"); config1.setUri("/api/one"); config1.setLimit(5);
		
		RateLimitConfig config2 = new RateLimitConfig();
		config2.setUser("User2"); config2.setUri("/api/two"); config2.setLimit(20);
		
		RateLimitConfig config3 = new RateLimitConfig();
		config3.setUser("User2"); config3.setUri("/api/two"); config3.setLimit(20);
		
		RateLimitConfig config4 = new RateLimitConfig();
		config4.setUser("User2"); config4.setUri("/api/four"); config4.setLimit(20);
		
		RateLimitConfig config5 = new RateLimitConfig();
		config5.setUser("User5"); config5.setUri("/api/two"); config5.setLimit(20);
		
		assertEquals(true, config1.equals(config1));
		assertEquals(false, config1.equals(null));
		assertEquals(false, config1.equals(new Object()));
		assertEquals(false, config1.equals(config2));
        assertEquals(true, config2.equals(config3));
        assertEquals(false, config2.equals(config4));
        assertEquals(false, config2.equals(config5));
	}
	
	@Test
	public void testRateLimitConfigHashCodeMethod() {
		RateLimitConfig config1 = new RateLimitConfig();
		config1.setUser(null); config1.setUri("/api/one"); config1.setLimit(5);
		
		RateLimitConfig config2 = new RateLimitConfig();
		config2.setUser("User2"); config2.setUri(null); config2.setLimit(20);
		
		RateLimitConfig config3 = new RateLimitConfig();
		config3.setUser(null); config3.setUri(null); config3.setLimit(20);
        
        assertNotNull(config1.hashCode());
        assertNotNull(config2.hashCode());
        assertNotNull(config3.hashCode());
	}
}