package com.vickezi.globals.model;


import java.io.Serializable;

public record EmailRegistrationEvent(String email) implements Serializable{
    @Override
    public String toString() {
        return "EmailRegistrationEvent{" +
                "email='" + email + '\'' +
                '}';
    }
}
