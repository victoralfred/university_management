package com.vickezi.security.dao.reads;

import com.vickezi.security.dao.UserAuthoritiesDTO;
import com.vickezi.security.model.Users;
import io.r2dbc.spi.Readable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import java.util.*;


@Service
public class GroupAndRoleServiceImpl {
    // SQL query to fetch user details, groups, and roles based on username or email
    private static final String USER_GROUP_QUERY = """
         SELECT u.userid, u.username, u.email, u.password,
           u.isaccountnonexpired, u.isaccountnonlocked, u.isenabled, u.iscredentialsnonexpired,
           g.name AS group_name, r.name AS role_name
         FROM users u
             JOIN users_groups ug ON u.userid = ug.user_id
             JOIN groups g ON ug.group_id = g.id
             JOIN groups_roles gr ON g.id = gr.group_id
             JOIN roles r ON gr.role_id = r.id
         WHERE u.username = :username OR u.email= :username
    """;
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final DatabaseClient databaseClient;
    // Constructor to initialize the database client
    public GroupAndRoleServiceImpl(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }
    /**
     * Finds user groups and roles by username or email.
     * @param username the username or email to search for.
     * @return a Mono containing UserAuthoritiesDTO with user details, groups, and roles.
     */
    public Mono<UserAuthoritiesDTO> findGroupsByUserNameOrEmail(String username) {
        return makeJDBCQuery(username);
    }
    /**
     * Executes the database query to fetch user details along with groups and roles.
     * @param username the username or email to search for.
     * @return a Mono containing UserAuthoritiesDTO if the user exists, otherwise empty.
     */
    private Mono<UserAuthoritiesDTO> makeJDBCQuery(String username) {
        return databaseClient.sql(USER_GROUP_QUERY)
                .bind("username", username)
                .map(this::transformRecord)
                .all()
                .collectList() // Collect all query results into a list
                .mapNotNull(results -> {
                    if (results.isEmpty()) {
                        return null; // Return null if no results found
                    }
                    final Set<String> groups = new HashSet<>(); // To store unique group names
                    final Set<String> roles = new HashSet<>();  // To store unique role names
                    Users user = null;
                    // Process each row in the results
                    for (Object[] row : results) {
                        user = (Users) row[0];  // Extract user object
                        groups.add((String) row[1]);  // Extract group name
                        roles.add((String) row[2]);   // Extract role name
                    }
                    // Build and return UserAuthoritiesDTO
                    return new UserAuthoritiesDTO.Builder(user)
                            .setGroups(groups)
                            .setAuthorities(roles)
                            .build();
                });
    }
    /**
     * Transforms a database row into an array containing user details, group, and role.
     * @param row the database row.
     * @return an array containing the user object, group name, and role name.
     */
    private Object[] transformRecord(Readable row) {
        // Extract user details from database row
        String loginName = row.get("username", String.class);
        UUID userId = row.get("userid", UUID.class);
        String email = row.get("email", String.class);
        String password = row.get("password", String.class);
        boolean isAccountNonExpired = Boolean.TRUE.equals(row.get("isaccountnonexpired", Boolean.class));
        boolean isAccountNonLocked = Boolean.TRUE.equals(row.get("isaccountnonlocked", Boolean.class));
        boolean isCredentialsNonExpired = Boolean.TRUE.equals(row.get("iscredentialsnonexpired", Boolean.class));
        boolean isEnabled = Boolean.TRUE.equals(row.get("isenabled", Boolean.class));
        String group = row.get("group_name", String.class);
        String role = row.get("role_name", String.class);
        // Create a Users object
        Users user = new Users(userId, loginName, email, password, isAccountNonExpired, isAccountNonLocked,
                isCredentialsNonExpired, isEnabled);
        // Return an array containing user, group, and role
        return new Object[]{user, group, role};
    }

}
