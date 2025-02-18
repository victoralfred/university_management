package com.vickezi.security.dao;


import java.util.Set;
import java.util.UUID;

public class UserAuthoritiesDTO {
    private UUID userId;
    private String username;
    private Set<String> authorities;

    // Constructors
    public UserAuthoritiesDTO() {}

    public UserAuthoritiesDTO(UUID userId, String username, Set<String> authorities) {
        this.userId = userId;
        this.username = username;
        this.authorities = authorities;
    }

    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = authorities;
    }
}