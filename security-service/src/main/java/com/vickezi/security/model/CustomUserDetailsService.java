package com.vickezi.security.model;

import com.vickezi.security.dao.UserAuthoritiesDTO;
import com.vickezi.security.dao.reads.GroupAndRoleServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;
/**
 * CustomUserDetailsService is a service that implements the ReactiveUserDetailsService interface.
 * It is responsible for loading user-specific data.
 */
@Configuration
public class CustomUserDetailsService implements ReactiveUserDetailsService {
    private final GroupAndRoleServiceImpl userRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    /**
     * Constructor for CustomUserDetailsService.
     *
     * @param userRepository the user repository service
     */
    public CustomUserDetailsService(GroupAndRoleServiceImpl userRepository) {
        this.userRepository = userRepository;
    }
    /**
     * Locates the user based on the username.
     *
     * @param username the username identifying the user whose data is required
     * @return a Mono emitting the UserDetails for the given username
     */
    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findGroupsByUserNameOrEmail(username)
                .flatMap(this::createUser);
    }
    /**
     * Creates a UserDetails object from the given UserAuthoritiesDTO.
     *
     * @param userAuthoritiesDTO the user authorities data transfer object
     * @return a Mono emitting the UserDetails
     */
    private Mono<UserDetails> createUser(final UserAuthoritiesDTO userAuthoritiesDTO){
        Users user = userAuthoritiesDTO.getUsers();
        Set<Groups> groups = getGroups(userAuthoritiesDTO);
        user.setGroupsSet(groups);
        SecureUser secureUser = new SecureUser(user);
        logger.info("Authorities: {}", secureUser.getAuthorities());
       return Mono.just(secureUser);
    }
    /**
     * Extracts groups and roles from the UserAuthoritiesDTO and creates a set of Groups.
     *
     * @param userAuthoritiesDTO the user authorities data transfer object
     * @return a set of Groups
     */
    private static Set<Groups> getGroups(UserAuthoritiesDTO userAuthoritiesDTO) {
        Set<String> group = userAuthoritiesDTO.getGroups();
        Set<String> role = userAuthoritiesDTO.getAuthorities();
        Set<Groups> groups = new HashSet<>();
        Set<Roles> roles = new HashSet<>();
        group.forEach(groupName -> groups.add(new Groups(groupName)));
        role.forEach(roleName -> roles.add(new Roles(roleName)));
        groups.forEach(result -> result.setRoles(roles));
        return groups;
    }
}
