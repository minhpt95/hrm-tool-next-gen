package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.dto.request.LoginRequest;
import com.vatek.hrmtoolnextgen.dto.request.RegisterRequest;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.vatek.hrmtoolnextgen.dto.response.LoginResponse;
import com.vatek.hrmtoolnextgen.dto.response.RegisterResponse;
import com.vatek.hrmtoolnextgen.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@AllArgsConstructor
@Log4j2
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/login")
    public ResponseEntity<CommonSuccessResponse<LoginResponse>> login(
            @RequestBody LoginRequest loginRequest, 
            HttpServletRequest request) {
        LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok(buildSuccessResponse(loginResponse, request));
    }

    @PostMapping(value = "/register", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<CommonSuccessResponse<RegisterResponse>> register(
            @ModelAttribute RegisterRequest registerRequest,
            @AuthenticationPrincipal UserPrincipalDto userPrincipal,
            HttpServletRequest request) {
        RegisterResponse registerResponse = authService.register(registerRequest, userPrincipal);
        return ResponseEntity.ok(buildSuccessResponse(registerResponse, request));
    }

    @PostMapping(value = "/register/temp", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<CommonSuccessResponse<RegisterResponse>> registerTemp(
            @RequestBody RegisterRequest registerRequest,
            @AuthenticationPrincipal UserPrincipalDto userPrincipal,
            HttpServletRequest request) {
        RegisterResponse registerResponse = authService.register(registerRequest, userPrincipal);
        return ResponseEntity.ok(buildSuccessResponse(registerResponse, request));
    }

    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .data(data)
                .build();
    }
}
