package com.spring.rest.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spring.rest.config.UserApiKeyConfig;
import com.spring.rest.model.UserApiKey;

@ExtendWith(MockitoExtension.class)
public class PerUserRateLimitInterceptorTest {
	
	@InjectMocks
	private PerUserRateLimitInterceptor mockPerUserRateLimitInterceptor;

	@Mock
	private UserApiKeyConfig mockUserApiKeyConfig;
	
	@Mock
	private HttpServletRequest mockRequest;
	
	@Mock
	private HttpServletResponse mockResponse;
	
	@Mock
	private Object mockObjectHandler;
	
	@Mock
	private Principal mockPrincipal;
	
	@BeforeEach
	public void setup() {
		when(mockUserApiKeyConfig.getRateLimitsMap()).thenReturn(getMockRateLimitsMap());
		when(mockUserApiKeyConfig.getDefaultRateLimit()).thenReturn(3);
		when(mockUserApiKeyConfig.getRefreshTimeInterval()).thenReturn(60);
		mockPerUserRateLimitInterceptor = new PerUserRateLimitInterceptor(mockUserApiKeyConfig);
	}
	
	private Map<UserApiKey, Integer> getMockRateLimitsMap() {
		Map<UserApiKey, Integer> resultMap = new HashMap<>();
		UserApiKey userApiKey1 = new UserApiKey("User1", "/api/one");
		UserApiKey userApiKey2 = new UserApiKey("User2", "/api/two");
		
		resultMap.put(userApiKey1, 5);
		resultMap.put(userApiKey2, 20);
		return resultMap;
	}
	
	@Test
	public void testPreHandleWithUserPrincipalNull() throws Exception {
		when(mockRequest.getUserPrincipal()).thenReturn(null);
		when(mockRequest.getRequestURI()).thenReturn("/api/one");
		boolean result = mockPerUserRateLimitInterceptor.preHandle(mockRequest, mockResponse, mockObjectHandler);
		assertTrue(result);
	}
	
	@Test
	public void testPreHandleWithUserPrincipalValue() throws Exception {
		when(mockPrincipal.getName()).thenReturn("User1");
		when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
		when(mockRequest.getRequestURI()).thenReturn("/api/one");
		boolean result = mockPerUserRateLimitInterceptor.preHandle(mockRequest, mockResponse, mockObjectHandler);
		assertEquals(true, result);
	}
}