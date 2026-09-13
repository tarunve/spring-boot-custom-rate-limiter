package io.github.tarunve.ratelimiter.aspect;

import io.github.tarunve.ratelimiter.annotation.RateLimit;
import io.github.tarunve.ratelimiter.bucket.RateLimiterService;
import io.github.tarunve.ratelimiter.exception.RateLimitExceededException;
import io.github.tarunve.ratelimiter.key.DefaultRateLimitKeyResolver;
import io.github.tarunve.ratelimiter.key.RateLimitKeyResolver;
import io.github.tarunve.ratelimiter.model.RateLimitResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RateLimitAspect")
class RateLimitAspectTest {

    @Mock private RateLimiterService rateLimiterService;
    @Mock private RateLimitKeyResolver keyResolver;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature methodSignature;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitAspect(rateLimiterService, keyResolver);
        when(rateLimiterService.backendName()).thenReturn("in-memory");
    }

    private void bindRequestContext() {
        ServletRequestAttributes attrs = new ServletRequestAttributes(request, response);
        RequestContextHolder.setRequestAttributes(attrs);
    }

    private RateLimit buildAnnotation(int limit, long duration, TimeUnit unit,
                                      RateLimit.KeyType keyType, String key) {
        return new RateLimit() {
            @Override public Class<RateLimit> annotationType() { return RateLimit.class; }
            @Override public int limit()            { return limit; }
            @Override public long duration()        { return duration; }
            @Override public TimeUnit durationUnit(){ return unit; }
            @Override public KeyType keyType()      { return keyType; }
            @Override public String key()           { return key; }
        };
    }

    @Test
    @DisplayName("should proceed without rate-limit check when no request context is bound")
    void noRequestContext_proceedsWithoutCheck() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        RateLimit annotation = buildAnnotation(10, 60, TimeUnit.SECONDS, RateLimit.KeyType.IP, "");

        Object expected = "response";
        when(joinPoint.proceed()).thenReturn(expected);

        Object result = aspect.enforce(joinPoint, annotation);

        assertThat(result).isEqualTo(expected);
        verifyNoInteractions(rateLimiterService, keyResolver);
    }

    @Nested
    @DisplayName("when request is within limit")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class WhenAllowed {

        @BeforeEach void setup() { bindRequestContext(); }

        @Test
        @DisplayName("should proceed and return the target method result")
        void allowed_proceedsAndReturnsResult() throws Throwable {
            RateLimit annotation = buildAnnotation(5, 60, TimeUnit.SECONDS, RateLimit.KeyType.IP, "");
            when(keyResolver.resolve(eq(annotation), eq(joinPoint), any())).thenReturn("192.168.1.1");
            when(rateLimiterService.tryConsume(eq("192.168.1.1"), eq(5), anyLong()))
                    .thenReturn(RateLimitResult.allowed(4L));
            when(joinPoint.proceed()).thenReturn("ok");

            Object result = aspect.enforce(joinPoint, annotation);

            assertThat(result).isEqualTo("ok");
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("should set X-RateLimit-Limit and X-RateLimit-Remaining headers on allow")
        void allowed_setsRateLimitHeaders() throws Throwable {
            RateLimit annotation = buildAnnotation(10, 60, TimeUnit.SECONDS, RateLimit.KeyType.IP, "");
            when(keyResolver.resolve(any(), any(), any())).thenReturn("10.0.0.1");
            when(rateLimiterService.tryConsume(any(), anyInt(), anyLong()))
                    .thenReturn(RateLimitResult.allowed(7L));

            aspect.enforce(joinPoint, annotation);

            verify(response).setHeader(RateLimitAspect.HEADER_LIMIT,     "10");
            verify(response).setHeader(RateLimitAspect.HEADER_REMAINING, "7");
        }

        @Test
        @DisplayName("should convert durationUnit to nanoseconds correctly before calling service")
        void allowed_convertsMinutesToNanos() throws Throwable {
            RateLimit annotation = buildAnnotation(100, 2, TimeUnit.MINUTES, RateLimit.KeyType.IP, "");
            when(keyResolver.resolve(any(), any(), any())).thenReturn("key");
            when(rateLimiterService.tryConsume(any(), anyInt(), anyLong()))
                    .thenReturn(RateLimitResult.allowed(99L));

            aspect.enforce(joinPoint, annotation);

            long expectedNanos = TimeUnit.MINUTES.toNanos(2);
            verify(rateLimiterService).tryConsume("key", 100, expectedNanos);
        }
    }

    @Nested
    @DisplayName("when rate limit is exceeded")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class WhenExceeded {

        @BeforeEach void setup() { bindRequestContext(); }

        @Test
        @DisplayName("should throw RateLimitExceededException")
        void exceeded_throwsException() {
            RateLimit annotation = buildAnnotation(3, 60, TimeUnit.SECONDS, RateLimit.KeyType.IP, "");
            when(keyResolver.resolve(any(), any(), any())).thenReturn("1.2.3.4");
            long nanosToWait = 45_000_000_000L;
            when(rateLimiterService.tryConsume(any(), anyInt(), anyLong()))
                    .thenReturn(RateLimitResult.denied(nanosToWait));

            assertThatThrownBy(() -> aspect.enforce(joinPoint, annotation))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessageContaining("1.2.3.4");
        }

        @Test
        @DisplayName("should NOT proceed to the target method when limit is exceeded")
        void exceeded_doesNotProceed() {
            RateLimit annotation = buildAnnotation(1, 60, TimeUnit.SECONDS, RateLimit.KeyType.IP, "");
            when(keyResolver.resolve(any(), any(), any())).thenReturn("key");
            when(rateLimiterService.tryConsume(any(), anyInt(), anyLong()))
                    .thenReturn(RateLimitResult.denied(1_000_000_000L));

            assertThatThrownBy(() -> aspect.enforce(joinPoint, annotation))
                    .isInstanceOf(RateLimitExceededException.class);

            verifyNoMoreInteractions(joinPoint);
        }

        @Test
        @DisplayName("should set X-RateLimit-Retry-After and X-RateLimit-Remaining=0 headers on deny")
        void exceeded_setsRetryAfterHeader() {
            RateLimit annotation = buildAnnotation(5, 60, TimeUnit.SECONDS, RateLimit.KeyType.IP, "");
            when(keyResolver.resolve(any(), any(), any())).thenReturn("key");
            long nanosToWait = 30_000_000_000L;
            when(rateLimiterService.tryConsume(any(), anyInt(), anyLong()))
                    .thenReturn(RateLimitResult.denied(nanosToWait));

            assertThatThrownBy(() -> aspect.enforce(joinPoint, annotation))
                    .isInstanceOf(RateLimitExceededException.class);

            verify(response).setHeader(RateLimitAspect.HEADER_REMAINING,   "0");
            verify(response).setHeader(RateLimitAspect.HEADER_RETRY_AFTER, "30");
        }

        @Test
        @DisplayName("RateLimitExceededException carries correct nanosToWait")
        void exceeded_exceptionCarriesNanosToWait() {
            RateLimit annotation = buildAnnotation(1, 60, TimeUnit.SECONDS, RateLimit.KeyType.IP, "");
            when(keyResolver.resolve(any(), any(), any())).thenReturn("some-key");
            long nanosToWait = 10_000_000_000L;
            when(rateLimiterService.tryConsume(any(), anyInt(), anyLong()))
                    .thenReturn(RateLimitResult.denied(nanosToWait));

            assertThatThrownBy(() -> aspect.enforce(joinPoint, annotation))
                    .isInstanceOf(RateLimitExceededException.class)
                    .satisfies(ex -> {
                        RateLimitExceededException rlex = (RateLimitExceededException) ex;
                        assertThat(rlex.getNanosToWait()).isEqualTo(nanosToWait);
                        assertThat(rlex.getRetryAfterSeconds()).isEqualTo(10L);
                    });
        }
    }

    @Nested
    @DisplayName("key resolution by IP")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class KeyResolutionByIp {

        @BeforeEach void setup() { bindRequestContext(); }

        @Test
        @DisplayName("DefaultRateLimitKeyResolver returns X-Forwarded-For first IP")
        void ipResolver_usesXForwardedFor() {
            DefaultRateLimitKeyResolver resolver = new DefaultRateLimitKeyResolver();
            RateLimit annotation = buildAnnotation(10, 60, TimeUnit.SECONDS, RateLimit.KeyType.IP, "");

            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getDeclaringTypeName()).thenReturn("com.example.Controller");
            when(methodSignature.getName()).thenReturn("handle");

            String key = resolver.resolve(annotation, joinPoint, request);

            assertThat(key).startsWith("com.example.Controller#handle:");
            assertThat(key).endsWith("203.0.113.5");
        }

        @Test
        @DisplayName("DefaultRateLimitKeyResolver falls back to RemoteAddr when no X-Forwarded-For")
        void ipResolver_fallsBackToRemoteAddr() {
            DefaultRateLimitKeyResolver resolver = new DefaultRateLimitKeyResolver();
            RateLimit annotation = buildAnnotation(10, 60, TimeUnit.SECONDS, RateLimit.KeyType.IP, "");

            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.0.99");
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getDeclaringTypeName()).thenReturn("com.example.Controller");
            when(methodSignature.getName()).thenReturn("handle");

            String key = resolver.resolve(annotation, joinPoint, request);

            assertThat(key).endsWith("192.168.0.99");
        }

        @Test
        @DisplayName("aspect passes resolved IP key to RateLimiterService")
        void aspect_passesIpKeyToService() throws Throwable {
            RateLimit annotation = buildAnnotation(10, 60, TimeUnit.SECONDS, RateLimit.KeyType.IP, "");
            String resolvedKey = "MyController#get:1.2.3.4";
            when(keyResolver.resolve(eq(annotation), eq(joinPoint), any())).thenReturn(resolvedKey);
            when(rateLimiterService.tryConsume(eq(resolvedKey), eq(10), anyLong()))
                    .thenReturn(RateLimitResult.allowed(9L));

            aspect.enforce(joinPoint, annotation);

            verify(rateLimiterService).tryConsume(resolvedKey, 10, TimeUnit.SECONDS.toNanos(60));
        }
    }

    @Nested
    @DisplayName("key resolution by custom SpEL")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class KeyResolutionBySpel {

        @BeforeEach void setup() { bindRequestContext(); }

        @Test
        @DisplayName("DefaultRateLimitKeyResolver evaluates SpEL against request header")
        void spelResolver_evaluatesHeaderExpression() throws NoSuchMethodException {
            DefaultRateLimitKeyResolver resolver = new DefaultRateLimitKeyResolver();
            RateLimit annotation = buildAnnotation(
                    50, 60, TimeUnit.SECONDS, RateLimit.KeyType.CUSTOM,
                    "#request.getHeader('X-Tenant-Id')");

            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(request.getHeader("X-Tenant-Id")).thenReturn("tenant-abc");

            Method method = SampleTarget.class.getMethod("doSomething", HttpServletRequest.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getDeclaringTypeName()).thenReturn("SampleTarget");
            when(methodSignature.getName()).thenReturn("doSomething");
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getTarget()).thenReturn(new SampleTarget());
            when(joinPoint.getArgs()).thenReturn(new Object[]{request});

            String key = resolver.resolve(annotation, joinPoint, request);

            assertThat(key).contains("custom:tenant-abc");
        }

        @Test
        @DisplayName("DefaultRateLimitKeyResolver falls back to IP when SpEL key is empty")
        void spelResolver_fallsBackToIpOnEmptyResult() throws NoSuchMethodException {
            DefaultRateLimitKeyResolver resolver = new DefaultRateLimitKeyResolver();
            RateLimit annotation = buildAnnotation(
                    50, 60, TimeUnit.SECONDS, RateLimit.KeyType.CUSTOM,
                    "#request.getHeader('X-Tenant-Id')");

            when(request.getHeader("X-Tenant-Id")).thenReturn("");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("10.0.0.2");

            Method method = SampleTarget.class.getMethod("doSomething", HttpServletRequest.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getDeclaringTypeName()).thenReturn("SampleTarget");
            when(methodSignature.getName()).thenReturn("doSomething");
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getTarget()).thenReturn(new SampleTarget());
            when(joinPoint.getArgs()).thenReturn(new Object[]{request});

            String key = resolver.resolve(annotation, joinPoint, request);

            assertThat(key).endsWith("10.0.0.2");
            assertThat(key).doesNotContain("custom:");
        }

        @Test
        @DisplayName("DefaultRateLimitKeyResolver falls back to IP when SpEL expression is blank")
        void spelResolver_fallsBackToIpWhenExpressionIsBlank() {
            DefaultRateLimitKeyResolver resolver = new DefaultRateLimitKeyResolver();
            RateLimit annotation = buildAnnotation(
                    50, 60, TimeUnit.SECONDS, RateLimit.KeyType.CUSTOM, "");

            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("10.0.0.3");
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getDeclaringTypeName()).thenReturn("SampleTarget");
            when(methodSignature.getName()).thenReturn("doSomething");

            String key = resolver.resolve(annotation, joinPoint, request);

            assertThat(key).endsWith("10.0.0.3");
        }

        @Test
        @DisplayName("aspect passes SpEL-resolved key to RateLimiterService")
        void aspect_passesSpelKeyToService() throws Throwable {
            RateLimit annotation = buildAnnotation(
                    200, 60, TimeUnit.SECONDS, RateLimit.KeyType.CUSTOM,
                    "#request.getHeader('X-Tenant-Id')");
            String resolvedKey = "MyController#data:custom:tenant-xyz";
            when(keyResolver.resolve(eq(annotation), eq(joinPoint), any())).thenReturn(resolvedKey);
            when(rateLimiterService.tryConsume(eq(resolvedKey), eq(200), anyLong()))
                    .thenReturn(RateLimitResult.allowed(199L));

            aspect.enforce(joinPoint, annotation);

            verify(rateLimiterService).tryConsume(resolvedKey, 200, TimeUnit.SECONDS.toNanos(60));
        }
    }

    public static class SampleTarget {
        public void doSomething(HttpServletRequest request) { }
    }
}
