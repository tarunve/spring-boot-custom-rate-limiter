# 🚦 Spring Boot Distributed Rate Limiter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.tarunve/rate-limiter-spring-boot-starter.svg)](https://central.sonatype.com/artifact/io.github.tarunve/rate-limiter-spring-boot-starter)
[![Build](https://github.com/tarunve/spring-boot-custom-rate-limiter/actions/workflows/ci.yml/badge.svg)](https://github.com/tarunve/spring-boot-custom-rate-limiter/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)
[![Spring Boot 3.x](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

Production-ready **distributed rate limiting** for Spring Boot applications. Drop in one dependency, annotate your methods — done.

Works out of the box with **in-memory** storage (no dependencies), or plug in **Redis** for distributed environments.

---

## ✨ Features

- 🎯 **Annotation-based** — `@RateLimit` on any controller method
- 🔄 **Token Bucket algorithm** — smooth, burst-friendly rate limiting
- 💾 **In-memory backend** — zero config, perfect for single-instance apps
- 🌐 **Redis backend** — distributed rate limiting across multiple instances
- 🔑 **Flexible key resolution** — by IP, by user principal, or custom SpEL expression
- ⚙️ **Global limits** — configure path-level limits in `application.yml`
- 🚀 **Spring Boot autoconfiguration** — no `@Enable*` annotation needed
- 🧩 **Spring Boot 3.x ready** — uses `jakarta.*` namespace
- ☕ **Java 17 & 21 compatible**

---

## 🚀 Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>io.github.tarunve</groupId>
    <artifactId>rate-limiter-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Annotate your endpoint

```java
@RestController
public class UserController {

    // 10 requests per minute per IP address
    @RateLimit(limit = 10, duration = 60)
    @GetMapping("/api/users")
    public List<User> getUsers() {
        return userService.findAll();
    }

    // 5 requests per minute per authenticated user
    @RateLimit(limit = 5, duration = 60, keyType = KeyType.USER)
    @GetMapping("/api/profile")
    public Profile getProfile(Principal principal) {
        return profileService.find(principal.getName());
    }

    // Custom key using SpEL
    @RateLimit(limit = 100, duration = 3600, key = "#userId")
    @GetMapping("/api/data/{userId}")
    public Data getData(@PathVariable String userId) {
        return dataService.find(userId);
    }
}
```

### 3. That is it. No other configuration needed.

---

## ⚙️ Configuration

```yaml
rate-limiter:
  enabled: true                  # default: true
  backend: IN_MEMORY             # IN_MEMORY or REDIS (default: IN_MEMORY)
  default-limit: 100             # fallback if no annotation (default: 100)
  default-duration: 60           # seconds (default: 60)

  # Optional: global path-level limits
  global-limits:
    - pattern: /api/public/**
      limit: 20
      duration: 60
    - pattern: /api/admin/**
      limit: 5
      duration: 60
```

### Redis Backend

Add Spring Data Redis to your project and set `backend: REDIS`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```yaml
rate-limiter:
  backend: REDIS

spring:
  data:
    redis:
      host: localhost
      port: 6379
```

No other changes needed. The starter auto-detects Redis and switches backends.

---

## 🪣 Why Token Bucket?

We chose the **Token Bucket** algorithm over alternatives for API rate limiting:

| Algorithm | Burst Handling | Memory | Distributed | Verdict |
|---|---|---|---|---|
| **Token Bucket** | ✅ Allows bursts | Low | ✅ Yes | **Our choice** |
| Fixed Window | ❌ Edge spikes | Low | ✅ Yes | Unfair at boundaries |
| Sliding Window | ❌ No burst | High | ⚠️ Complex | Memory intensive |
| Leaky Bucket | ❌ No burst | Low | ✅ Yes | Too strict for APIs |

**Token Bucket** works like a bucket that fills with tokens at a fixed rate. Each request consumes one token. If the bucket is empty, the request is rejected. This allows short bursts (a user doing a few quick actions) while still enforcing the overall rate — which is exactly what real-world API usage looks like.

We use [Bucket4j](https://bucket4j.com/) under the hood — battle-tested, used in production at scale.

---

## 📋 Response Headers

Every rate-limited response includes:

| Header | Description |
|---|---|
| `X-RateLimit-Limit` | Maximum requests allowed |
| `X-RateLimit-Remaining` | Tokens remaining in current window |
| `X-RateLimit-Retry-After` | Milliseconds until next token is available (on 429) |

---

## 🛑 Rate Limit Exceeded

When the limit is exceeded, the library returns:

```
HTTP 429 Too Many Requests
X-RateLimit-Remaining: 0
X-RateLimit-Retry-After: 45000

{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 45 seconds."
}
```

You can customise the response by providing your own `RateLimitExceededHandler` bean.

---

## 🔧 Custom Key Resolver

Implement `RateLimitKeyResolver` to define your own key strategy:

```java
@Component
public class TenantKeyResolver implements RateLimitKeyResolver {

    @Override
    public String resolve(HttpServletRequest request, Method method) {
        return request.getHeader("X-Tenant-ID");
    }
}
```

The custom resolver is auto-detected and used automatically.

---

## 📦 Module Structure

```
spring-boot-custom-rate-limiter/
├── rate-limiter-core/                  # Core algorithm + annotation + AOP
├── rate-limiter-spring-boot-starter/   # Autoconfiguration
└── rate-limiter-demo/                  # Working example app
```

---

## 🤝 Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md).

Areas to contribute:
- Additional algorithms (sliding window, fixed window)
- More key resolvers (by header, by JWT claim)
- Metrics integration (Micrometer)
- Rate limit configuration per HTTP method

---

## 📄 License

MIT License — see [LICENSE](LICENSE)

---

## ⭐ Support

If this project helps you, please give it a ⭐ — it helps others discover it and keeps the project going!
