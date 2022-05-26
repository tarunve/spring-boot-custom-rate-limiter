package com.spring.rest;

import com.spring.rest.config.UserApiKeyConfig;
import com.spring.rest.interceptor.PerUserRateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan
@ConfigurationPropertiesScan
@ConditionalOnClass(PerUserRateLimitInterceptor.class)
public class RateLimiterAutoConfiguration implements WebMvcConfigurer {

    @Autowired
    private PerUserRateLimitInterceptor perUserRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(perUserRateLimitInterceptor).addPathPatterns("/**");
    }

    @Bean
    @ConditionalOnMissingBean
    public PerUserRateLimitInterceptor getInstance(@Autowired UserApiKeyConfig userApiKeyConfig) {
        return new PerUserRateLimitInterceptor(userApiKeyConfig);
    }
}
