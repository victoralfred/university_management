package com.vickezi.registration.model;

import java.util.UUID;

public class Users{
    private UUID id;
    private String email;

    public Users() {
        id = UUID.randomUUID();
    }

    public Users(UUID id, String email) {
        this.id = id;
        this.email = email;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
