package com.minhpt.hrmtoolnextgen.controller;

import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.ADMIN;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.IT_ADMIN;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.USER;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.PROJECT_MANAGER;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.HR;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    // -------------------------------------------------------------------------
    // POST /api/v1/device  (createDevice)
    // -------------------------------------------------------------------------

    private static final String VALID_CREATE_DEVICE_JSON =
            "{\"name\":\"Test Laptop\",\"serialNumber\":\"SN-AUTH-TEST-001\"," +
            "\"type\":\"LAPTOP\",\"status\":\"ACTIVE\"}";

    @Test
    @WithMockUser(authorities = USER)
    void createDevice_withUserAuthority_shouldBeBlockedByPreAuthorize() throws Exception {
        // AccessDeniedException from @PreAuthorize → handled by CommonControllerAdvice → 403
        mockMvc.perform(post("/api/v1/device")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_DEVICE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void createDevice_withProjectManagerAuthority_shouldBeBlockedByPreAuthorize() throws Exception {
        mockMvc.perform(post("/api/v1/device")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_DEVICE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void createDevice_withAdminAuthority_shouldPassPreAuthorizeAndReachService() throws Exception {
        // @PreAuthorize passes for ADMIN. A valid body with a unique serial number → 201 Created.
        // 201 proves the method was invoked (gate did NOT block).
        mockMvc.perform(post("/api/v1/device")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_DEVICE_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = IT_ADMIN)
    void createDevice_withItAdminAuthority_shouldPassPreAuthorizeAndReachService() throws Exception {
        // Use a different serial number to avoid unique-constraint collision with the ADMIN test above.
        mockMvc.perform(post("/api/v1/device")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Laptop IT\",\"serialNumber\":\"SN-AUTH-TEST-002\"," +
                                 "\"type\":\"LAPTOP\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/device/{id}  (updateDevice)
    // -------------------------------------------------------------------------

    private static final String VALID_UPDATE_DEVICE_JSON =
            "{\"name\":\"Updated Laptop\",\"serialNumber\":\"SN-UPDATED-001\"," +
            "\"type\":\"DESKTOP\",\"status\":\"INACTIVE\"}";

    @Test
    @WithMockUser(authorities = USER)
    void updateDevice_withUserAuthority_shouldBeBlockedByPreAuthorize() throws Exception {
        // AccessDeniedException from @PreAuthorize → 403
        mockMvc.perform(put("/api/v1/device/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_DEVICE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = HR)
    void updateDevice_withHrAuthority_shouldBeBlockedByPreAuthorize() throws Exception {
        mockMvc.perform(put("/api/v1/device/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_DEVICE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void updateDevice_withAdminAuthority_shouldPassPreAuthorizeAndReachService() throws Exception {
        // @PreAuthorize passes for ADMIN. Non-existent device → NotFoundException → 404.
        // 404 proves the method was actually invoked (gate did NOT block).
        mockMvc.perform(put("/api/v1/device/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_DEVICE_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = IT_ADMIN)
    void updateDevice_withItAdminAuthority_shouldPassPreAuthorizeAndReachService() throws Exception {
        mockMvc.perform(put("/api/v1/device/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_DEVICE_JSON))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/device/{id}  (deleteDevice)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = USER)
    void deleteDevice_withUserAuthority_shouldBeBlockedByPreAuthorize() throws Exception {
        // AccessDeniedException from @PreAuthorize → 403
        mockMvc.perform(delete("/api/v1/device/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void deleteDevice_withProjectManagerAuthority_shouldBeBlockedByPreAuthorize() throws Exception {
        mockMvc.perform(delete("/api/v1/device/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void deleteDevice_withAdminAuthority_shouldPassPreAuthorizeAndReachService() throws Exception {
        // @PreAuthorize passes for ADMIN. Non-existent device → NotFoundException → 404.
        // 404 proves the method was actually invoked (gate did NOT block).
        mockMvc.perform(delete("/api/v1/device/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = IT_ADMIN)
    void deleteDevice_withItAdminAuthority_shouldPassPreAuthorizeAndReachService() throws Exception {
        mockMvc.perform(delete("/api/v1/device/999999"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/device/{id}  (getDeviceById) — sanity: READ stays open
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = USER)
    void getDeviceById_withUserAuthority_shouldNotBeBlockedByPreAuthorize() throws Exception {
        // No @PreAuthorize on getDeviceById. Non-existent device → NotFoundException → 404.
        // The key assertion: NOT 403 — the read endpoint has no admin gate.
        mockMvc.perform(get("/api/v1/device/999999"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/device  (getAllDevices) — sanity: list READ stays open
    // Gap: this endpoint was not covered by any prior assertion.
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = USER)
    void getAllDevices_withUserAuthority_shouldNotBeBlockedByPreAuthorize() throws Exception {
        // No @PreAuthorize on getAllDevices. Any authenticated user may list devices.
        // Empty result set → 200 OK (NOT 403).
        mockMvc.perform(get("/api/v1/device"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = HR)
    void getAllDevices_withHrAuthority_shouldNotBeBlockedByPreAuthorize() throws Exception {
        mockMvc.perform(get("/api/v1/device"))
                .andExpect(status().isOk());
    }
}
