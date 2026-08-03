package com.spring.rest.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
class RateLimitConfig {

    private String user;
    private String uri;
    private int limit;

}
