package com.minhpt.hrmtoolnextgen.service.auth;

import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.ForgotPasswordRequest;
import com.minhpt.hrmtoolnextgen.dto.request.LoginRequest;
import com.minhpt.hrmtoolnextgen.dto.request.RefreshTokenRequest;
import com.minhpt.hrmtoolnextgen.dto.request.RegisterRequest;
import com.minhpt.hrmtoolnextgen.dto.request.ResetPasswordRequest;
import com.minhpt.hrmtoolnextgen.dto.response.LoginResponse;
import com.minhpt.hrmtoolnextgen.dto.response.RefreshTokenResponse;
import com.minhpt.hrmtoolnextgen.dto.response.RegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthSessionService authSessionService;
    private final AuthAccountService authAccountService;

    public LoginResponse login(LoginRequest loginRequest) {
        return authSessionService.login(loginRequest);
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        return authSessionService.refreshToken(refreshTokenRequest);
    }

    public void logout(UserPrincipalDto userPrincipal) {
        authSessionService.logout(userPrincipal);
    }

    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest, UserPrincipalDto userPrincipal) {
        return authAccountService.register(registerRequest, userPrincipal);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        authAccountService.forgotPassword(request);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        authAccountService.resetPassword(request);
    }
}
