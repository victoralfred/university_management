package com.vickezi.userservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID userId;
    // Unique username for authentication
    @Column(nullable = false, unique = true)
    private String username;
    private String email;
    // Hashed password (should be stored securely)
    @Column(nullable = false)
    @JsonIgnore
    private String password;
    @Column(nullable = false)
    public boolean isAccountNonExpired;
    @Column(nullable = false)
    public boolean isAccountNonLocked;
    @Column(nullable = false)
    public boolean isCredentialsNonExpired;
    @Column(nullable = false)
    public boolean isEnabled;
    // A user belongs to one or more groups
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_groups",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<Groups> groups = new HashSet<>();

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public UUID getUserId() {
        return userId;
    }
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return isAccountNonExpired;
    }
    public void setAccountNonExpired(boolean accountNonExpired) {
        isAccountNonExpired = accountNonExpired;
    }
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return isAccountNonLocked;
    }
    public void setAccountNonLocked(boolean accountNonLocked) {
        isAccountNonLocked = accountNonLocked;
    }
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return isCredentialsNonExpired;
    }
    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        isCredentialsNonExpired = credentialsNonExpired;
    }
    @JsonIgnore
    public boolean isEnabled() {
        return isEnabled;
    }
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public Set<Groups> getGroupsSet() {
        return groups;
    }
    public void setGroupsSet(Set<Groups> groupsSet) {
        if(this.groups==null){
            this.groups= new HashSet<>();
        }
        this.groups = groupsSet;
    }
    public void addGroupToUser(Groups group){
        if(this.groups==null){
            this.groups = new HashSet<>();
        }
        this.groups.add(group);
    }
    public void addGroupsToUser(Collection<Groups> groups){
        if(this.groups==null){
            this.groups = new HashSet<>();
        }
        this.groups.addAll(groups);
    }
    public void removeGroupsFromUser(Groups group){
            this.groups.remove(group);
    }
    @Override
    public boolean equals(Object o) {
        if(null == o) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof Users user)) {
            return false;
        }
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
    @Override
    public String toString() {
        return "Users{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", isAccountNonExpired=" + isAccountNonExpired +
                ", isAccountNonLocked=" + isAccountNonLocked +
                ", isCredentialsNonExpired=" + isCredentialsNonExpired +
                ", isEnabled=" + isEnabled +
                '}';
    }

}
