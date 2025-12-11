package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.project.ProjectDto;
import com.vatek.hrmtoolnextgen.dto.request.CreateProjectRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateProjectRequest;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.vatek.hrmtoolnextgen.service.ProjectService;
import com.vatek.hrmtoolnextgen.util.CommonUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@AllArgsConstructor
@Log4j2
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "CRUD APIs for managing projects and team assignments")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @Operation(
            summary = "List projects",
            description = "Returns a paginated list of projects including managers, members, and lifecycle status."
    )
    public ResponseEntity<CommonSuccessResponse<Page<ProjectDto>>> getAllProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Pageable pageable = CommonUtils.buildPageable(page, size);
        Page<ProjectDto> projects = projectService.getAllProjects(pageable);
        return ResponseEntity.ok(buildSuccessResponse(projects, request));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get project detail",
            description = "Fetches a project by ID with manager, members, and timeline metadata."
    )
    public ResponseEntity<CommonSuccessResponse<ProjectDto>> getProjectById(
            @PathVariable Long id,
            HttpServletRequest request) {
        ProjectDto project = projectService.getProjectById(id);
        return ResponseEntity.ok(buildSuccessResponse(project, request));
    }

    @PostMapping
    @Operation(
            summary = "Create project",
            description = "Creates a new project, assigns a manager, and optionally adds team members"
    )
    public ResponseEntity<CommonSuccessResponse<ProjectDto>> createProject(
            @Valid @RequestBody CreateProjectRequest createProjectRequest,
            HttpServletRequest request) {
        ProjectDto project = projectService.createProject(createProjectRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildSuccessResponse(project, request));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update project",
            description = "Updates project metadata, status, manager, and members."
    )
    public ResponseEntity<CommonSuccessResponse<ProjectDto>> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest updateProjectRequest,
            HttpServletRequest request) {
        ProjectDto project = projectService.updateProject(id, updateProjectRequest);
        return ResponseEntity.ok(buildSuccessResponse(project, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete project",
            description = "Soft-deletes a project by setting its `isDelete` flag."
    )
    public ResponseEntity<CommonSuccessResponse<Void>> deleteProject(
            @PathVariable Long id,
            HttpServletRequest request) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(buildSuccessResponse(null, request));
    }

    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .data(data)
                .build();
    }
}

