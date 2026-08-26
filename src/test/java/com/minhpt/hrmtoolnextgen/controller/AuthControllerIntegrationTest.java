package com.minhpt.hrmtoolnextgen.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.minhpt.hrmtoolnextgen.component.TokenBucketRateLimiter;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.LoginRequest;
import com.minhpt.hrmtoolnextgen.dto.request.RefreshTokenRequest;
import com.minhpt.hrmtoolnextgen.dto.response.LoginResponse;
import com.minhpt.hrmtoolnextgen.dto.response.RefreshTokenResponse;
import com.minhpt.hrmtoolnextgen.exception.UnauthorizedException;
import com.minhpt.hrmtoolnextgen.service.auth.AuthService;
import com.minhpt.hrmtoolnextgen.support.AbstractIntegrationTest;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Integration tests for AuthController endpoints.
 *
 * Strategy: AuthService is @MockBean so the test is fully deterministic without
 * live Redis, SMTP, or a real authentication flow. TokenBucketRateLimiter is
 * @MockBean (returns true = allowed) to avoid Redis calls from the @RateLimit
 * aspect. JavaMailSender is @MockBean to prevent SMTP wiring failures.
 *
 * Auth endpoints are permit-all in the security config, so no @WithMockUser
 * is needed for the login/refresh/logout/forgot/reset endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @TestConfiguration
    static class TestConfig extends AbstractIntegrationTest {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    // -------------------------------------------------------------------------
    // /api/auth/login — happy path
    // -------------------------------------------------------------------------

    @Test
    void login_withValidCredentials_returns200AndTokenBody() throws Exception {
        LoginResponse loginResponse = LoginResponse.builder()
                .id(1L)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .email("user@example.com")
                .firstName("First")
                .lastName("Last")
                .roles(List.of("USER"))
                .build();

        when(tokenBucketRateLimiter.tryConsume(any(), any(int.class), any(int.class))).thenReturn(true);
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user@example.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    // -------------------------------------------------------------------------
    // /api/auth/login — bad credentials → 401
    // -------------------------------------------------------------------------

    @Test
    void login_withBadCredentials_returns401() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(any(), any(int.class), any(int.class))).thenReturn(true);
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user@example.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // /api/auth/login — 401 with localized error body (Vietnamese)
    // -------------------------------------------------------------------------

    @Test
    void login_withBadCredentials_returnsLocalizedMessageBody() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(any(), any(int.class), any(int.class))).thenReturn(true);
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Tên đăng nhập hoặc mật khẩu không đúng"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "vi")
                        .content("""
                                {"username":"user@example.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));
    }

    // -------------------------------------------------------------------------
    // /api/v1/auth/login — dual-mapped path works identically
    // -------------------------------------------------------------------------

    @Test
    void login_viaV1Path_returns200() throws Exception {
        LoginResponse loginResponse = LoginResponse.builder()
                .id(2L).accessToken("tok-v1").refreshToken("r-v1").email("v1@example.com").build();

        when(tokenBucketRateLimiter.tryConsume(any(), any(int.class), any(int.class))).thenReturn(true);
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"v1@example.com","password":"secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("tok-v1"));
    }

    // -------------------------------------------------------------------------
    // /api/auth/refresh — happy path
    // -------------------------------------------------------------------------

    @Test
    void refreshToken_withValidToken_returns200AndNewTokens() throws Exception {
        RefreshTokenResponse refreshResponse = RefreshTokenResponse.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .type("Bearer")
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(refreshResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"some-refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.type").value("Bearer"));
    }

    // -------------------------------------------------------------------------
    // /api/auth/refresh — invalid token → 401
    // -------------------------------------------------------------------------

    @Test
    void refreshToken_withInvalidToken_returns401() throws Exception {
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"bad-token"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // /api/auth/logout — happy path (no auth principal in MockMvc = null principal)
    // -------------------------------------------------------------------------

    @Test
    void logout_returns200WithSuccessMessage() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isString());
    }

    // -------------------------------------------------------------------------
    // /api/auth/forgot-password — valid email
    // -------------------------------------------------------------------------

    @Test
    void forgotPassword_withValidEmail_returns200() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(any(), any(int.class), any(int.class))).thenReturn(true);
        doNothing().when(authService).forgotPassword(any());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isString());
    }

    // -------------------------------------------------------------------------
    // /api/auth/forgot-password — unknown email → 400 (code throws BadRequestException)
    // -------------------------------------------------------------------------

    @Test
    void forgotPassword_withUnknownEmail_returns400() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(any(), any(int.class), any(int.class))).thenReturn(true);
        doThrow(new com.minhpt.hrmtoolnextgen.exception.BadRequestException("User not found"))
                .when(authService).forgotPassword(any());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // /api/auth/reset-password — valid token
    // -------------------------------------------------------------------------

    @Test
    void resetPassword_withValidToken_returns200() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(any(), any(int.class), any(int.class))).thenReturn(true);
        doNothing().when(authService).resetPassword(any());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"valid-reset-token","newPassword":"NewPassword1!"}
                                """))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // /api/auth/reset-password — invalid token → 400
    // -------------------------------------------------------------------------

    @Test
    void resetPassword_withInvalidToken_returns400() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(any(), any(int.class), any(int.class))).thenReturn(true);
        doThrow(new com.minhpt.hrmtoolnextgen.exception.BadRequestException("Token invalid"))
                .when(authService).resetPassword(any());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"bad-token","newPassword":"NewPassword1!"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Validation — blank fields return 400
    // -------------------------------------------------------------------------

    @Test
    void login_withBlankUsername_returns400FromValidation() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"secret"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withMissingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
