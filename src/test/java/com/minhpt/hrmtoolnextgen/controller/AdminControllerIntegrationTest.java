package com.minhpt.hrmtoolnextgen.controller;

import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.ADMIN;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.dto.user.UserDto;
import com.minhpt.hrmtoolnextgen.dto.user.UserInfoDto;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.service.project.ProjectService;
import com.minhpt.hrmtoolnextgen.service.user.UserService;

/**
 * Integration tests for AdminController.
 *
 * Strategy: @MockBean UserService (and ProjectService) so no H2 writes, no S3, no Redis.
 * This keeps every scenario deterministic and independent of infrastructure.
 *
 * Security: all admin endpoints require hasAnyAuthority(ADMIN, IT_ADMIN).
 * Wrong role → 403 at the URL filter; correct role → handler reached.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerIntegrationTest {

    @SuppressWarnings("unused")
    @TestConfiguration
    static class MailTestConfig {
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }

    @MockBean
    private UserService userService;

    @MockBean
    private ProjectService projectService;

    @Autowired
    private MockMvc mockMvc;

    private UserDto sampleUserDto;

    @BeforeEach
    void setUp() {
        sampleUserDto = new UserDto();
        sampleUserDto.setId(1L);
        sampleUserDto.setEmail("admin-test@example.com");
        sampleUserDto.setEnabled(true);
        sampleUserDto.setRoles(List.of(EUserRole.USER));

        UserInfoDto info = UserInfoDto.builder()
                .firstName("Test")
                .lastName("User")
                .build();
        sampleUserDto.setUserInfo(info);
    }

    // -------------------------------------------------------------------------
    // R7.1 POST /api/v1/admin/user — multipart create → 201
    // @MockBean UserService returns sampleUserDto; controller wraps in CommonSuccessResponse.
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = ADMIN)
    void createUser_asAdmin_multipartRequest_returns201() throws Exception {
        when(userService.createUser(any())).thenReturn(sampleUserDto);

        mockMvc.perform(multipart("/api/v1/admin/user")
                        .param("email", "newuser@example.com")
                        .param("roles", "USER")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("admin-test@example.com"))
                .andExpect(jsonPath("$.message").value("Successfully"));

        verify(userService).createUser(any());
    }

    // -------------------------------------------------------------------------
    // R7.2 GET /api/v1/admin/user/{id} — existing id → 200
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = ADMIN)
    void getUserById_existingId_returns200() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sampleUserDto);

        mockMvc.perform(get("/api/v1/admin/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("admin-test@example.com"));
    }

    // -------------------------------------------------------------------------
    // R7.2 GET /api/v1/admin/user/{id} — absent id → 404
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = ADMIN)
    void getUserById_absentId_returns404() throws Exception {
        when(userService.getUserById(999L)).thenThrow(new NotFoundException("user not found"));

        mockMvc.perform(get("/api/v1/admin/user/999"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // R7.6 GET /api/v1/admin/users — paginated list → 200 with pagination envelope
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = ADMIN)
    void getAllUsers_returns200WithPaginationEnvelope() throws Exception {
        PaginationResponse<UserDto> page = PaginationResponse.<UserDto>builder()
                .currentPage(0)
                .totalPages(1)
                .pageSize(10)
                .totalElements(1)
                .numberOfElements(1)
                .sortBy("id")
                .direction("ASC")
                .first(true)
                .last(true)
                .items(List.of(sampleUserDto))
                .build();

        when(userService.getAllUsersForAdmin(any(PaginationRequest.class), isNull(), isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].email").value("admin-test@example.com"));
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void getAllUsers_withNameFilter_returns200() throws Exception {
        PaginationResponse<UserDto> page = PaginationResponse.<UserDto>builder()
                .currentPage(0)
                .totalPages(0)
                .pageSize(10)
                .totalElements(0)
                .numberOfElements(0)
                .sortBy("id")
                .direction("ASC")
                .first(true)
                .last(true)
                .items(List.of())
                .build();

        when(userService.getAllUsersForAdmin(any(PaginationRequest.class), anyString(), isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users").param("name", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // -------------------------------------------------------------------------
    // R4.4 Authorization gating — admin endpoints (positive smoke-check only)
    // Full negative role matrix (USER → 403, PROJECT_MANAGER → 403, etc.) is
    // the responsibility of AuthorizationMatrixTest, which is the source of
    // truth for role-based access control across all admin endpoints.
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = ADMIN)
    void adminUserEndpoint_withAdminAuthority_reachesHandler() throws Exception {
        when(userService.getAllUsersForAdmin(any(), isNull(), isNull()))
                .thenReturn(PaginationResponse.<UserDto>builder()
                        .currentPage(0).totalPages(0).pageSize(10).totalElements(0)
                        .numberOfElements(0).sortBy("id").direction("ASC")
                        .first(true).last(true).items(List.of()).build());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().is(allOf(not(401), not(403))));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/admin/user/{id} — soft-delete gate (R4.4, R7.4)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = ADMIN)
    void deleteUser_asAdmin_existingId_returns200() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/v1/admin/user/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void deleteUser_asAdmin_absentId_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new NotFoundException("not found"))
                .when(userService).deleteUser(999L);

        mockMvc.perform(delete("/api/v1/admin/user/999"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/admin/user/{id}/password — password reset (R7.5)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = ADMIN)
    void setUserPassword_asAdmin_validBody_returns200() throws Exception {
        doNothing().when(userService).setUserPassword(1L, "newPassword123");

        mockMvc.perform(put("/api/v1/admin/user/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"newPassword123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void setUserPassword_asAdmin_tooShortPassword_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/admin/user/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }
}
