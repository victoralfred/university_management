package com.vickezi.userservice.model;

import jakarta.persistence.*;

import java.util.Objects;
@Entity
@Table(name = "roles")
public class Roles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;

    public Roles(String name) {
        this.name = name;
    }

    public Roles() {
    }

    public String getRole() {
        return name;
    }
    // Getters and setters
    public Long getId() { return id; }
    public void setAuthority(String authority) { this.name = authority; }
    @Override
    public boolean equals(Object o) {
        if(null==o) return false;
        if (this == o) return true;
        if (!(o instanceof Roles roles)) return false;
        return Objects.equals(name, roles.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
