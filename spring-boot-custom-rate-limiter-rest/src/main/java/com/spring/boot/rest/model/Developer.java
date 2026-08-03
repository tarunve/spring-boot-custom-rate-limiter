package com.spring.boot.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Developer {

    private int id;
    private String name;
    private String type;
    private String designation;
    private String organization;
    private String location;

}
