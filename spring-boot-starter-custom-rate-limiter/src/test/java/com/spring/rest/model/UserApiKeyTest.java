package com.spring.rest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertEquals("tarun", userApiKey1.getUser());
        assertNotEquals("/api/v1", userApiKey1.getUri());
    }

    @Test
    public void testDefaultUserApiKey(){
        UserApiKey userApiKey = new UserApiKey();
        assertNull(userApiKey.getUser());
    }

    @Test
    public void testNullValues(){
        UserApiKey userApiKey = new UserApiKey(null, null);
        assertEquals(31*7*31 , userApiKey.hashCode());
        assertEquals(false, userApiKey1.equals(userApiKey));
    }

    @Test
    public void testEqualMethod(){
    	UserApiKey userApiKey3 = new UserApiKey("tarun", null);
    	UserApiKey userApiKey4 = new UserApiKey(null, "/api");
    	
    	assertEquals(true, userApiKey1.equals(userApiKey1));
		assertEquals(false, userApiKey1.equals(null));
		assertEquals(false, userApiKey1.equals(new Object()));
		assertEquals(true, userApiKey1.equals(userApiKey2));
        assertEquals(false, userApiKey1.equals(userApiKey3));
        assertEquals(false, userApiKey1.equals(userApiKey4));
    }

    @Test
    public void testToString(){
        assertEquals("tarun-/api" , userApiKey1.toString());
    }
}