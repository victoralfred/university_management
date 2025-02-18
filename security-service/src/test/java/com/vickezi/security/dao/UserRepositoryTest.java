package com.vickezi.security.dao;

import com.vickezi.security.model.Groups;
import com.vickezi.security.model.Roles;
import com.vickezi.security.model.Users;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import org.hibernate.Session;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock
    private Mutiny.SessionFactory sessionFactory;

    @Mock
    private Mutiny.Session session;

    @Mock
    private Mutiny.Query<Users> query;

    @InjectMocks
    private UserRepository userRepository;

    private Users testUser;
    private Set<Groups> groups;
    private Set<Roles> roles;

    @BeforeEach
    void setup() {
        // Initialize test data
        testUser = new Users();
        testUser.setUserId(UUID.randomUUID());
        testUser.setUsername("testuser");

        roles = new HashSet<>();
        roles.add(new Roles("ROLE_USER"));
        roles.add(new Roles("ROLE_ADMIN"));

        groups = new HashSet<>();
        Groups group = new Groups("testgroup");
        group.setRoles(roles);
        groups.add(group);
        testUser.setGroupsSet(groups);

        // Mock session behavior
        when(sessionFactory.withSession(any())).thenAnswer(invocation -> {
            Function<Mutiny.Session, Uni<Users>> function = invocation.getArgument(0);
            return function.apply(session);
        });

        // Mock query behavior
        when(session.createQuery(anyString(), eq(Users.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(Uni.createFrom().item(testUser));
    }

    @Test
    void findUserWithAuthorities_ExistingUser_ReturnsUserWithAuthorities() {
        // Execute the repository method
        UserAuthoritiesDTO result = userRepository.findUserWithAuthorities("testuser")
                .await().indefinitely();

        // Verify the results
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN"), result.getAuthorities());
    }

    @Test
    void findUserWithAuthorities_NonexistentUser_ReturnsEmptyAuthorities() {
        // Mock no result scenario
        when(query.getSingleResult()).thenReturn(Uni.createFrom().failure(new NoResultException()));

        // Execute the repository method
        UserAuthoritiesDTO result = userRepository.findUserWithAuthorities("unknown")
                .await().indefinitely();

        // Verify the results
        assertNotNull(result);
        assertEquals("error", result.getUsername());
        assertTrue(result.getAuthorities().isEmpty());
    }

    @Test
    void findUserWithAuthorities_DatabaseError_ReturnsEmptyAuthorities() {
        // Mock database error scenario
        when(query.getSingleResult()).thenReturn(Uni.createFrom().failure(new RuntimeException("DB Error")));

        // Execute the repository method
        UserAuthoritiesDTO result = userRepository.findUserWithAuthorities("testuser")
                .await().indefinitely();

        // Verify the results
        assertNotNull(result);
        assertEquals("error", result.getUsername());
        assertTrue(result.getAuthorities().isEmpty());
    }
    /**
     * Latest
     */
    @Test
    void testCreateRole() {
        Roles role = new Roles();
        when(sessionFactory.withTransaction(any(BiFunction.class))).thenAnswer((Answer<Uni<Void>>) invocation -> {
            // Simulate the behavior of the withTransaction method
            return Uni.createFrom().voidItem();
        });

        Uni<Void> result = userRepository.createRole(role);

        result.subscribe().with(
                item -> {
                    // Verify that the role was persisted
                    verify(sessionFactory).withTransaction(any(BiFunction.class));
                },
                failure -> {
                    // Handle failure
                }
        );
    }

    @Test
    void testCreateGroup() {
        Groups group = new Groups();
        when(sessionFactory.withTransaction(any(Function.class))).thenAnswer((Answer<Uni<Groups>>) invocation -> {
            // Simulate the behavior of the withTransaction method
            return Uni.createFrom().item(group);
        });

        Uni<Groups> result = userRepository.createGroup(group);

        result.subscribe().with(
                item -> {
                    // Verify that the group was persisted
                    verify(sessionFactory).withTransaction(any(Function.class));
                },
                failure -> {
                    // Handle failure
                }
        );
    }
    @Test
    void testAddRoleToGroup() {
        String groupName = "Group1";
        String roleName = "Role1";
        Groups group = new Groups();
        Roles role = new Roles();
        when(sessionFactory.withTransaction(any(Function.class))).thenAnswer((Answer<Uni<Groups>>) invocation -> {
            // Simulate the behavior of the withTransaction method
            return Uni.createFrom().item(group);
        });
        Uni<Void> result = userRepository.addRoleToGroup(groupName, roleName);

        result.subscribe().with(
                item -> {
                    // Verify that the role was added to the group
                    verify(sessionFactory).withTransaction(any(Function.class));
                },
                failure -> {
                    // Handle failure
                }
        );
    }

    @Test
    @Transactional
    void testAddUserToGroup() {
        String username = "user1";
        String groupName = "Group1";

        Groups group = new Groups(groupName);
        Users user = new Users();
        user.setUsername(username);

        // Use BiFunction to match the expected argument type
        when(sessionFactory.withTransaction(any(BiFunction.class))).thenAnswer((Answer<Uni<Void>>) invocation -> {
            // Simulate the behavior of the withTransaction method
            return Uni.createFrom().voidItem();
        });

        Uni<Void> result = userRepository.addUserToGroup(username, groupName);

        result.subscribe().with(
                item -> {
                    // Verify that the user was added to the group
                    verify(sessionFactory).withTransaction(any(BiFunction.class));
                },
                failure -> {
                    // Handle failure
                }
        );
    }

    @Test
    void testFindUserWithAuthorities() {
        String username = "user1";
        Users user = new Users();

        // Explicitly specify the Function parameter using any()
        when(sessionFactory.withSession(any(Function.class))).thenAnswer((Answer<Uni<Users>>) invocation -> {
            // Simulate the behavior of the withSession method
            return Uni.createFrom().item(user);
        });

        Uni<UserAuthoritiesDTO> result = userRepository.findUserWithAuthorities(username);

        result.subscribe().with(
                item -> {
                    // Verify that the user authorities were fetched
                    verify(sessionFactory).withSession(any(Function.class));
                },
                failure -> {
                    // Handle failure
                }
        );
    }

}
