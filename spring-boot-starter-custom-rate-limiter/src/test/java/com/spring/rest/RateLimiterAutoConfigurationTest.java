package com.spring.rest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import com.spring.rest.config.UserApiKeyConfig;
import com.spring.rest.interceptor.PerUserRateLimitInterceptor;

@ExtendWith(MockitoExtension.class)
public class RateLimiterAutoConfigurationTest {

	@InjectMocks
	private RateLimiterAutoConfiguration mockRateLimiterAutoConfiguration;
	
	@Mock
	private PerUserRateLimitInterceptor mockPerUserRateLimitInterceptor;
	
	@Mock
	private UserApiKeyConfig mockUserApiKeyConfig;
	
	@Mock
	private InterceptorRegistry mockInterceptorRegistry;
	
	@Mock
	private InterceptorRegistration mockInterceptorRegistration;
	
	@Test
	public void testAddInterceptors() {
		when(mockInterceptorRegistry.addInterceptor(mockPerUserRateLimitInterceptor)).thenReturn(mockInterceptorRegistration);
		mockRateLimiterAutoConfiguration.addInterceptors(mockInterceptorRegistry);
		verify(mockInterceptorRegistry, times(1)).addInterceptor(mockPerUserRateLimitInterceptor);
	}
	
	@Test
	public void testGetInstance() {
		PerUserRateLimitInterceptor interceptor = mockRateLimiterAutoConfiguration.getInstance(mockUserApiKeyConfig);
		assertNotNull(interceptor);
	}
}