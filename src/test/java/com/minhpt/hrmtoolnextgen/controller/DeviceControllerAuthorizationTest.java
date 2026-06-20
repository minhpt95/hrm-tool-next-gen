package com.minhpt.hrmtoolnextgen.controller;

import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.ADMIN;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.IT_ADMIN;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.USER;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.PROJECT_MANAGER;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.HR;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that the @PreAuthorize("hasAnyAuthority(ADMIN_AUTHORITIES)") guard on
 * DeviceController#manageDeviceUsers (POST /api/v1/device/{id}/users) and
 * DeviceController#getDeviceUsers  (GET  /api/v1/device/{id}/users) is enforced.
 *
 * Authorities are stored without the "ROLE_" prefix (see UserDetailsServiceImpl),
 * so @WithMockUser(authorities = "ADMIN") correctly matches hasAnyAuthority("ADMIN").
 *
 * The device endpoints sit under USER_ENDPOINTS in the filter chain, meaning any
 * authenticated user passes the URL-level check. The method-level @PreAuthorize
 * is the ONLY gate preventing non-admin access — this test validates that gate.
 *
 * When @PreAuthorize blocks a request, Spring throws AccessDeniedException.
 * CommonControllerAdvice now handles AccessDeniedException explicitly via
 * @ExceptionHandler(AccessDeniedException.class) @ResponseStatus(HttpStatus.FORBIDDEN),
 * so denials return 403. We therefore assert:
 *   - Non-admin users  → 403  (AccessDeniedException handled by CommonControllerAdvice: gate fired)
 *   - ADMIN / IT_ADMIN → 404  (@PreAuthorize passed, service threw NotFoundException
 *                               for a non-existent device ID: method was invoked)
 *
 * This pair of assertions provides a deterministic, bidirectional proof that the
 * authorization boundary is in the right place.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DeviceControllerAuthorizationTest {

    @SuppressWarnings("unused")
    @TestConfiguration
    static class MailTestConfig {
        @SuppressWarnings("unused")
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // GET /api/v1/device/{id}/users
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = USER)
    void getDeviceUsers_withUserAuthority_shouldBeBlockedByPreAuthorize() throws Exception {
        // AccessDeniedException from @PreAuthorize → handled by CommonControllerAdvice → 403
        mockMvc.perform(get("/api/v1/device/1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void getDeviceUsers_withProjectManagerAuthority_shouldBeBlockedByPreAuthorize() throws Exception {
        mockMvc.perform(get("/api/v1/device/1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = HR)
    void getDeviceUsers_withHrAuthority_shouldBeBlockedByPreAuthorize() throws Exception {
        mockMvc.perform(get("/api/v1/device/1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void getDeviceUsers_withAdminAuthority_shouldPassPreAuthorizeAndReachService() throws Exception {
        // @PreAuthorize passes for ADMIN. Non-existent device → NotFoundException → 404.
        // 404 proves the method was actually invoked (gate did NOT block).
        mockMvc.perform(get("/api/v1/device/999999/users"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = IT_ADMIN)
    void getDeviceUsers_withItAdminAuthority_shouldPassPreAuthorizeAndReachService() throws Exception {
        mockMvc.perform(get("/api/v1/device/999999/users"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/device/{id}/users
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = USER)
    void manageDeviceUsers_withUserAuthority_shouldBeBlockedByPreAuthorize() throws Exception {
        mockMvc.perform(post("/api/v1/device/1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void manageDeviceUsers_withProjectManagerAuthority_shouldBeBlockedByPreAuthorize() throws Exception {
        mockMvc.perform(post("/api/v1/device/1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = HR)
    void manageDeviceUsers_withHrAuthority_shouldBeBlockedByPreAuthorize() throws Exception {
        mockMvc.perform(post("/api/v1/device/1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void manageDeviceUsers_withAdminAuthority_shouldPassPreAuthorizeAndReachService() throws Exception {
        // Empty userIds list is valid (detach-all semantics). Non-existent device → 404.
        mockMvc.perform(post("/api/v1/device/999999/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = IT_ADMIN)
    void manageDeviceUsers_withItAdminAuthority_shouldPassPreAuthorizeAndReachService() throws Exception {
        mockMvc.perform(post("/api/v1/device/999999/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[]}"))
                .andExpect(status().isNotFound());
    }
}
