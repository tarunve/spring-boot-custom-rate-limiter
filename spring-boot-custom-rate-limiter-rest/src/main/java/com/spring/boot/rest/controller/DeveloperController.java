package com.spring.boot.rest.controller;

import com.spring.boot.rest.model.Developer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class DeveloperController {

    @GetMapping("/api/v1/developers")
    public ResponseEntity<List<Developer>> retrieveDevelopers(){
        List<Developer> developers = new ArrayList<>();
        developers.add(new Developer(1, "developer1", "fullstack", "developer", "organization1", "pune"));
        developers.add(new Developer(2, "developer2", "backend", "associate", "organization2", "pune"));
        developers.add(new Developer(3, "developer3", "frontend", "lead", "organization3", "gurgaon"));
        return ResponseEntity.ok(developers);
    }
}
