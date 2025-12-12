package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.dto.project.ProjectDto;
import com.vatek.hrmtoolnextgen.dto.request.CreateDayOffRequest;
import com.vatek.hrmtoolnextgen.dto.request.CreateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.request.PaginationRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.vatek.hrmtoolnextgen.dto.response.PaginationResponse;
import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import com.vatek.hrmtoolnextgen.service.DayOffService;
import com.vatek.hrmtoolnextgen.service.ProjectService;
import com.vatek.hrmtoolnextgen.service.TimesheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@AllArgsConstructor
@Log4j2
@RequestMapping("/api/user")
@Tag(name = "User", description = "CRUD APIs for User")
public class UserController {

    private final TimesheetService timesheetService;
    private final ProjectService projectService;
    private final DayOffService dayOffService;

    @PostMapping("/timesheet")
    public ResponseEntity<CommonSuccessResponse<TimesheetDto>> createTimesheet(
            @RequestBody CreateTimesheetRequest createTimesheetReq,
            HttpServletRequest request
    ) {
        TimesheetDto timesheetDto = timesheetService.createTimesheet(createTimesheetReq);

        return ResponseEntity.ok(buildSuccessResponse(timesheetDto, request));
    }

    @PutMapping("/timesheet")
    public ResponseEntity<CommonSuccessResponse<TimesheetDto>> updateTimesheet(
            @RequestBody UpdateTimesheetRequest updateTimesheetReq,
            HttpServletRequest request
    ) {
        TimesheetDto timesheetDto = timesheetService.updateTimesheet(updateTimesheetReq);

        return ResponseEntity.ok(buildSuccessResponse(timesheetDto, request));
    }

    @GetMapping("/project")
    @Operation(
            summary = "List projects by member",
            description = "Returns a paginated list of all non-deleted projects where the current user is a member. Can filter by project name and project status. Default sort by created date descending."
    )
    public ResponseEntity<CommonSuccessResponse<PaginationResponse<ProjectDto>>> getProjectsByMemberId(
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

        PaginationResponse<ProjectDto> projects = projectService.getProjectsByMemberIdWithFilters(
                userPrincipalDto.getId(),
                paginationRequest,
                projectName,
                projectStatus
        );
        return ResponseEntity.ok(buildSuccessResponse(projects, request));
    }

    @PostMapping("/dayoff")
    @Operation(
            summary = "Request day off",
            description = "Creates a new day off request for the current user"
    )
    public ResponseEntity<CommonSuccessResponse<DayOffDto>> createDayOffRequest(
            @AuthenticationPrincipal UserPrincipalDto userPrincipalDto,
            @Valid @RequestBody CreateDayOffRequest createDayOffRequest,
            HttpServletRequest request) {
        DayOffDto dayOff = dayOffService.createDayOffRequest(userPrincipalDto.getId(), createDayOffRequest);
        return ResponseEntity.ok(buildSuccessResponse(dayOff, request));
    }

    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .data(data)
                .build();
    }
}

