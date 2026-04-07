package com.minhpt.hrmtoolnextgen.service;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.component.jwt.JwtProvider;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.LoginRequest;
import com.minhpt.hrmtoolnextgen.dto.request.RefreshTokenRequest;
import com.minhpt.hrmtoolnextgen.dto.response.LoginResponse;
import com.minhpt.hrmtoolnextgen.dto.response.RefreshTokenResponse;
import com.minhpt.hrmtoolnextgen.entity.redis.UserTokenRedisEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserTokenType;
import com.minhpt.hrmtoolnextgen.exception.UnauthorizedException;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.repository.redis.UserTokenRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthSessionService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UserTokenRedisRepository userTokenRedisRepository;
    private final MessageService messageService;

    @Value("${hrm.app.jwtExpiration}")
    private long jwtExpiration;

    @Value("${hrm.app.refreshTokenExpiration}")
    private long refreshTokenExpiration;

    public LoginResponse login(LoginRequest loginRequest) {
        try {
            log.info("Login attempt for user: {}", loginRequest.getUsername());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipalDto principal = (UserPrincipalDto) authentication.getPrincipal();

            String accessToken = jwtProvider.generateJwtToken(authentication);
            String refreshToken = jwtProvider.generateRefreshToken(principal.getEmail(), principal.getId());

            saveTokenToRedis(principal.getId(), accessToken, EUserTokenType.ACCESS_TOKEN, jwtExpiration);
            saveTokenToRedis(principal.getId(), refreshToken, EUserTokenType.REFRESH_TOKEN, refreshTokenExpiration);

            log.info("User {} logged in successfully", principal.getEmail());
            return LoginResponse.builder()
                    .id(principal.getId())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .email(principal.getEmail())
                    .firstName(principal.getFirstName())
                    .lastName(principal.getLastName())
                    .roles(principal.getRoles())
                    .build();
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException(messageService.getMessage("auth.login.invalid"));
        }
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.info("Refresh token request received");
        String refreshToken = refreshTokenRequest.getRefreshToken();

        if (!jwtProvider.validateJwtToken(refreshToken)) {
            throw new UnauthorizedException(messageService.getMessage("auth.refresh.token.invalid"));
        }
        if (!jwtProvider.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException(messageService.getMessage("auth.refresh.token.not.refresh"));
        }

        Long userId = jwtProvider.getIdFromJwtToken(refreshToken);
        if (userId == null) {
            throw new UnauthorizedException(messageService.getMessage("auth.refresh.token.invalid.id"));
        }

        UserTokenRedisEntity storedRefreshToken = userTokenRedisRepository
                .findUserByUserIdAndTokenType(userId, EUserTokenType.REFRESH_TOKEN);
        if (storedRefreshToken == null || !storedRefreshToken.getToken().equals(refreshToken)) {
            throw new UnauthorizedException(messageService.getMessage("auth.refresh.token.not.found"));
        }

        String email = jwtProvider.getEmailFromJwtToken(refreshToken);
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new UnauthorizedException(messageService.getMessage("auth.user.not.found"));
        }

        String newAccessToken = jwtProvider.generateTokenFromEmail(email, userId);
        String newRefreshToken = jwtProvider.generateRefreshToken(email, userId);

        invalidateUserTokens(userId);
        saveTokenToRedis(userId, newAccessToken, EUserTokenType.ACCESS_TOKEN, jwtExpiration);
        saveTokenToRedis(userId, newRefreshToken, EUserTokenType.REFRESH_TOKEN, refreshTokenExpiration);

        log.info("Tokens refreshed successfully for user id: {}", userId);
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .type("Bearer")
                .build();
    }

    public void logout(UserPrincipalDto userPrincipal) {
        if (userPrincipal != null && userPrincipal.getId() != null) {
            invalidateUserTokens(userPrincipal.getId());
            log.info("User {} logged out successfully", userPrincipal.getEmail());
        }
    }

    public void saveTokenToRedis(Long userId, String token, EUserTokenType tokenType, long expiration) {
        String id = userId + ":" + tokenType.name();
        UserTokenRedisEntity tokenEntity = UserTokenRedisEntity.builder()
                .id(id)
                .userId(userId)
                .tokenType(tokenType)
                .token(token)
                .ttl(expiration)
                .build();
        userTokenRedisRepository.save(Objects.requireNonNull(tokenEntity));
    }

    public void invalidateUserTokens(Long userId) {
        UserTokenRedisEntity accessToken = userTokenRedisRepository
                .findUserByUserIdAndTokenType(userId, EUserTokenType.ACCESS_TOKEN);
        if (accessToken != null) {
            userTokenRedisRepository.delete(accessToken);
        }

        UserTokenRedisEntity refreshToken = userTokenRedisRepository
                .findUserByUserIdAndTokenType(userId, EUserTokenType.REFRESH_TOKEN);
        if (refreshToken != null) {
            userTokenRedisRepository.delete(refreshToken);
        }
    }
}
