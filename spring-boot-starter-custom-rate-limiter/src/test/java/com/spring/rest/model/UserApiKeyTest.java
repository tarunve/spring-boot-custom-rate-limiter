package com.spring.rest.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserApiKeyTest {

    private UserApiKey userApiKey1;
    private UserApiKey userApiKey2;

    @BeforeEach
    public void setup() {
        userApiKey1 = new UserApiKey("tarun", "/api");
        userApiKey2 = new UserApiKey("tarun", "/api");
    }

    @Test
    public void testUserApiKey(){
        Assertions.assertEquals("tarun", userApiKey1.getUser());
        Assertions.assertNotEquals("/api/v1", userApiKey1.getUri());
    }

    @Test
    public void testDefaultUserApiKey(){
        UserApiKey userApiKey = new UserApiKey();
        Assertions.assertNull(userApiKey.getUser());
    }

    @Test
    public void testNullValues(){
        UserApiKey userApiKey = new UserApiKey(null, null);
        Assertions.assertEquals(31*7*31 , userApiKey.hashCode());
    }

    @Test
    public void testEqualObjects(){
        Assertions.assertEquals(userApiKey1 , userApiKey2);
    }

    @Test
    public void testEqualReferenceObjects(){
        Assertions.assertEquals(userApiKey1 , userApiKey1);
    }

    @Test
    public void testNullObjects(){
        UserApiKey userApiKey3 = null;
        Assertions.assertNotEquals(userApiKey1 , userApiKey3);
    }

    @Test
    public void testToString(){
        Assertions.assertNotEquals("tarun" , userApiKey1.toString());
    }
}
