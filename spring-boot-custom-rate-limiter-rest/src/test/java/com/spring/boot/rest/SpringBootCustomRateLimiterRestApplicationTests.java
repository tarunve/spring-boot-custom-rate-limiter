package com.spring.boot.rest;

import com.spring.boot.rest.controller.DeveloperController;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpringBootCustomRateLimiterRestApplicationTests {

    @Autowired
    private DeveloperController developerController;

    @Test
    void contextLoads() {
        Assertions.assertThat(developerController).isNotNull();
    }

    @Test
    public void testMainMethod() {
        SpringBootCustomRateLimiterRestApplication.main(new String[]{});
    }

}
