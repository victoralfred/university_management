package com.vickezi.security.model;

import com.vickezi.globals.util.CustomValidator;
import com.vickezi.security.dao.UserAuthoritiesDTO;
import com.vickezi.security.dao.reads.GroupAndRoleServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.HashSet;
import java.util.Set;
/**
 * CustomUserDetailsService is a service that implements the ReactiveUserDetailsService interface.
 * It is responsible for loading user-specific data.
 */
@Component
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
        logger.info("Authentication attempt for username/email: {}", username);
        final String authUserName = isEmailLogin(username);
        if (authUserName.isEmpty()) {
            loggWarning();
            return Mono.just(new SecureUser(defaultEmpty()));
        }
        return userRepository.findGroupsByUserNameOrEmail(authUserName)
                .defaultIfEmpty(new UserAuthoritiesDTO.Builder(defaultEmpty()).build())
                .flatMap(this::createUser);
    }
    /**
     * Creates a UserDetails object from the given UserAuthoritiesDTO.
     *
     * @param userAuthoritiesDTO the user authorities data transfer object
     * @return a Mono emitting the UserDetails
     */
    private Mono<UserDetails> createUser(final UserAuthoritiesDTO userAuthoritiesDTO){
        if(null==userAuthoritiesDTO.getUsers().userId){
            loggWarning();
            return Mono.empty();
        }
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
    private String isEmailLogin(String input){
        if(input.contains("@")){
            CustomValidator.isValidEmailFormat(input); // Sanitize the userInput
            return String.valueOf(CustomValidator.genericValidation(input)); // Sanitize the userInput
        }else {
            return CustomValidator.validateStringLiterals(input);
        }
    }
    private Users defaultEmpty(){
        return new Users.UserBuilder(null, null,null,null).build();
    }
    private void loggWarning(){
        logger.warn("Failed Authentication attempt: Empty username or sanitized input");
    }
}
