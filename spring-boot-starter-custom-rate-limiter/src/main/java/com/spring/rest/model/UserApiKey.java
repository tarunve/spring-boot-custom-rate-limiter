package com.spring.rest.model;

import lombok.Getter;

@Getter
public class UserApiKey {
    private String user;
    private String uri;

    UserApiKey() {}

    public UserApiKey(String user, String uri){
        this.uri = uri;
        this.user = user;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (user == null ? 0 : user.hashCode());
        hash = 31 * hash + (uri == null ? 0 : uri.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj){
            return true;
        }
        if(obj == null || obj.getClass() != this.getClass()){
            return false;
        }
        UserApiKey userApiKey = (UserApiKey) obj;
        return this.user.equals(userApiKey.getUser()) && this.uri.equals(userApiKey.getUri());
    }

    @Override
    public String toString(){
        return this.user + "-" + this.uri;
    }
}
