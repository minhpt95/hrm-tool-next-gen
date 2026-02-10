package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.vatek.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.dto.project.ProjectDto;
import com.vatek.hrmtoolnextgen.dto.request.CreateDayOffRequest;
import com.vatek.hrmtoolnextgen.dto.request.CreateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.request.PaginationRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.vatek.hrmtoolnextgen.dto.response.PaginationResponse;
import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import com.vatek.hrmtoolnextgen.enumeration.EUserLevel;
import com.vatek.hrmtoolnextgen.enumeration.EUserPosition;
import com.vatek.hrmtoolnextgen.enumeration.EUserRole;
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

import java.util.List;

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
    private final UserService userService;

    @PostMapping("/timesheet")
    @Operation(
            summary = "Create timesheet entry",
            description = "Creates a new timesheet entry for the current user. Requires project ID, working hours, working day, and optional title, description, and timesheet type."
    )
    public ResponseEntity<CommonSuccessResponse<TimesheetDto>> createTimesheet(
            @Valid @RequestBody CreateTimesheetRequest createTimesheetReq,
            HttpServletRequest request
    ) {
        TimesheetDto timesheetDto = timesheetService.createTimesheet(createTimesheetReq);

        return ResponseEntity.ok(buildSuccessResponse(timesheetDto, request));
    }

    @PutMapping("/timesheet")
    @Operation(
            summary = "Update timesheet entry",
            description = "Updates an existing timesheet entry. Requires the timesheet ID along with the fields to update."
    )
    public ResponseEntity<CommonSuccessResponse<TimesheetDto>> updateTimesheet(
            @Valid @RequestBody UpdateTimesheetRequest updateTimesheetReq,
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

    @GetMapping("/birthday/today")
    @Operation(
            summary = "Get users with birthday today",
            description = "Returns a paginated list of all users who have birthday today. Default sort by id ascending."
    )
    public ResponseEntity<CommonSuccessResponse<PaginationResponse<UserDto>>> getUsersWithBirthdayToday(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            HttpServletRequest request) {

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PaginationResponse<UserDto> users = userService.getUsersWithBirthdayToday(paginationRequest);
        return ResponseEntity.ok(buildSuccessResponse(users, request));
    }

    @GetMapping("/birthday/upcoming")
    @Operation(
            summary = "Get users with upcoming birthdays",
            description = "Returns a paginated list of all users who have birthdays in the next 4 days (excluding today). The endpoint checks birthdays for tomorrow through 4 days from today. Default sort by id ascending."
    )
    public ResponseEntity<CommonSuccessResponse<PaginationResponse<UserDto>>> getUsersWithUpcomingBirthdays(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            HttpServletRequest request) {

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PaginationResponse<UserDto> users = userService.getUsersWithUpcomingBirthdays(paginationRequest);
        return ResponseEntity.ok(buildSuccessResponse(users, request));
    }

    @GetMapping("/roles")
    @Operation(
            summary = "Get all roles",
            description = "Returns all available user roles"
    )
    public ResponseEntity<CommonSuccessResponse<List<EUserRole>>> getAllRoles(HttpServletRequest request) {
        List<EUserRole> roles = userService.getAllRoles();
        return ResponseEntity.ok(buildSuccessResponse(roles, request));
    }

    @GetMapping("/positions")
    @Operation(
            summary = "Get all user positions",
            description = "Returns all available user positions"
    )
    public ResponseEntity<CommonSuccessResponse<List<EUserPosition>>> getAllPositions(HttpServletRequest request) {
        List<EUserPosition> positions = userService.getAllPositions();
        return ResponseEntity.ok(buildSuccessResponse(positions, request));
    }

    @GetMapping("/levels")
    @Operation(
            summary = "Get all user levels",
            description = "Returns all available user levels"
    )
    public ResponseEntity<CommonSuccessResponse<List<EUserLevel>>> getAllLevels(HttpServletRequest request) {
        List<EUserLevel> levels = userService.getAllLevels();
        return ResponseEntity.ok(buildSuccessResponse(levels, request));
    }

    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .httpStatusCode(HttpStatus.OK)
                .message("Successfully")
                .data(data)
                .build();
    }
}

