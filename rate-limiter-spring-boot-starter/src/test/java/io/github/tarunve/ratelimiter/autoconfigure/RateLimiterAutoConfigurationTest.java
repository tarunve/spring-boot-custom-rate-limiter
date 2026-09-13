package io.github.tarunve.ratelimiter.autoconfigure;

import io.github.tarunve.ratelimiter.aspect.RateLimitAspect;
import io.github.tarunve.ratelimiter.bucket.InMemoryRateLimiterService;
import io.github.tarunve.ratelimiter.bucket.RateLimiterService;
import io.github.tarunve.ratelimiter.config.RateLimitProperties;
import io.github.tarunve.ratelimiter.key.DefaultRateLimitKeyResolver;
import io.github.tarunve.ratelimiter.key.RateLimitKeyResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RateLimiterAutoConfiguration}.
 *
 * <p>Uses {@link WebApplicationContextRunner} for lightweight, focused context
 * tests, plus a full {@link SpringBootTest} for end-to-end smoke testing.
 */
@DisplayName("RateLimiterAutoConfiguration")
class RateLimiterAutoConfigurationTest {

    /**
     * Reusable runner that applies the auto-configuration and its prerequisite
     * WebMvc auto-configuration in a web servlet application context.
     */
    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            WebMvcAutoConfiguration.class,
                            RateLimiterAutoConfiguration.class));

    // -------------------------------------------------------------------------
    // Default auto-configuration
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("default settings")
    class DefaultSettings {

        @Test
        @DisplayName("auto-configuration loads and all expected beans are present")
        void defaultConfig_allBeansPresent() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(RateLimitProperties.class);
                assertThat(context).hasSingleBean(RateLimitKeyResolver.class);
                assertThat(context).hasSingleBean(RateLimiterService.class);
                assertThat(context).hasSingleBean(RateLimitAspect.class);
            });
        }

        @Test
        @DisplayName("RateLimitProperties has expected defaults")
        void defaultConfig_propertiesDefaults() {
            contextRunner.run(context -> {
                RateLimitProperties props = context.getBean(RateLimitProperties.class);
                assertThat(props.isEnabled()).isTrue();
                assertThat(props.getDefaultLimit()).isEqualTo(100);
                assertThat(props.getDefaultDuration()).isEqualTo(60);
                assertThat(props.getBackend()).isEqualTo(RateLimitProperties.Backend.IN_MEMORY);
            });
        }

        @Test
        @DisplayName("DefaultRateLimitKeyResolver is registered as the key resolver")
        void defaultConfig_defaultKeyResolver() {
            contextRunner.run(context ->
                    assertThat(context.getBean(RateLimitKeyResolver.class))
                            .isInstanceOf(DefaultRateLimitKeyResolver.class));
        }
    }

    // -------------------------------------------------------------------------
    // InMemoryRateLimiterService created by default
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("InMemoryRateLimiterService as default backend")
    class InMemoryDefault {

        @Test
        @DisplayName("InMemoryRateLimiterService is created by default (no Redis property)")
        void defaultBackend_inMemory() {
            contextRunner.run(context -> {
                RateLimiterService service = context.getBean(RateLimiterService.class);
                assertThat(service).isInstanceOf(InMemoryRateLimiterService.class);
                assertThat(service.backendName()).isEqualTo("in-memory");
            });
        }

        @Test
        @DisplayName("InMemoryRateLimiterService is created when rate-limiter.backend=IN_MEMORY")
        void explicitInMemoryBackend() {
            contextRunner
                    .withPropertyValues("rate-limiter.backend=IN_MEMORY")
                    .run(context -> {
                        RateLimiterService service = context.getBean(RateLimiterService.class);
                        assertThat(service).isInstanceOf(InMemoryRateLimiterService.class);
                    });
        }

        @Test
        @DisplayName("user-defined RateLimiterService bean takes priority over InMemoryRateLimiterService")
        void userDefinedService_takesPriority() {
            contextRunner
                    .withUserConfiguration(CustomRateLimiterConfig.class)
                    .run(context -> {
                        RateLimiterService service = context.getBean(RateLimiterService.class);
                        assertThat(service.backendName()).isEqualTo("custom-test");
                        assertThat(service).isNotInstanceOf(InMemoryRateLimiterService.class);
                    });
        }

        @Test
        @DisplayName("user-defined RateLimitKeyResolver bean takes priority over default")
        void userDefinedKeyResolver_takesPriority() {
            contextRunner
                    .withUserConfiguration(CustomKeyResolverConfig.class)
                    .run(context -> {
                        RateLimitKeyResolver resolver = context.getBean(RateLimitKeyResolver.class);
                        assertThat(resolver).isNotInstanceOf(DefaultRateLimitKeyResolver.class);
                        assertThat(resolver).isInstanceOf(CustomKeyResolverConfig.NoOpKeyResolver.class);
                    });
        }
    }

    // -------------------------------------------------------------------------
    // Configuration disabled via property
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("when rate-limiter.enabled=false")
    class WhenDisabled {

        @Test
        @DisplayName("no RateLimiterService bean is created")
        void disabled_noRateLimiterServiceBean() {
            contextRunner
                    .withPropertyValues("rate-limiter.enabled=false")
                    .run(context ->
                            assertThatThrownBy(() -> context.getBean(RateLimiterService.class))
                                    .isInstanceOf(NoSuchBeanDefinitionException.class));
        }

        @Test
        @DisplayName("no RateLimitAspect bean is created")
        void disabled_noRateLimitAspectBean() {
            contextRunner
                    .withPropertyValues("rate-limiter.enabled=false")
                    .run(context ->
                            assertThatThrownBy(() -> context.getBean(RateLimitAspect.class))
                                    .isInstanceOf(NoSuchBeanDefinitionException.class));
        }

        @Test
        @DisplayName("no RateLimitKeyResolver bean is created")
        void disabled_noKeyResolverBean() {
            contextRunner
                    .withPropertyValues("rate-limiter.enabled=false")
                    .run(context ->
                            assertThatThrownBy(() -> context.getBean(RateLimitKeyResolver.class))
                                    .isInstanceOf(NoSuchBeanDefinitionException.class));
        }

        @Test
        @DisplayName("auto-configuration is active by default (matchIfMissing=true)")
        void defaultWithoutProperty_isActive() {
            // No rate-limiter.enabled property set — should still load
            contextRunner.run(context ->
                    assertThat(context).hasSingleBean(RateLimiterService.class));
        }
    }

    // -------------------------------------------------------------------------
    // Global rules configuration
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("global rules configuration")
    class GlobalRules {

        @Test
        @DisplayName("WebMvcConfigurer is registered when global rules are configured")
        void withGlobalRules_webMvcConfigurerRegistered() {
            contextRunner
                    .withPropertyValues(
                            "rate-limiter.global-rules[0].pattern=/api/**",
                            "rate-limiter.global-rules[0].limit=100",
                            "rate-limiter.global-rules[0].duration=60")
                    .run(context -> {
                        assertThat(context).hasBean("rateLimitWebMvcConfigurer");
                        RateLimitProperties props = context.getBean(RateLimitProperties.class);
                        assertThat(props.getGlobalRules()).hasSize(1);
                        assertThat(props.getGlobalRules().get(0).getPattern()).isEqualTo("/api/**");
                    });
        }

        @Test
        @DisplayName("multiple global rules can be configured")
        void withMultipleGlobalRules_allParsed() {
            contextRunner
                    .withPropertyValues(
                            "rate-limiter.global-rules[0].pattern=/api/public/**",
                            "rate-limiter.global-rules[0].limit=200",
                            "rate-limiter.global-rules[1].pattern=/api/admin/**",
                            "rate-limiter.global-rules[1].limit=10")
                    .run(context -> {
                        RateLimitProperties props = context.getBean(RateLimitProperties.class);
                        assertThat(props.getGlobalRules()).hasSize(2);
                    });
        }
    }

    // -------------------------------------------------------------------------
    // Full SpringBootTest smoke test
    // -------------------------------------------------------------------------

    /**
     * Minimal Spring Boot application for the {@code @SpringBootTest} smoke test.
     * Defined as a static inner class so it is self-contained in this test file.
     */
    @org.springframework.boot.autoconfigure.SpringBootApplication(
            scanBasePackages = "io.github.tarunve.ratelimiter.autoconfigure")
    static class TestApp { }

    @SpringBootTest(
            classes = TestApp.class,
            webEnvironment = SpringBootTest.WebEnvironment.MOCK,
            properties = "spring.main.web-application-type=servlet")
    @DisplayName("SpringBootTest — full application context smoke test")
    @Nested
    class FullContextSmokeTest {

        @org.springframework.beans.factory.annotation.Autowired
        private RateLimiterService rateLimiterService;

        @org.springframework.beans.factory.annotation.Autowired
        private RateLimitKeyResolver keyResolver;

        @org.springframework.beans.factory.annotation.Autowired
        private RateLimitAspect rateLimitAspect;

        @org.springframework.beans.factory.annotation.Autowired
        private RateLimitProperties rateLimitProperties;

        @Test
        @DisplayName("application context loads successfully with all rate-limiter beans")
        void contextLoads() {
            assertThat(rateLimiterService).isNotNull().isInstanceOf(InMemoryRateLimiterService.class);
            assertThat(keyResolver).isNotNull().isInstanceOf(DefaultRateLimitKeyResolver.class);
            assertThat(rateLimitAspect).isNotNull();
            assertThat(rateLimitProperties).isNotNull();
            assertThat(rateLimitProperties.isEnabled()).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // Test configuration inner classes
    // -------------------------------------------------------------------------

    /** Provides a custom {@link RateLimiterService} to test {@code @ConditionalOnMissingBean}. */
    @TestConfiguration
    static class CustomRateLimiterConfig {
        @Bean
        RateLimiterService customRateLimiterService() {
            return new RateLimiterService() {
                @Override
                public io.github.tarunve.ratelimiter.model.RateLimitResult tryConsume(
                        String key, int limit, long durationNanos) {
                    return io.github.tarunve.ratelimiter.model.RateLimitResult.allowed(limit - 1);
                }
                @Override public String backendName() { return "custom-test"; }
            };
        }
    }

    /** Provides a custom {@link RateLimitKeyResolver} to test {@code @ConditionalOnMissingBean}. */
    @TestConfiguration
    static class CustomKeyResolverConfig {
        @Bean
        RateLimitKeyResolver customKeyResolver() {
            return new NoOpKeyResolver();
        }

        static class NoOpKeyResolver implements RateLimitKeyResolver {
            @Override
            public String resolve(
                    io.github.tarunve.ratelimiter.annotation.RateLimit annotation,
                    org.aspectj.lang.ProceedingJoinPoint joinPoint,
                    jakarta.servlet.http.HttpServletRequest request) {
                return "no-op";
            }
        }
    }
}
