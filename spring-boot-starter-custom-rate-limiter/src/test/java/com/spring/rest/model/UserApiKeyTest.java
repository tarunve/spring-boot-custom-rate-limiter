package com.spring.rest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserApiKeyTest {

	@Test
    public void testUserApiKeyParameterized() {
        UserApiKey user1 = new UserApiKey("User1", "/api/one");
        
        assertEquals("User1", user1.getUser());
        assertEquals("/api/one", user1.getUri());
        assertEquals("User1-/api/one", user1.toString());
    }
	
	@Test
	public void testUserApiKeyNoParameterized() {
		UserApiKey user = new UserApiKey();
		assertNotNull(user);
	}
	
	@Test
	public void testUserApiKeyEqualsMethod() {
		UserApiKey user1 = new UserApiKey("User1", "/api/one");
        UserApiKey user2 = new UserApiKey("User2", "/api/two");
        UserApiKey user3 = new UserApiKey("User2", "/api/two");
        UserApiKey user4 = new UserApiKey("User2", "/api/four");
        UserApiKey user5 = new UserApiKey("User5", "/api/two");
        
        assertEquals(true, user1.equals(user1));
        assertEquals(false, user1.equals(user2));
        assertEquals(true, user2.equals(user3));
        assertEquals(false, user2.equals(user4));
        assertEquals(false, user2.equals(user5));
        assertEquals(false, user1.equals(null));
        assertEquals(false, user1.equals(new Object()));
	}
	
	@Test
	public void testUserApiKeyHashCodeMethod() {
		UserApiKey user1 = new UserApiKey(null, "/api/one");
        UserApiKey user2 = new UserApiKey("User2", null);
        UserApiKey user3 = new UserApiKey(null, null);
        
        assertNotNull(user1.hashCode());
        assertNotNull(user2.hashCode());
        assertNotNull(user3.hashCode());
	}
}