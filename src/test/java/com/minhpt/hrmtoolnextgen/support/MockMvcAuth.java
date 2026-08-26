package com.minhpt.hrmtoolnextgen.support;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.minhpt.hrmtoolnextgen.constant.RoleConstant;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * MockMvc request post-processors for authenticating as a given role in controller tests.
 *
 * <p>Authorities use raw role strings (no "ROLE_" prefix) matching
 * {@link RoleConstant} and {@code UserDetailsServiceImpl}.
 *
 * <p>Usage:
 * <pre>
 *   mockMvc.perform(get("/api/v1/device/1").with(MockMvcAuth.asAdmin()))
 * </pre>
 */
public final class MockMvcAuth {

    private MockMvcAuth() {}

    public static RequestPostProcessor asAdmin() {
        return as(RoleConstant.ADMIN);
    }

    public static RequestPostProcessor asItAdmin() {
        return as(RoleConstant.IT_ADMIN);
    }

    public static RequestPostProcessor asProjectManager() {
        return as(RoleConstant.PROJECT_MANAGER);
    }

    public static RequestPostProcessor asUser() {
        return as(RoleConstant.USER);
    }

    public static RequestPostProcessor asHr() {
        return as(RoleConstant.HR);
    }

    /**
     * Generic factory — pass one or more raw authority strings.
     *
     * @param authorities authority strings without "ROLE_" prefix
     */
    public static RequestPostProcessor as(String... authorities) {
        var grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        return SecurityMockMvcRequestPostProcessors
                .user("test-user")
                .authorities(grantedAuthorities);
    }
}
