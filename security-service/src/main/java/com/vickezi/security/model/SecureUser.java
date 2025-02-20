package com.vickezi.security.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.stream.Collectors;
/**
 * SecureUser class implements the UserDetails interface to provide user information
 * to Spring Security.
 */
public class SecureUser implements UserDetails {
    private final Users user;
    /**
     * Constructor to initialize SecureUser with a Users object.
     *
     * @param user the Users object containing user details.
     */
    public SecureUser(Users user) {
        this.user = user;
    }
    /**
     * Returns a collection of GrantedAuthority objects representing the roles of the user.
     * This is a part of the Spring Security process for assigning permissions and authorities to the user.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.user.getGroupsSet().stream()
                // Flatten the group-role relationships. For each group, extract its roles into the stream.
                .flatMap(groups -> groups.getRoles().stream()
                        // Convert each role to a GrantedAuthority (SimpleGrantedAuthority) object.
                        .map(role -> new SimpleGrantedAuthority(
                                role.getRole()
                        )))// Role name to authority object.
                .distinct()  // Ensure that the returned list has unique roles.
                .collect(Collectors.toList());  // Collect the authorities into a list for Spring Security.
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
