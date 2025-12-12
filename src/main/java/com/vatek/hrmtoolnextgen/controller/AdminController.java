package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.request.CreateUserRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateUserRequest;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
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
@RequestMapping("/admin")
@Tag(name = "Admin", description = "CRUD APIs for Admin User")
public class AdminController {

    private final UserService userService;

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

