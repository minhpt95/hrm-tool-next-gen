package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.request.CreateUserRequest;
import com.vatek.hrmtoolnextgen.dto.request.PaginationRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateUserRequest;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.vatek.hrmtoolnextgen.dto.response.PaginationResponse;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@AllArgsConstructor
@Log4j2
@RequestMapping("/users")
@Tag(name = "Users", description = "CRUD APIs for managing employees")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(
            summary = "List users",
            description = "Returns a paginated list of employees with profile and role information."
    )
    public ResponseEntity<CommonSuccessResponse<PaginationResponse<UserDto>>> getAllUsers(
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

        PaginationResponse<UserDto> users = userService.getPageUsers(paginationRequest);
        return ResponseEntity.ok(buildSuccessResponse(users, request));
    }

    @GetMapping("/{id}")
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

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
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

    @PutMapping("/{id}")
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

    @DeleteMapping("/{id}")
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

    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .data(data)
                .build();
    }
}

