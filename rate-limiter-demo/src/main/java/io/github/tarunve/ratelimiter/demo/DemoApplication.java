package io.github.tarunve.ratelimiter.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the rate-limiter demo application.
 *
 * <p>Run with:
 * <pre>{@code
 * mvn -pl rate-limiter-demo spring-boot:run
 * }</pre>
 *
 * <p>Then try the endpoints:
 * <ul>
 *   <li>{@code GET  /api/public/hello}    — 10 req / 10 s per IP (annotation)</li>
 *   <li>{@code GET  /api/public/weather}  — global rule: 200 req / 60 s per IP</li>
 *   <li>{@code POST /api/orders}          — 5 req / 60 s per user (annotation)</li>
 *   <li>{@code GET  /api/tenant/data}     — 100 req / 60 s per X-Tenant-Id header</li>
 * </ul>
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
