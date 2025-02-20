package com.vickezi.security.dao;
import com.vickezi.security.model.Users;

import java.util.Set;

public class UserAuthoritiesDTO {
    private Users user;
    private  Set<String> authorities;
    private  Set<String> groups;
    public UserAuthoritiesDTO() {}
    public UserAuthoritiesDTO(Builder builder){
        this.user = builder.users;
        this.authorities = builder.authorities;
        this.groups = builder.groups;
    }

    public Users getUsers() {
        return user;
    }

    public Users getUser() {
        return user;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public static class Builder{
        private final Users users;
        private  Set<String> authorities;
        private  Set<String> groups;
        public Builder(Users users){
            this.users = users;
        }
        public Builder setAuthorities(Set<String> authorities){
            this.authorities = authorities;
            return this;
        }
        public Builder setGroups(Set<String> groups){
            this.groups = groups;
            return this;
        }
        public UserAuthoritiesDTO build(){
            return new UserAuthoritiesDTO(this);
        }
    }

}