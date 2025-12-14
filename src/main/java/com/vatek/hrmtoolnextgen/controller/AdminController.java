package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.project.ProjectDto;
import com.vatek.hrmtoolnextgen.dto.request.CreateUserRequest;
import com.vatek.hrmtoolnextgen.dto.request.PaginationRequest;
import com.vatek.hrmtoolnextgen.dto.request.SetUserPasswordRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateUserRequest;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.vatek.hrmtoolnextgen.dto.response.PaginationResponse;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import com.vatek.hrmtoolnextgen.service.ProjectService;
import com.vatek.hrmtoolnextgen.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@AllArgsConstructor
@Log4j2
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "CRUD APIs for Admin User")
public class AdminController {

    private final UserService userService;
    private final ProjectService projectService;

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

    @PostMapping(value = "/user", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(
            summary = "Create user",
            description = "Creates a new employee profile"
    )
    public ResponseEntity<CommonSuccessResponse<UserDto>> createUser(
            @Valid @ModelAttribute CreateUserRequest createUserRequest,
            HttpServletRequest request) {
        UserDto user = userService.createUser(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildSuccessResponse(user, request));
    }

    @PutMapping("/user/{id}")
    @Operation(
            summary = "Update user",
            description = "Updates employee profile, status, and role assignments."
    )
    public ResponseEntity<CommonSuccessResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateUserRequest updateUserRequest,
            HttpServletRequest request) {
        UserDto user = userService.updateUser(id, updateUserRequest);
        return ResponseEntity.ok(buildSuccessResponse(user, request));
    }

    @DeleteMapping("/user/{id}")
    @Operation(
            summary = "Delete user",
            description = "Soft-deactivates an employee by ID."
    )
    public ResponseEntity<CommonSuccessResponse<Void>> deleteUser(
            @PathVariable Long id,
            HttpServletRequest request) {
        userService.deleteUser(id);
        return ResponseEntity.ok(buildSuccessResponse(null, request));
    }

    @PutMapping("/user/{id}/password")
    @Operation(
            summary = "Set user password",
            description = "Allows admin to set a new password for a specific user. Password must be at least 6 characters long."
    )
    public ResponseEntity<CommonSuccessResponse<Void>> setUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody SetUserPasswordRequest setUserPasswordRequest,
            HttpServletRequest request) {
        userService.setUserPassword(id, setUserPasswordRequest.getNewPassword());
        return ResponseEntity.ok(buildSuccessResponse(null, request));
    }

    @GetMapping("/users")
    @Operation(
            summary = "Get all users (Admin)",
            description = "Returns a paginated list of all users. Can filter by name and email. Default sort by id ascending."
    )
    public ResponseEntity<CommonSuccessResponse<PaginationResponse<UserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            HttpServletRequest request) {

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PaginationResponse<UserDto> users = userService.getAllUsersForAdmin(
                paginationRequest,
                name,
                email
        );
        return ResponseEntity.ok(buildSuccessResponse(users, request));
    }

    @GetMapping("/projects")
    @Operation(
            summary = "Get all projects (Admin)",
            description = "Returns a paginated list of all non-deleted projects. Can filter by project name and project status. Default sort by created date descending."
    )
    public ResponseEntity<CommonSuccessResponse<PaginationResponse<ProjectDto>>> getAllProjects(
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

        PaginationResponse<ProjectDto> projects = projectService.getAllProjectsForAdmin(
                paginationRequest,
                projectName,
                projectStatus
        );
        return ResponseEntity.ok(buildSuccessResponse(projects, request));
    }

    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .data(data)
                .build();
    }
}

