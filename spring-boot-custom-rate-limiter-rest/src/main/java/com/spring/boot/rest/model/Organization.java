package com.spring.boot.rest.model;

import lombok.Data;

@Data
public class Organization {
    private int id;
    private String name;
    private String location;
    private int numEmployees;
    private int rating;

    public Organization(int id, String name, String location, int num, int rating){
        this.id = id;
        this.name = name;
        this.location = location;
        this.numEmployees = num;
        this.rating = rating;
    }
}
