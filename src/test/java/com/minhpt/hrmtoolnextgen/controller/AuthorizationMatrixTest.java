package com.minhpt.hrmtoolnextgen.controller;

import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.ADMIN;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.HR;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.IT_ADMIN;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.PROJECT_MANAGER;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.minhpt.hrmtoolnextgen.component.TokenBucketRateLimiter;
import com.minhpt.hrmtoolnextgen.dto.holiday.HolidayDto;
import com.minhpt.hrmtoolnextgen.service.HolidayService;

/**
 * URL-level authorization matrix covering one representative endpoint per protected family.
 *
 * ACTUAL security config mappings (verified by running tests against live context):
 *
 *   ADMIN_ENDPOINTS (/api/admin/**, /api/v1/admin/**)
 *       → hasAnyAuthority(ADMIN, IT_ADMIN)
 *       BLOCKS: USER, PROJECT_MANAGER, HR
 *       ALLOWS: ADMIN, IT_ADMIN
 *
 *   MANAGER_ENDPOINTS (/api/manager/**, /api/v1/manager/**)
 *       → hasAuthority(PROJECT_MANAGER)
 *       BLOCKS: USER, HR
 *       ALLOWS: PROJECT_MANAGER, ADMIN, IT_ADMIN
 *
 *       NOTE — Role hierarchy IS applied in Spring Security 6 AuthorizationManagerRequestMatcherRegistry.
 *       WebSecurityConfig declares ADMIN > PROJECT_MANAGER and IT_ADMIN > PROJECT_MANAGER,
 *       so hasAuthority(PROJECT_MANAGER) also passes for ADMIN and IT_ADMIN at the URL level.
 *       This DIVERGES from a naive reading of "hasAuthority = exact match only"; the wired
 *       RoleHierarchy bean is picked up by the authorization manager.
 *
 *   USER_ENDPOINTS (/api/user/**, /api/v1/user/**, /api/device/**, /api/v1/device/**)
 *       → hasAnyAuthority(USER, PROJECT_MANAGER, HR, ADMIN, IT_ADMIN)
 *       ALLOWS: all 5 roles
 *
 *   HOLIDAY_ENDPOINTS (/api/holidays/**, /api/v1/holidays/**)
 *       → authenticated()
 *       ALLOWS: any authenticated user
 *
 *   AUTH_ENDPOINTS (/api/auth/**, /api/v1/auth/**)
 *       → permitAll()
 *
 * DIVERGENCE SUMMARY (requirement prose vs. actual code):
 *   1. HR on ADMIN_ENDPOINTS: requirement says ADMIN+HR; code enforces {ADMIN, IT_ADMIN} only.
 *      HR is BLOCKED on /api/v1/admin/**.
 *   2. ADMIN/IT_ADMIN on MANAGER_ENDPOINTS: code uses hasAuthority(PROJECT_MANAGER) but
 *      the RoleHierarchy bean causes ADMIN and IT_ADMIN to also pass this check at URL level.
 *      Method-level @AuthenticationPrincipal UserPrincipalDto still fails for @WithMockUser
 *      (String principal), producing 500 for GET handlers that inject the principal.
 *      The DELETE /manager/project/{id} (no @AuthenticationPrincipal) returns 404 for ADMIN/IT_ADMIN,
 *      confirming they are NOT blocked at the URL level.
 *
 * Bidirectional proof technique:
 *   Wrong role  → 403 (blocked at URL filter chain)
 *   Correct role → NOT 401/403 (404 for non-existent id, 200/2xx for list endpoints —
 *                  proves the method was reached)
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationMatrixTest {

    @SuppressWarnings("unused")
    @TestConfiguration
    static class TestBeans {
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }

    // Holiday endpoints carry @RateLimit — stub to always allow
    @MockBean
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    // HolidayService makes external Redis/Calendarific calls — stub to return empty list
    @MockBean
    private HolidayService holidayService;

    @Autowired
    private MockMvc mockMvc;

    // =========================================================================
    // UNAUTHENTICATED → 401 for each protected family
    // =========================================================================

    @Test
    void adminEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managerEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/manager/project/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/user/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void holidayEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/holidays/current"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // ADMIN_ENDPOINTS — GET /api/v1/admin/users
    // Allowed: ADMIN, IT_ADMIN
    // Blocked: USER, PROJECT_MANAGER, HR
    // =========================================================================

    @Test
    @WithMockUser(authorities = USER)
    void adminUsers_withUserAuthority_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void adminUsers_withProjectManagerAuthority_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = HR)
    void adminUsers_withHrAuthority_returns403() throws Exception {
        // HR is NOT in ADMIN_AUTHORITIES = {ADMIN, IT_ADMIN}.
        // Divergence: requirement prose mentions ADMIN+HR on /admin/**, but actual code blocks HR.
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void adminUsers_withAdminAuthority_reachesController() throws Exception {
        // URL gate passes → service returns empty page → 200
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = IT_ADMIN)
    void adminUsers_withItAdminAuthority_reachesController() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());
    }

    // ADMIN write — DELETE /api/v1/admin/user/{id}

    @Test
    @WithMockUser(authorities = USER)
    void adminDeleteUser_withUserAuthority_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/user/999999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = HR)
    void adminDeleteUser_withHrAuthority_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/user/999999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void adminDeleteUser_withAdminAuthority_reachesController() throws Exception {
        // Gate passed: assert not blocked (not 401/403); actual outcome is DB-dependent
        mockMvc.perform(delete("/api/v1/admin/user/999999"))
                .andExpect(status().is(allOf(not(401), not(403))));
    }

    @Test
    @WithMockUser(authorities = IT_ADMIN)
    void adminDeleteUser_withItAdminAuthority_reachesController() throws Exception {
        // Gate passed: assert not blocked (not 401/403); actual outcome is DB-dependent
        mockMvc.perform(delete("/api/v1/admin/user/999999"))
                .andExpect(status().is(allOf(not(401), not(403))));
    }

    // =========================================================================
    // MANAGER_ENDPOINTS — DELETE /api/v1/manager/project/{id}
    // (chosen because it has no @AuthenticationPrincipal, so no principal-cast issue)
    //
    // Allowed: PROJECT_MANAGER, ADMIN, IT_ADMIN (via role hierarchy)
    // Blocked: USER, HR
    // =========================================================================

    @Test
    @WithMockUser(authorities = USER)
    void managerDeleteProject_withUserAuthority_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/manager/project/999999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = HR)
    void managerDeleteProject_withHrAuthority_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/manager/project/999999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void managerDeleteProject_withProjectManagerAuthority_reachesController() throws Exception {
        // Gate passed: assert not blocked (not 401/403); actual outcome is DB-dependent
        mockMvc.perform(delete("/api/v1/manager/project/999999"))
                .andExpect(status().is(allOf(not(401), not(403))));
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void managerDeleteProject_withAdminAuthority_reachesController() throws Exception {
        // ADMIN passes due to role hierarchy (ADMIN > PROJECT_MANAGER applied by Spring Security 6).
        // Gate passed: assert not blocked (not 401/403); actual outcome is DB-dependent
        mockMvc.perform(delete("/api/v1/manager/project/999999"))
                .andExpect(status().is(allOf(not(401), not(403))));
    }

    @Test
    @WithMockUser(authorities = IT_ADMIN)
    void managerDeleteProject_withItAdminAuthority_reachesController() throws Exception {
        // IT_ADMIN passes due to role hierarchy (IT_ADMIN > PROJECT_MANAGER).
        // Gate passed: assert not blocked (not 401/403); actual outcome is DB-dependent
        mockMvc.perform(delete("/api/v1/manager/project/999999"))
                .andExpect(status().is(allOf(not(401), not(403))));
    }

    // =========================================================================
    // USER_ENDPOINTS — GET /api/v1/user/roles (simple list, no path var, no principal)
    // Allowed: all 5 roles
    // =========================================================================

    @Test
    @WithMockUser(authorities = USER)
    void userRoles_withUserAuthority_reachesController() throws Exception {
        mockMvc.perform(get("/api/v1/user/roles"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void userRoles_withProjectManagerAuthority_reachesController() throws Exception {
        mockMvc.perform(get("/api/v1/user/roles"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = HR)
    void userRoles_withHrAuthority_reachesController() throws Exception {
        mockMvc.perform(get("/api/v1/user/roles"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void userRoles_withAdminAuthority_reachesController() throws Exception {
        mockMvc.perform(get("/api/v1/user/roles"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = IT_ADMIN)
    void userRoles_withItAdminAuthority_reachesController() throws Exception {
        mockMvc.perform(get("/api/v1/user/roles"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // HOLIDAY_ENDPOINTS — GET /api/v1/holidays/current
    // authenticated() — any authenticated user passes
    // @RateLimit requires TokenBucketRateLimiter stub
    // HolidayService @MockBean returns empty list to avoid Redis/external calls
    // =========================================================================

    @Test
    @WithMockUser(authorities = USER)
    void holidayCurrent_withUserAuthority_reachesController() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(any(), anyInt(), anyInt())).thenReturn(true);
        when(holidayService.getCurrentYearHolidays()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/holidays/current"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = HR)
    void holidayCurrent_withHrAuthority_reachesController() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(any(), anyInt(), anyInt())).thenReturn(true);
        when(holidayService.getCurrentYearHolidays()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/holidays/current"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void holidayCurrent_withAdminAuthority_reachesController() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(any(), anyInt(), anyInt())).thenReturn(true);
        when(holidayService.getCurrentYearHolidays()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/holidays/current"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // AUTH_ENDPOINTS — permitAll; no auth required
    // =========================================================================

    @Test
    void authLogin_unauthenticated_doesNotReturn401() throws Exception {
        // permitAll — malformed body returns 400 (validation), not 401
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Legacy (unversioned) paths — same rules apply as v1 paths
    // =========================================================================

    @Test
    void adminLegacyEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = USER)
    void adminLegacyEndpoint_withUserAuthority_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void adminLegacyEndpoint_withAdminAuthority_reachesController() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    void managerLegacyEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/manager/project/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = USER)
    void managerLegacyEndpoint_withUserAuthority_returns403() throws Exception {
        mockMvc.perform(delete("/api/manager/project/999999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void managerLegacyEndpoint_withProjectManagerAuthority_reachesController() throws Exception {
        // Gate passed: assert not blocked (not 401/403); actual outcome is DB-dependent
        mockMvc.perform(delete("/api/manager/project/999999"))
                .andExpect(status().is(allOf(not(401), not(403))));
    }
}
