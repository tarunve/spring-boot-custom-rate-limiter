## Custom Rate Limiter Spring Boot Starter

---

### Information about the project

*   ***UserApiKey*** : Key definition by combining the username and request URI.
*   ***UserApiKeyConfig*** : Config Retrieval from ***application.properties*** file. 
*   ***PerUserRateLimitInterceptor*** : Interceptor created for the rate limit based on ***UserApiKey***.
    *   ***bucket4j*** library is used for the rate limit.
    *   it is a rate-limiting library based on token-bucket algorithm.


*   Sample configuration needed in the consumer application for rate limits definition:
      ``` yaml
        custom:
          defaultRateLimit: 3
          refreshTimeInterval: 60
          rateLimits:
            - user: user1
              uri:  /api/one
              limit:  5
            - user: user2
              uri: /api/one
              limit: 20
            - user: default
              uri: /api/two
              limit: 30
      ```
    
---