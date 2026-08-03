package com.spring.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserApiKey {

    private String user;
    private String uri;

    @Override
    public String toString(){
        return this.user + "-" + this.uri;
    }
}
