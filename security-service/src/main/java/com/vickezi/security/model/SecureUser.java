package com.vickezi.security.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

public class SecureUser implements UserDetails {
    private final Users user;
    public SecureUser(Users user) {
        this.user = user;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.user.getGroupsSet().stream()
                .flatMap(groups -> groups.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(
                                role.getRole()
                        )))
                .collect(Collectors.toList());
    }
    @Override
    public String getPassword() {
        return user.getPassword();
    }
    @Override
    public String getUsername() {
        return user.getUsername();
    }
    @Override
    public boolean isAccountNonExpired() {
        return user.isAccountNonExpired();
    }
    @Override
    public boolean isAccountNonLocked() {
        return user.isAccountNonLocked();
    }
    @Override
    public boolean isCredentialsNonExpired() {
        return user.isCredentialsNonExpired();
    }
    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
