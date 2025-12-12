package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.dto.project.ProjectDto;
import com.vatek.hrmtoolnextgen.dto.request.ApprovalTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateProjectRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.service.ProjectService;
import com.vatek.hrmtoolnextgen.service.TimesheetService;
import com.vatek.hrmtoolnextgen.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@AllArgsConstructor
@Log4j2
@RequestMapping("/manager")
@Tag(name = "Manager", description = "CRUD APIs for Manager")
public class ManagerController {

    private final UserService userService;
    private final ProjectService projectService;
    private final TimesheetService timesheetService;

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
            description = "Returns all active projects managed by the given user."
    )
    public ResponseEntity<CommonSuccessResponse<List<ProjectDto>>> getProjectsByManagerId(
            @AuthenticationPrincipal UserPrincipalDto userPrincipalDto,
            HttpServletRequest request) {
        List<ProjectDto> projects = projectService.getProjectsByManagerId(userPrincipalDto.getId());
        return ResponseEntity.ok(buildSuccessResponse(projects, request));
    }


    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .data(data)
                .build();
    }
}

