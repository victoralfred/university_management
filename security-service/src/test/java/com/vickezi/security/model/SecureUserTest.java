package com.vickezi.security.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecureUserTest {
    @Test
    void userWithMultipleGroupsHasAllAuthorities() {
        Set<Groups> groups = Set.of(
                new Groups(Set.of(new Roles("ROLE_ADMIN"), new Roles("ROLE_USER"))),
                new Groups(Set.of(new Roles("ROLE_MANAGER")))
        );
        Users user = new Users();
        user.setGroupsSet(groups);
        SecureUser secureUser = new SecureUser(user);

        Collection<? extends GrantedAuthority> authorities = secureUser.getAuthorities();

        assertEquals(3, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_MANAGER")));
    }

    @Test
    void userWithNoGroupsHasNoAuthorities() {
        Users user = new Users();
        user.setGroupsSet(Collections.emptySet());
        SecureUser secureUser = new SecureUser(user);

        Collection<? extends GrantedAuthority> authorities = secureUser.getAuthorities();

        assertTrue(authorities.isEmpty());
    }

    @Test
    void userDetailsReflectUnderlyingUserState() {
        Users user = new Users();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setEnabled(true);
        SecureUser secureUser = new SecureUser(user);

        assertEquals("testuser", secureUser.getUsername());
        assertEquals("password123", secureUser.getPassword());
        assertTrue(secureUser.isAccountNonExpired());
        assertTrue(secureUser.isAccountNonLocked());
        assertTrue(secureUser.isCredentialsNonExpired());
        assertTrue(secureUser.isEnabled());
    }

    @Test
    void disabledUserStateIsReflected() {
        Users user = new Users();
        user.setEnabled(false);
        user.setAccountNonExpired(false);
        user.setAccountNonLocked(false);
        user.setCredentialsNonExpired(false);
        SecureUser secureUser = new SecureUser(user);

        assertFalse(secureUser.isEnabled());
        assertFalse(secureUser.isAccountNonExpired());
        assertFalse(secureUser.isAccountNonLocked());
        assertFalse(secureUser.isCredentialsNonExpired());
    }
}