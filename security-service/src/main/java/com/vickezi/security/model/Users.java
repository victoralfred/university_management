package com.vickezi.security.model;

import jakarta.persistence.FetchType;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    private boolean isAccountNonExpired;
    @Column(nullable = false)
    private boolean isAccountNonLocked;
    @Column(nullable = false)
    private boolean isCredentialsNonExpired;
    @Column(nullable = false)
    private boolean isEnabled;
    // A user belongs to one or more groups
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_groups",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<Groups> groups = new HashSet<>();

    public Users(UserBuilder builder){
        this.userId = builder.userId;
        this.username = builder.username;
        this.email = builder.email;
        this.password = builder.password;
        this.isAccountNonExpired = builder.isAccountNonExpired;
        this.isAccountNonLocked = builder.isAccountNonLocked;
        this.isCredentialsNonExpired = builder.isCredentialsNonExpired;
        this.isEnabled = builder.isEnabled;
        this.groups = builder.groups;
    }

    public Users() {

    }

    public String getUsername() {
        return username;
    }
    public UUID getUserId() {
        return userId;
    }
    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return isAccountNonExpired;
    }
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return isAccountNonLocked;
    }
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return isCredentialsNonExpired;
    }
    @JsonIgnore
    public boolean isEnabled() {
        return isEnabled;
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
    public static class UserBuilder{
        public final UUID userId;
        private final String username;
        private final String email;
        private final  String password;
        private boolean isAccountNonExpired;
        private boolean isAccountNonLocked;
        private boolean isCredentialsNonExpired;
        private boolean isEnabled;
        private Set<Groups> groups = new HashSet<>();

        public UserBuilder(UUID userId, String username, String email, String password) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.password = password;
        }
        public UserBuilder  isAccountNonExpired(boolean value) {
            this.isAccountNonExpired = value;
            return this;
        }
        public UserBuilder isAccountNonLocked(boolean value) {
            this.isAccountNonLocked = value;
            return this;
        }
        public UserBuilder isCredentialsNonExpired(boolean value) {
            this.isCredentialsNonExpired = value;
            return this;
        }
        public UserBuilder isEnabled(boolean value) {
            this.isEnabled = value;
            return this;
        }
        public UserBuilder groups(Set<Groups> groups){
                this.groups = groups;
                return this;
        }
        public Users build(){
            return new Users(this);
        }
    }
}
