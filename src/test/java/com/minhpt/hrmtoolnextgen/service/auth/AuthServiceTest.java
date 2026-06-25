package com.minhpt.hrmtoolnextgen.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.LoginRequest;
import com.minhpt.hrmtoolnextgen.dto.request.RefreshTokenRequest;
import com.minhpt.hrmtoolnextgen.dto.response.LoginResponse;
import com.minhpt.hrmtoolnextgen.dto.response.RefreshTokenResponse;
import com.minhpt.hrmtoolnextgen.exception.UnauthorizedException;

/**
 * Unit tests for AuthService.
 *
 * AuthService is a thin delegator: login/refreshToken/logout dispatch straight to
 * AuthSessionService; forgotPassword/resetPassword dispatch to AuthAccountService.
 * These tests verify the delegation contract and that exceptions propagate unchanged.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthSessionService authSessionService;
    @Mock
    private AuthAccountService authAccountService;

    @InjectMocks
    private AuthService authService;

    // -------------------------------------------------------------------------
    // login — success path delegates to AuthSessionService
    // -------------------------------------------------------------------------

    @Test
    void login_success_delegatesAndReturnsLoginResponse() {
        LoginRequest req = new LoginRequest("user@example.com", "secret");
        LoginResponse expected = LoginResponse.builder()
                .id(1L)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .email("user@example.com")
                .firstName("First")
                .lastName("Last")
                .roles(List.of("USER"))
                .build();

        when(authSessionService.login(req)).thenReturn(expected);

        LoginResponse result = authService.login(req);

        assertEquals(expected, result);
        verify(authSessionService).login(req);
    }

    // -------------------------------------------------------------------------
    // login — invalid credentials propagates UnauthorizedException (R1.2)
    // -------------------------------------------------------------------------

    @Test
    void login_withInvalidCredentials_propagatesUnauthorizedException() {
        LoginRequest req = new LoginRequest("user@example.com", "wrong-password");
        when(authSessionService.login(req)).thenThrow(new UnauthorizedException("Invalid credentials"));

        assertThrows(UnauthorizedException.class, () -> authService.login(req));
    }

    // -------------------------------------------------------------------------
    // refreshToken — delegates and returns response
    // -------------------------------------------------------------------------

    @Test
    void refreshToken_delegatesAndReturnsResponse() {
        RefreshTokenRequest req = new RefreshTokenRequest("refresh-token");
        RefreshTokenResponse expected = RefreshTokenResponse.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .type("Bearer")
                .build();
        when(authSessionService.refreshToken(req)).thenReturn(expected);

        RefreshTokenResponse result = authService.refreshToken(req);

        assertEquals(expected, result);
        verify(authSessionService).refreshToken(req);
    }

    // -------------------------------------------------------------------------
    // logout — delegates to AuthSessionService
    // -------------------------------------------------------------------------

    @Test
    void logout_delegatesToAuthSessionService() {
        UserPrincipalDto principal = UserPrincipalDto.internalBuilder()
                .id(1L).email("user@example.com").build();

        authService.logout(principal);

        verify(authSessionService).logout(principal);
    }
}
