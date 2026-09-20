package io.github.tarunve.ratelimiter.autoconfigure;

import io.github.tarunve.ratelimiter.aspect.RateLimitAspect;
import io.github.tarunve.ratelimiter.bucket.InMemoryRateLimiterService;
import io.github.tarunve.ratelimiter.bucket.RateLimiterService;
import io.github.tarunve.ratelimiter.bucket.RedisRateLimiterService;
import io.github.tarunve.ratelimiter.config.RateLimitProperties;
import io.github.tarunve.ratelimiter.config.RateLimitProperties.Backend;
import io.github.tarunve.ratelimiter.key.DefaultRateLimitKeyResolver;
import io.github.tarunve.ratelimiter.key.RateLimitKeyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Boot auto-configuration for the distributed rate limiter.
 *
 * <p>Activated automatically when the starter JAR is on the classpath.
 * Can be disabled entirely with:
 * <pre>{@code
 * rate-limiter:
 *   enabled: false
 * }</pre>
 *
 * <p><b>Bean registration summary</b></p>
 * <ul>
 *   <li>{@link RateLimitProperties} — bound to {@code rate-limiter.*}</li>
 *   <li>{@link RateLimitKeyResolver} — {@link DefaultRateLimitKeyResolver} unless overridden</li>
 *   <li>{@link RateLimiterService} — {@link InMemoryRateLimiterService} by default;
 *       {@link RedisRateLimiterService} when
 *       {@code rate-limiter.backend=REDIS} and Lettuce is on the classpath</li>
 *   <li>{@link RateLimitAspect} — wires the key resolver and service together</li>
 *   <li>{@link WebMvcConfigurer} — registers the global interceptor when
 *       {@code rate-limiter.global-rules} is non-empty</li>
 * </ul>
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "rate-limiter", name = "enabled", matchIfMissing = true)
public class RateLimiterAutoConfiguration {

    // -------------------------------------------------------------------------
    // Key resolver
    // -------------------------------------------------------------------------

    /**
     * Default key resolver — replaced by any user-defined
     * {@link RateLimitKeyResolver} bean.
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitKeyResolver.class)
    public RateLimitKeyResolver rateLimitKeyResolver() {
        log.debug("Registering DefaultRateLimitKeyResolver");
        return new DefaultRateLimitKeyResolver();
    }

    // -------------------------------------------------------------------------
    // In-memory backend (default)
    // -------------------------------------------------------------------------

    /**
     * Registers the in-memory backend when no other {@link RateLimiterService}
     * bean has been defined.  This is the safe default — it works with no
     * external infrastructure but is local to the JVM.
     */
    @Bean
    @ConditionalOnMissingBean(RateLimiterService.class)
    public RateLimiterService inMemoryRateLimiterService() {
        log.info("Rate limiter backend: IN_MEMORY (default)");
        return new InMemoryRateLimiterService();
    }

    // -------------------------------------------------------------------------
    // Redis backend (optional — activated by property + classpath)
    // -------------------------------------------------------------------------

    /**
     * Nested configuration class that is activated only when:
     * <ol>
     *   <li>{@code RedisConnectionFactory} is present on the classpath, <em>and</em></li>
     *   <li>{@code rate-limiter.backend=REDIS} is set in application properties.</li>
     * </ol>
     *
     * <p>The {@code @ConditionalOnMissingBean(RateLimiterService.class)} on the
     * inner bean means the Redis backend gracefully loses the contest to any
     * user-provided service bean.
     */
    @Configuration
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnProperty(prefix = "rate-limiter", name = "backend", havingValue = "REDIS")
    static class RedisRateLimiterConfiguration {

        @Bean
        @ConditionalOnMissingBean(RateLimiterService.class)
        public RateLimiterService redisRateLimiterService(RedisConnectionFactory connectionFactory) {
            log.info("Rate limiter backend: REDIS");
            return new RedisRateLimiterService(connectionFactory);
        }
    }

    // -------------------------------------------------------------------------
    // Aspect
    // -------------------------------------------------------------------------

    /**
     * Registers the AOP aspect that enforces {@code @RateLimit} on controller methods.
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    public RateLimitAspect rateLimitAspect(RateLimiterService rateLimiterService,
                                           RateLimitKeyResolver keyResolver) {
        log.debug("Registering RateLimitAspect (backend={})", rateLimiterService.backendName());
        return new RateLimitAspect(rateLimiterService, keyResolver);
    }

    // -------------------------------------------------------------------------
    // Global interceptor (optional)
    // -------------------------------------------------------------------------

    /**
     * Registers a {@link WebMvcConfigurer} that installs the
     * {@link RateLimitInterceptor} for URL patterns defined in
     * {@code rate-limiter.global-rules}.
     *
     * <p>Only registered when at least one global rule is configured.
     */
    @Bean
    @ConditionalOnMissingBean(name = "rateLimitWebMvcConfigurer")
    public WebMvcConfigurer rateLimitWebMvcConfigurer(RateLimitProperties properties,
                                                      RateLimiterService rateLimiterService) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                if (CollectionUtils.isEmpty(properties.getGlobalRules())) {
                    log.debug("No global rate-limit rules configured; skipping interceptor registration");
                    return;
                }

                RateLimitInterceptor interceptor =
                        new RateLimitInterceptor(rateLimiterService, properties);

                String[] patterns = properties.getGlobalRules().stream()
                        .map(RateLimitProperties.GlobalRule::getPattern)
                        .toArray(String[]::new);

                registry.addInterceptor(interceptor).addPathPatterns(patterns);
                log.info("Global rate-limit interceptor registered for {} pattern(s): {}",
                        patterns.length, java.util.Arrays.toString(patterns));
            }
        };
    }
}
