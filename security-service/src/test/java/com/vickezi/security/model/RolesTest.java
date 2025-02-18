package com.vickezi.security.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RolesTest {
    @Test
    void equalsReturnsTrueForSameRole() {
        Roles role1 = new Roles("ADMIN");
        Roles role2 = new Roles("ADMIN");

        assertThat(role1.equals(role2)).isTrue();
    }

    @Test
    void equalsReturnsFalseForDifferentRoles() {
        Roles role1 = new Roles("ADMIN");
        Roles role2 = new Roles("USER");

        assertThat(role1.equals(role2)).isFalse();
    }

    @Test
    void equalsReturnsFalseForNull() {
        Roles role = new Roles("ADMIN");

        assertThat(role.equals(null)).isFalse();
    }

    @Test
    void equalsReturnsTrueForSameObject() {
        Roles role = new Roles("ADMIN");

        assertThat(role.equals(role)).isTrue();
    }

    @Test
    void sameRoleNameProducesSameHashCode() {
        Roles role1 = new Roles("ADMIN");
        Roles role2 = new Roles("ADMIN");

        assertThat(role1.hashCode()).isEqualTo(role2.hashCode());
    }

    @Test
    void getterReturnsRoleName() {
        String roleName = "ADMIN";
        Roles role = new Roles(roleName);

        assertThat(role.getRole()).isEqualTo(roleName);
    }

    @Test
    void setterUpdatesRoleName() {
        Roles role = new Roles("ADMIN");
        String newRoleName = "USER";

        role.setAuthority(newRoleName);

        assertThat(role.getRole()).isEqualTo(newRoleName);
    }
}