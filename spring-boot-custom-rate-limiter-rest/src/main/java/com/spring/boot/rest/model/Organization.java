package com.spring.boot.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Organization {

    private int id;
    private String name;
    private String location;
    private int numEmployees;
    private int rating;

}
