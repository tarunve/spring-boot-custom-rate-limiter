package com.spring.boot.rest.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserApiKeyTest {

    @Test
    public void testDeveloperTypeObjectCreation() {
        Developer developer = new Developer(1, "Tarun", "fullstack", "developer", "org1", "delhi");
        Assertions.assertEquals(1, developer.getId());
        Assertions.assertEquals("Tarun", developer.getName());
        Assertions.assertEquals("fullstack", developer.getType());
        Assertions.assertEquals("developer", developer.getDesignation());
        Assertions.assertEquals("org1", developer.getOrganization());
        Assertions.assertEquals("delhi", developer.getLocation());
        Assertions.assertEquals("Developer(id=1, name=Tarun, type=fullstack, designation=developer, organization=org1, location=delhi)", developer.toString());
    }

    @Test
    public void testOrgTypeObjectCreation() {
        Organization organization = new Organization(1, "Tarun", "pune", 10000, 4);
        Assertions.assertEquals(1, organization.getId());
        Assertions.assertEquals("Tarun", organization.getName());
        Assertions.assertEquals("pune", organization.getLocation());
        Assertions.assertNotEquals(900, organization.getNumEmployees());
        Assertions.assertNotEquals(5, organization.getRating());
        Assertions.assertNotEquals("test", organization.toString());
    }
}
