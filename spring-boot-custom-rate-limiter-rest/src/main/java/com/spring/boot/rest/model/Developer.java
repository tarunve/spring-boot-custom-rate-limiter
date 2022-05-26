package com.spring.boot.rest.model;

import lombok.Data;

@Data
public class Developer {
    private int id;
    private String name;
    private String type;
    private String designation;
    private String organization;
    private String location;

    public Developer(int id, String name, String type, String designation, String organization, String location){
        this.id = id;
        this.name = name;
        this.type = type;
        this.designation = designation;
        this.organization = organization;
        this.location = location;
    }
}
