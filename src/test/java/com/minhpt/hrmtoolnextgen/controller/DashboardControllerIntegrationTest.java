package com.minhpt.hrmtoolnextgen.controller;

import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.ADMIN;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.HR;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.IT_ADMIN;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.PROJECT_MANAGER;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.USER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.minhpt.hrmtoolnextgen.dto.dashboard.DashboardSummaryDto;
import com.minhpt.hrmtoolnextgen.dto.dashboard.ProjectStatusCountDto;
import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;
import com.minhpt.hrmtoolnextgen.service.DashboardService;

/**
 * Integration tests for DashboardController.
 *
 * Security gate: /api/admin/dashboard/** and /api/v1/admin/dashboard/**
 * fall under ADMIN_ENDPOINTS → hasAnyAuthority(ADMIN, IT_ADMIN).
 *
 * DIVERGENCE NOTE (R20.2): The requirement prose says "not an ADMIN → 403".
 * The actual gate is hasAnyAuthority(ADMIN, IT_ADMIN), so IT_ADMIN also passes.
 * Tests reflect the ACTUAL gate: both ADMIN and IT_ADMIN → 200; USER/HR/PROJECT_MANAGER → 403.
 *
 * @MockBean DashboardService keeps the test deterministic — no DB, no Redis.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerIntegrationTest {

    @SuppressWarnings("unused")
    @TestConfiguration
    static class MailTestConfig {
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }

    @MockBean
    private DashboardService dashboardService;

    @Autowired
    private MockMvc mockMvc;

    private DashboardSummaryDto sampleSummary;

    @BeforeEach
    void setUp() {
        sampleSummary = new DashboardSummaryDto(
                List.of(
                        new ProjectStatusCountDto(EProjectStatus.RUNNING, 5L),
                        new ProjectStatusCountDto(EProjectStatus.INCOMING, 2L),
                        new ProjectStatusCountDto(EProjectStatus.DONE, 8L)
                ),
                42L
        );
    }

    // -------------------------------------------------------------------------
    // R20.1 — versioned path (ADMIN) → 200 + response envelope with data
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = ADMIN)
    void getDashboardSummary_asAdmin_v1Path_returns200WithData() throws Exception {
        when(dashboardService.getDashboardSummary()).thenReturn(sampleSummary);

        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeEmployeeCount").value(42))
                .andExpect(jsonPath("$.data.projectStatusCounts").isArray())
                .andExpect(jsonPath("$.data.projectStatusCounts.length()").value(3));
    }

    // -------------------------------------------------------------------------
    // R20.1 — legacy (unversioned) path (ADMIN) → 200 + same envelope
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = ADMIN)
    void getDashboardSummary_asAdmin_legacyPath_returns200WithData() throws Exception {
        when(dashboardService.getDashboardSummary()).thenReturn(sampleSummary);

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeEmployeeCount").value(42))
                .andExpect(jsonPath("$.data.projectStatusCounts").isArray());
    }

    // -------------------------------------------------------------------------
    // R20.1 — IT_ADMIN also passes the ADMIN gate (actual gate = {ADMIN, IT_ADMIN})
    // DIVERGENCE: requirement says "ADMIN only"; code allows IT_ADMIN too.
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = IT_ADMIN)
    void getDashboardSummary_asItAdmin_returns200() throws Exception {
        when(dashboardService.getDashboardSummary()).thenReturn(sampleSummary);

        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeEmployeeCount").value(42));
    }

    // -------------------------------------------------------------------------
    // R20.2 — wrong roles → 403
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = USER)
    void getDashboardSummary_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void getDashboardSummary_asProjectManager_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = HR)
    void getDashboardSummary_asHr_returns403() throws Exception {
        // HR is NOT in ADMIN_AUTHORITIES = {ADMIN, IT_ADMIN}.
        // Consistent with AuthorizationMatrixTest: HR blocked on all /api/v1/admin/** endpoints.
        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Unauthenticated → 401
    // -------------------------------------------------------------------------

    @Test
    void getDashboardSummary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDashboardSummary_unauthenticated_legacyPath_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }
}
