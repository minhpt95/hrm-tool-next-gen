package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.dto.project.ProjectDto;
import com.vatek.hrmtoolnextgen.dto.request.*;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.vatek.hrmtoolnextgen.dto.response.PaginationResponse;
import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.vatek.hrmtoolnextgen.service.DayOffService;
import com.vatek.hrmtoolnextgen.service.ProjectService;
import com.vatek.hrmtoolnextgen.service.TimesheetService;
import com.vatek.hrmtoolnextgen.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@AllArgsConstructor
@Log4j2
@RequestMapping("/${hrm.api.prefix}/manager")
@Tag(name = "Manager", description = "CRUD APIs for Manager")
public class ManagerController {

    private final UserService userService;
    private final ProjectService projectService;
    private final TimesheetService timesheetService;
    private final DayOffService dayOffService;

    @GetMapping("/user/{id}")
    @Operation(
            summary = "Get user detail",
            description = "Fetches a single employee by ID, including profile and assigned roles."
    )
    public ResponseEntity<CommonSuccessResponse<UserDto>> getUserById(
            @PathVariable Long id,
            HttpServletRequest request) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(buildSuccessResponse(user, request));
    }

    @PostMapping("/project")
    @Operation(
            summary = "Create project",
            description = "Creates a new project. The current manager will be set as the project manager if not specified in the request."
    )
    public ResponseEntity<CommonSuccessResponse<ProjectDto>> createProject(
            @AuthenticationPrincipal UserPrincipalDto userPrincipalDto,
            @RequestBody CreateProjectRequest createProjectRequest,
            HttpServletRequest request) {
        // Set the current manager as the project manager if not specified
        if (createProjectRequest.getProjectManager() == null) {
            createProjectRequest.setProjectManager(userPrincipalDto.getId());
        }
        ProjectDto project = projectService.createProject(createProjectRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildSuccessResponse(project, request));
    }

    @PutMapping("/project/{id}")
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

    @DeleteMapping("/project/{id}")
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

    @PutMapping("/timesheet")
    public ResponseEntity<CommonSuccessResponse<TimesheetDto>> updateTimesheet(
            @RequestBody UpdateTimesheetRequest updateTimesheetReq,
            HttpServletRequest request
    ) {
        TimesheetDto timesheetDto = timesheetService.updateTimesheet(updateTimesheetReq);

        return ResponseEntity.ok(buildSuccessResponse(timesheetDto, request));
    }

    @PutMapping("/timesheet/approval")
    public ResponseEntity<CommonSuccessResponse<TimesheetDto>> approvalTimesheet(
            ApprovalTimesheetRequest approvalForm,
            HttpServletRequest request
    ) {
        TimesheetDto timesheetDto = timesheetService.approvalTimesheet(approvalForm);

        return ResponseEntity.ok(buildSuccessResponse(timesheetDto, request));
    }

    @GetMapping("/project")
    @Operation(
            summary = "List projects by manager",
            description = "Returns a paginated list of all non-deleted projects managed by the current user. Can filter by project name and project status. Default sort by created date descending."
    )
    public ResponseEntity<CommonSuccessResponse<PaginationResponse<ProjectDto>>> getProjectsByManagerId(
            @AuthenticationPrincipal UserPrincipalDto userPrincipalDto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) EProjectStatus projectStatus,
            HttpServletRequest request) {

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PaginationResponse<ProjectDto> projects = projectService.getProjectsByManagerIdWithFilters(
                userPrincipalDto.getId(),
                paginationRequest,
                projectName,
                projectStatus
        );
        return ResponseEntity.ok(buildSuccessResponse(projects, request));
    }

    @GetMapping("/timesheet")
    @Operation(
            summary = "List timesheets by manager",
            description = "Returns a paginated list of all non-deleted timesheets from projects managed by the current user. Can filter by status and project. Default sort by created date descending."
    )
    public ResponseEntity<CommonSuccessResponse<PaginationResponse<TimesheetDto>>> getTimesheetsByManager(
            @AuthenticationPrincipal UserPrincipalDto userPrincipalDto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) ETimesheetStatus status,
            @RequestParam(required = false) Long projectId,
            HttpServletRequest request) {

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PaginationResponse<TimesheetDto> timesheets = timesheetService.getTimesheetsByManagerWithFilters(
                userPrincipalDto.getId(),
                paginationRequest,
                status,
                projectId
        );
        return ResponseEntity.ok(buildSuccessResponse(timesheets, request));
    }

    @PutMapping("/dayoff/approval")
    @Operation(
            summary = "Approve or reject day off request",
            description = "Manager can approve or reject a day off request from a user"
    )
    public ResponseEntity<CommonSuccessResponse<DayOffDto>> approveDayOffRequest(
            @Valid @RequestBody ApprovalDayOffRequest approvalDayOffRequest,
            HttpServletRequest request) {
        DayOffDto dayOff = dayOffService.approveDayOffRequest(approvalDayOffRequest);
        return ResponseEntity.ok(buildSuccessResponse(dayOff, request));
    }

    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .data(data)
                .build();
    }
}

