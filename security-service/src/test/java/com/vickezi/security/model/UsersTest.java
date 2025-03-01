package com.vickezi.security.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UsersTest {

    @Test
    void addGroupToUser_SingleGroup() {
        Users user = new Users();
        Groups group = new Groups("ADMIN");

        user.addGroupToUser(group);

        assertEquals(1, user.getGroupsSet().size());
        assertTrue(user.getGroupsSet().contains(group));
    }

    @Test
    void addGroupsToUser_MultipleGroups() {
        Users user = new Users();
        Set<Groups> groups = Set.of(
                new Groups("ADMIN"),
                new Groups("USER")
        );

        user.addGroupsToUser(groups);

        assertEquals(2, user.getGroupsSet().size());
        assertTrue(user.getGroupsSet().containsAll(groups));
    }

    @Test
    void removeGroupsFromUser_ExistingGroup() {
        Users user = new Users();
        Groups group = new Groups("ADMIN");
        user.addGroupToUser(group);

        user.removeGroupsFromUser(group);

        assertTrue(user.getGroupsSet().isEmpty());
    }

    @Test
    void removeGroupsFromUser_NonExistingGroup() {
        Users user = new Users();
        Groups existingGroup = new Groups("ADMIN");
        Groups nonExistingGroup = new Groups("USER");
        user.addGroupToUser(existingGroup);

        user.removeGroupsFromUser(nonExistingGroup);

        assertEquals(1, user.getGroupsSet().size());
        assertTrue(user.getGroupsSet().contains(existingGroup));
    }

    @Test
    void equals_SameUsername() {
        Users user1 = new Users.UserBuilder(null,"user1",null,null ).build();

        Users user2 = new Users.UserBuilder(null,"user2",null,null ).build();


        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    void equals_DifferentUsername() {
        Users user1 = new Users.UserBuilder(null,"user1",null,null ).build();

        Users user2 = new Users.UserBuilder(null,"user2",null,null ).build();

        assertNotEquals(user1, user2);
        assertNotEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    void equals_NullUsername() {
        Users user1 = new Users();
        Users user2 = new Users();

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    void equals_NullObject() {
        Users user = new Users.UserBuilder(null,"user1",null,null ).build();

        assertNotEquals(null, user);
    }
}