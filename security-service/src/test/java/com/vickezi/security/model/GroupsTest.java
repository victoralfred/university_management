package com.vickezi.security.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GroupsTest {
    @Test
    void createGroupWithName() {
        Groups group = new Groups("admins");
        assertEquals("admins", group.getName());
        assertNotNull(group.getUsers());
        assertTrue(group.getUsers().isEmpty());
        assertNotNull(group.getRoles());
        assertTrue(group.getRoles().isEmpty());
    }

    @Test
    void createGroupWithRoles() {
        Set<Roles> roles = new HashSet<>();
        roles.add(new Roles("ADMIN"));
        roles.add(new Roles("USER"));

        Groups group = new Groups(roles);
        assertEquals(roles, group.getRoles());
        assertNotNull(group.getUsers());
        assertTrue(group.getUsers().isEmpty());
    }

    @Test
    void createEmptyGroup() {
        Groups group = new Groups();
        assertNull(group.getName());
        assertNotNull(group.getUsers());
        assertTrue(group.getUsers().isEmpty());
        assertNotNull(group.getRoles());
        assertTrue(group.getRoles().isEmpty());
    }

    @Test
    void setGroupName() {
        Groups group = new Groups();
        group.setName("moderators");
        assertEquals("moderators", group.getName());
    }

    @Test
    void addUserToGroup() {
        Groups group = new Groups("testers");
        Users user = new Users.UserBuilder(null,"user1",null,null )
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();
        group.getUsers().add(user);
        assertEquals(1, group.getUsers().size());
        assertTrue(group.getUsers().contains(user));
    }

    @Test
    void addRoleToGroup() {
        Groups group = new Groups("analysts");
        Roles role = new Roles("ANALYST");
        group.getRoles().add(role);
        assertEquals(1, group.getRoles().size());
        assertTrue(group.getRoles().contains(role));
    }

}