package com.vickezi.security.dao;

import com.vickezi.security.model.Groups;
import com.vickezi.security.model.Roles;
import com.vickezi.security.model.Users;
import io.smallrye.mutiny.Uni;
import jakarta.transaction.Transactional;
import org.hibernate.reactive.mutiny.Mutiny;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class UserRepository {
    private final Mutiny.SessionFactory sessionFactory;
    private final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    public UserRepository(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    @Transactional
    public Uni<Void> createRole(Roles role) {
        return sessionFactory.withTransaction((session, tx) ->
                session.persist(role)
        ).replaceWithVoid();
    }
    @Transactional
    public Uni<Groups> createGroup(Groups group) {
        return sessionFactory.withTransaction((session, tx) ->
                session.persist(group).replaceWith(group)
        );
    }
    @Transactional
    public Uni<Void> createGroup(String groupName, Set<String> roleNames) {
        return sessionFactory.withTransaction((session, tx) -> {
            Groups group = new Groups();
            group.setName(groupName);

            // Fetch roles by names
            Uni<List<Roles>> rolesUni = session.createQuery("FROM Roles WHERE name IN :names", Roles.class)
                    .setParameter("names", roleNames)
                    .getResultList();

            return rolesUni.onItem().transformToUni(roles -> {
                group.getRoles().addAll(roles);
                return session.persist(group);
            });
        }).replaceWithVoid();
    }
    @Transactional
    public Uni<Users> createUser(Users user) {
        return sessionFactory.withTransaction((session, tx) ->
                session.persist(user).replaceWith(user)
        );
    }
    @Transactional
    public Uni<Users> findUserByUsername(String username) {
        return sessionFactory.withSession(session ->
                session.createQuery("FROM Users WHERE username = :username", Users.class)
                        .setParameter("username", username)
                        .getSingleResultOrNull()
        );
    }
    @Transactional
    public Uni<Void> addRoleToGroup(String groupName, String roleName) {
        return sessionFactory.withTransaction((session, tx) -> {
            Uni<Groups> groupUni = session.createQuery("FROM Groups WHERE name = :name", Groups.class)
                    .setParameter("name", groupName)
                    .getSingleResultOrNull();

            Uni<Roles> roleUni = session.createQuery("FROM Roles WHERE name = :name", Roles.class)
                    .setParameter("name", roleName)
                    .getSingleResultOrNull();

            return Uni.combine().all().unis(groupUni, roleUni).asTuple()
                    .onItem().transformToUni(tuple -> {
                        Groups group = tuple.getItem1();
                        Roles role = tuple.getItem2();

                        if (group == null || role == null) {
                            return Uni.createFrom().failure(new IllegalArgumentException("Group or Role not found"));
                        }

                        group.getRoles().add(role);
                        return session.merge(group).replaceWithVoid();
                    });
        });
    }
    public Uni<UserAuthoritiesDTO> findUserWithAuthorities(String username) {
        return sessionFactory.withSession(session ->
                session.createQuery(
                                "SELECT u FROM Users u " +
                                        "LEFT JOIN FETCH u.groups g " +
                                        "LEFT JOIN FETCH g.roles " +
                                        "WHERE u.userId = :userId", Users.class)
                        .setParameter("username", username)
                        .getSingleResult()
                        .map(user -> {
                            Set<String> authorities = user.getGroupsSet().stream()
                                    .flatMap(group -> group.getRoles().stream())
                                    .map(Roles::getRole)
                                    .collect(Collectors.toSet());
                            return new UserAuthoritiesDTO(
                                    user.getUserId(),
                                    user.getUsername(),
                                    authorities
                            );
                        }).onFailure().recoverWithItem(e -> {
                            logger.error("Failed to fetch authorities for user {}", username, e);
                            return new UserAuthoritiesDTO(null, "error", Set.of());
                        })
        );
    }
    @Transactional
    public Uni<Void> addUserToGroup(String username, String groupName) {
        return sessionFactory.withTransaction((session, tx) -> {
            Uni<Users> userUni = findUserByUsername(username);
            Uni<Groups> groupUni = session.createQuery("FROM Groups WHERE name = :name", Groups.class)
                    .setParameter("name", groupName)
                    .getSingleResultOrNull();

            return Uni.combine().all().unis(userUni, groupUni).asTuple()
                    .onItem().transformToUni(tuple -> {
                        Users user = tuple.getItem1();
                        Groups group = tuple.getItem2();

                        if (user == null || group == null) {
                            return Uni.createFrom().failure(new IllegalArgumentException("User or Group not found"));
                        }

                        user.getGroupsSet().add(group);
                        return session.merge(user).replaceWithVoid();
                    });
        });
    }

}

