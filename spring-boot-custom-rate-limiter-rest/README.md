## Custom Rate Limiter Rest API

---

### Information about the project

*   Two endpoints defined for sample: 
    * ***/api/v1/developers***
    * ***/api/v1/organizations***

*   Rate Limiter Custom Configuration for these endpoints
      ``` yaml
        custom:
          defaultRateLimit: 3
          refreshTimeInterval: 60
          rateLimits:
            - user: default
              uri:  /api/v1/developers
              limit:  5
            - user: test
              uri: /api/v1/organizations
              limit: 20
      ```

*   Here, assumption I made is that consumer application have spring security implementation so that user can be retrieved for the request hit.
*   If user is not retrieved, ***default*** user can be configured for rate limit for any specific API. If not, configured, default value will be assigned from parameter ***defaultRateLimit***.
*   ***refreshTimeInterval*** is the number of seconds for rate. After specified number of seconds, it will be refreshed.

---

### Installation 

*   Setup Java, Maven in your system.
*   Setup this project. 
*   Run Spring boot application - ***SpringBootCustomRateLimiterRestApplication***

---