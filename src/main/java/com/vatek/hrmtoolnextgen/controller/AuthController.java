package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.dto.request.ForgotPasswordRequest;
import com.vatek.hrmtoolnextgen.dto.request.LoginRequest;
import com.vatek.hrmtoolnextgen.dto.request.RefreshTokenRequest;
import com.vatek.hrmtoolnextgen.dto.request.ResetPasswordRequest;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.vatek.hrmtoolnextgen.dto.response.LoginResponse;
import com.vatek.hrmtoolnextgen.dto.response.RefreshTokenResponse;
import com.vatek.hrmtoolnextgen.service.AuthService;
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
@RequestMapping("/${hrm.api.prefix}/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/login")
    @Operation(summary = "User login", description = "Authenticate user and return access token and refresh token")
    public ResponseEntity<CommonSuccessResponse<LoginResponse>> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok(buildSuccessResponse(loginResponse, request));
    }

    @PostMapping(value = "/refresh")
    @Operation(summary = "Refresh access token", description = "Exchange refresh token for new access token and refresh token")
    public ResponseEntity<CommonSuccessResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request) {
        RefreshTokenResponse refreshTokenResponse = authService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(buildSuccessResponse(refreshTokenResponse, request));
    }

    @PostMapping(value = "/logout")
    @Operation(summary = "User logout", description = "Invalidate user's access token and refresh token")
    public ResponseEntity<CommonSuccessResponse<String>> logout(
            @AuthenticationPrincipal UserPrincipalDto userPrincipal,
            HttpServletRequest request) {
        authService.logout(userPrincipal);
        return ResponseEntity.ok(buildSuccessResponse("Logged out successfully", request));
    }

    @PostMapping(value = "/forgot-password")
    @Operation(
            summary = "Forgot password",
            description = "Generates a password reset token for the user with the provided email and sends it via email."
    )
    public ResponseEntity<CommonSuccessResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest,
            HttpServletRequest request) {
        authService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.ok(buildSuccessResponse(
                "If the email exists, a password reset link has been sent to your email address.",
                request));
    }

    @PostMapping(value = "/reset-password")
    @Operation(
            summary = "Reset password",
            description = "Resets the user's password using the reset token received via email. The token must be valid and not expired."
    )
    public ResponseEntity<CommonSuccessResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest resetPasswordRequest,
            HttpServletRequest request) {
        authService.resetPassword(resetPasswordRequest);
        return ResponseEntity.ok(buildSuccessResponse(
                "Password has been reset successfully. You can now login with your new password.",
                request));
    }

    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .data(data)
                .build();
    }
}
