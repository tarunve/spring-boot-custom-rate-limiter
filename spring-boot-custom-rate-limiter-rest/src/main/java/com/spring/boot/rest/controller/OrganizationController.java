package com.spring.boot.rest.controller;

import com.spring.boot.rest.model.Organization;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrganizationController {

    @GetMapping("/api/v1/organizations")
    public ResponseEntity<List<Organization>> retrieveOrganizations(){
        List<Organization> organizations = List.of(
                new Organization(1, "organization1", "pune", 10000, 4),
                new Organization(2, "organization2", "gurgaon", 5000, 5),
                new Organization(3, "organization3", "pune", 200000, 4)
        );
        return ResponseEntity.ok(organizations);
    }
}
