package com.minhpt.hrmtoolnextgen.controller;

import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.ADMIN;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.PROJECT_MANAGER;
import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.USER;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.project.ProjectDto;
import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;
import com.minhpt.hrmtoolnextgen.service.project.ProjectService;
import com.minhpt.hrmtoolnextgen.support.AbstractIntegrationTest;

/**
 * Integration tests for project endpoints in ManagerController and AdminController.
 *
 * Strategy: @MockBean ProjectService — no DB, no Redis.
 *
 * Endpoints under test:
 *   POST   /api/v1/manager/project          → 201  (R13.1)
 *   PUT    /api/v1/manager/project/{id}     → 200  (R13.2)
 *   DELETE /api/v1/manager/project/{id}     → 200  (R13.3)
 *   GET    /api/v1/manager/project          → 200  (R13.4)
 *   GET    /api/v1/admin/projects           → 200/403 (R13.5)
 *   Manager-gating: USER on manager endpoint → 403  (R4.5)
 *
 * Principal injection note:
 *   createProject and getProjectsByManagerId use @AuthenticationPrincipal UserPrincipalDto.
 *   @WithMockUser injects a String-based principal which causes a ClassCastException → 500
 *   at runtime.  For these handlers we use
 *   SecurityMockMvcRequestPostProcessors.user(UserPrincipalDto) so that Spring Security
 *   stores a real UserPrincipalDto as the authenticated principal.
 *
 * Validator-bug check (CreateProjectRequest / UpdateProjectRequest):
 *   @NotEmpty on String fields (projectName) — valid constraint, no HV000030 issue.
 *   @NotNull on Long/Enum fields — valid.
 *   No @Disabled tests needed here (unlike ApprovalTimesheetRequest which had @NotEmpty on Enum).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ManagerProjectIntegrationTest {

    @SuppressWarnings("unused")
    @TestConfiguration
    static class TestConfig extends AbstractIntegrationTest {
    }

    @MockBean
    private ProjectService projectService;

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * UserPrincipalDto that Spring Security will expose to @AuthenticationPrincipal.
     * Used for handlers that inject UserPrincipalDto (createProject, getProjectsByManagerId).
     */
    private UserPrincipalDto projectManagerPrincipal() {
        return UserPrincipalDto.internalBuilder()
                .id(99L)
                .email("manager@example.com")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(PROJECT_MANAGER)))
                .build();
    }

    private ProjectDto sampleProjectDto() {
        ProjectDto dto = new ProjectDto();
        dto.setId(1L);
        dto.setName("Test Project");
        dto.setProjectStatus(EProjectStatus.RUNNING);
        dto.setIsDelete(false);
        return dto;
    }

    private PaginationResponse<ProjectDto> singleProjectPage() {
        return PaginationResponse.<ProjectDto>builder()
                .currentPage(0)
                .totalPages(1)
                .pageSize(10)
                .totalElements(1)
                .numberOfElements(1)
                .sortBy("createdDate")
                .direction("DESC")
                .first(true)
                .last(true)
                .items(List.of(sampleProjectDto()))
                .build();
    }

    // =========================================================================
    // R13.1 — POST /api/v1/manager/project as PROJECT_MANAGER → 201
    //
    // createProject uses @AuthenticationPrincipal UserPrincipalDto, so we inject
    // a real UserPrincipalDto via user(projectManagerPrincipal()).
    // =========================================================================

    @Test
    void createProject_asProjectManager_returns201() throws Exception {
        when(projectService.createProject(any())).thenReturn(sampleProjectDto());

        String body = """
                {
                  "projectName": "Test Project",
                  "projectManager": 99,
                  "projectStatus": "RUNNING"
                }
                """;

        mockMvc.perform(post("/api/v1/manager/project")
                        .with(user(projectManagerPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name", is("Test Project")));
    }

    @Test
    void createProject_asProjectManager_legacyPath_returns201() throws Exception {
        when(projectService.createProject(any())).thenReturn(sampleProjectDto());

        String body = """
                {
                  "projectName": "Test Project",
                  "projectManager": 99,
                  "projectStatus": "RUNNING"
                }
                """;

        mockMvc.perform(post("/api/manager/project")
                        .with(user(projectManagerPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    // =========================================================================
    // R13.2 — PUT /api/v1/manager/project/{id} as PROJECT_MANAGER → 200
    //
    // updateProject uses @Valid on the request body; @NotEmpty on String is valid.
    // =========================================================================

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void updateProject_asProjectManager_returns200() throws Exception {
        when(projectService.updateProject(eq(1L), any())).thenReturn(sampleProjectDto());

        String body = """
                {
                  "projectName": "Updated Project",
                  "projectStatus": "RUNNING"
                }
                """;

        mockMvc.perform(put("/api/v1/manager/project/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Test Project")));
    }

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void updateProject_asProjectManager_legacyPath_returns200() throws Exception {
        when(projectService.updateProject(eq(1L), any())).thenReturn(sampleProjectDto());

        String body = """
                {
                  "projectName": "Updated Project",
                  "projectStatus": "RUNNING"
                }
                """;

        mockMvc.perform(put("/api/manager/project/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // R13.3 — DELETE /api/v1/manager/project/{id} as PROJECT_MANAGER → 200
    //
    // deleteProject returns ResponseEntity.ok(buildSuccessResponse(null, ...)) → HTTP 200.
    // =========================================================================

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void deleteProject_asProjectManager_returns200() throws Exception {
        doNothing().when(projectService).deleteProject(1L);

        mockMvc.perform(delete("/api/v1/manager/project/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void deleteProject_asProjectManager_legacyPath_returns200() throws Exception {
        doNothing().when(projectService).deleteProject(1L);

        mockMvc.perform(delete("/api/manager/project/1"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // R13.4 — GET /api/v1/manager/project paginated as PROJECT_MANAGER → 200
    //
    // getProjectsByManagerId uses @AuthenticationPrincipal UserPrincipalDto.
    // =========================================================================

    @Test
    void getProjectsByManager_asProjectManager_returns200WithEnvelope() throws Exception {
        when(projectService.getProjectsByManagerIdWithFilters(
                eq(99L), any(PaginationRequest.class), isNull(), isNull()))
                .thenReturn(singleProjectPage());

        mockMvc.perform(get("/api/v1/manager/project")
                        .with(user(projectManagerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.items[0].name", is("Test Project")));
    }

    @Test
    void getProjectsByManager_withFilters_returns200() throws Exception {
        when(projectService.getProjectsByManagerIdWithFilters(
                eq(99L), any(PaginationRequest.class), eq("Test"), eq(EProjectStatus.RUNNING)))
                .thenReturn(singleProjectPage());

        mockMvc.perform(get("/api/v1/manager/project")
                        .with(user(projectManagerPrincipal()))
                        .param("projectName", "Test")
                        .param("projectStatus", "RUNNING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)));
    }

    // =========================================================================
    // R13.5 — GET /api/v1/admin/projects as ADMIN → 200; as USER → 403
    //
    // AdminController.getAllProjects is ADMIN-gated (ADMIN_ENDPOINTS security rule).
    // =========================================================================

    @Test
    @WithMockUser(authorities = ADMIN)
    void getAllProjectsAdmin_asAdmin_returns200WithEnvelope() throws Exception {
        when(projectService.getAllProjectsForAdmin(any(PaginationRequest.class), isNull(), isNull()))
                .thenReturn(singleProjectPage());

        mockMvc.perform(get("/api/v1/admin/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.items[0].name", is("Test Project")));
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void getAllProjectsAdmin_withFilters_returns200() throws Exception {
        PaginationResponse<ProjectDto> filtered = PaginationResponse.<ProjectDto>builder()
                .currentPage(0).totalPages(0).pageSize(10).totalElements(0)
                .numberOfElements(0).sortBy("createdDate").direction("DESC")
                .first(true).last(true).items(List.of()).build();

        when(projectService.getAllProjectsForAdmin(
                any(PaginationRequest.class), eq("Alpha"), eq(EProjectStatus.DONE)))
                .thenReturn(filtered);

        mockMvc.perform(get("/api/v1/admin/projects")
                        .param("projectName", "Alpha")
                        .param("projectStatus", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(0)));
    }

    @Test
    @WithMockUser(authorities = USER)
    void getAllProjectsAdmin_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/projects"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = PROJECT_MANAGER)
    void getAllProjectsAdmin_asProjectManager_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/projects"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // R4.5 — representative manager-gating: USER → 403 on manager project endpoint
    //
    // Full role matrix is in AuthorizationMatrixTest; one representative assertion here.
    // =========================================================================

    @Test
    @WithMockUser(authorities = USER)
    void deleteProject_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/manager/project/1"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Unauthenticated → 401
    // =========================================================================

    @Test
    void createProject_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/manager/project")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectName\":\"X\",\"projectManager\":1,\"projectStatus\":\"RUNNING\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllProjectsAdmin_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/projects"))
                .andExpect(status().isUnauthorized());
    }
}
