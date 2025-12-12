package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.component.jwt.JwtProvider;
import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.dto.request.ForgotPasswordRequest;
import com.vatek.hrmtoolnextgen.dto.request.LoginRequest;
import com.vatek.hrmtoolnextgen.dto.request.RefreshTokenRequest;
import com.vatek.hrmtoolnextgen.dto.request.RegisterRequest;
import com.vatek.hrmtoolnextgen.dto.request.ResetPasswordRequest;
import com.vatek.hrmtoolnextgen.dto.response.LoginResponse;
import com.vatek.hrmtoolnextgen.dto.response.RefreshTokenResponse;
import com.vatek.hrmtoolnextgen.dto.response.RegisterResponse;
import com.vatek.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.entity.redis.UserTokenRedisEntity;
import com.vatek.hrmtoolnextgen.enumeration.EUserTokenType;
import com.vatek.hrmtoolnextgen.exception.BadRequestException;
import com.vatek.hrmtoolnextgen.exception.UnauthorizedException;
import com.vatek.hrmtoolnextgen.mapping.UserMapping;
import com.vatek.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.UserRepository;
import com.vatek.hrmtoolnextgen.repository.redis.UserTokenRedisRepository;
import com.vatek.hrmtoolnextgen.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UserMapping userMapping;
    private final RoleRepository roleRepository;
    private final UserTokenRedisRepository userTokenRedisRepository;
    private final EmailService emailService;

    @Value("${hrm.app.jwtExpiration}")
    private long jwtExpiration;

    @Value("${hrm.app.refreshTokenExpiration}")
    private long refreshTokenExpiration;

    @Value("${hrm.app.resetPasswordTokenExpiration:3600000}")
    private long resetPasswordTokenExpiration; // Default 1 hour in milliseconds

    public LoginResponse login(LoginRequest loginRequest) {
        // Authentication manager will handle password validation
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipalDto principal = (UserPrincipalDto) authentication.getPrincipal();

        // Generate tokens
        String accessToken = jwtProvider.generateJwtToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(principal.getEmail(), principal.getId());

        // Store tokens in Redis
        saveTokenToRedis(principal.getId(), accessToken, EUserTokenType.ACCESS_TOKEN, jwtExpiration);
        saveTokenToRedis(principal.getId(), refreshToken, EUserTokenType.REFRESH_TOKEN, refreshTokenExpiration);

        return LoginResponse
                .builder()
                .id(principal.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(principal.getEmail())
                .firstName(principal.getFirstName())
                .lastName(principal.getLastName())
                .roles(principal.getRoles())
                .build();
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        // Validate refresh token
        if (!jwtProvider.validateJwtToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        // Ensure it's actually a refresh token, not an access token
        if (!jwtProvider.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Token provided is not a refresh token");
        }

        // Get user ID from token
        Long userId = jwtProvider.getIdFromJwtToken(refreshToken);
        if (userId == null) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        // Verify refresh token exists in Redis
        UserTokenRedisEntity storedRefreshToken = userTokenRedisRepository
                .findUserByUserIdAndTokenType(userId, EUserTokenType.REFRESH_TOKEN);

        if (storedRefreshToken == null || !storedRefreshToken.getToken().equals(refreshToken)) {
            throw new UnauthorizedException("Refresh token not found or invalid");
        }

        // Get user email from token
        String email = jwtProvider.getEmailFromJwtToken(refreshToken);

        // Verify user exists
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new UnauthorizedException("User not found");
        }

        // Generate new tokens
        String newAccessToken = jwtProvider.generateTokenFromEmail(email, userId);
        String newRefreshToken = jwtProvider.generateRefreshToken(email, userId);

        // Invalidate old tokens and store new ones
        invalidateUserTokens(userId);
        saveTokenToRedis(userId, newAccessToken, EUserTokenType.ACCESS_TOKEN, jwtExpiration);
        saveTokenToRedis(userId, newRefreshToken, EUserTokenType.REFRESH_TOKEN, refreshTokenExpiration);

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

    private void saveTokenToRedis(Long userId, String token, EUserTokenType tokenType, long expiration) {
        String id = userId + ":" + tokenType.name();
        UserTokenRedisEntity tokenEntity = UserTokenRedisEntity.builder()
                .id(id)
                .userId(userId)
                .tokenType(tokenType)
                .token(token)
                .ttl(expiration)
                .build();

        userTokenRedisRepository.save(tokenEntity);
    }

    private void invalidateUserTokens(Long userId) {
        // Delete access token
        UserTokenRedisEntity accessToken = userTokenRedisRepository
                .findUserByUserIdAndTokenType(userId, EUserTokenType.ACCESS_TOKEN);
        if (accessToken != null) {
            userTokenRedisRepository.delete(accessToken);
        }

        // Delete refresh token
        UserTokenRedisEntity refreshToken = userTokenRedisRepository
                .findUserByUserIdAndTokenType(userId, EUserTokenType.REFRESH_TOKEN);
        if (refreshToken != null) {
            userTokenRedisRepository.delete(refreshToken);
        }
    }

    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest, UserPrincipalDto userPrincipal) {
        UserEntity userEntity = userRepository.findByEmail(registerRequest.getEmail()).orElse(null);

        if (userEntity != null) {
            throw new BadRequestException("Email already in use");
        }

        userEntity = userMapping.createUser(registerRequest);

        userEntity.setActive(true);
        if (userPrincipal == null) {
            userEntity.getUserInfo().setCreatedBy(0L);
        } else {
            userEntity.getUserInfo().setCreatedBy(userPrincipal.getId());
        }

        List<RoleEntity> roles = roleRepository.findByUserRoleIn(registerRequest.getRoles().stream().toList());

        if (!roles.isEmpty()) {
            userEntity.setRoles(roles);
        }

        String password = CommonUtils.randomPassword(12);

        userEntity.setPassword(passwordEncoder.encode(password));
        userEntity.setCreatedBy(0L);
        userEntity.setCreatedDate(LocalDateTime.now());

        userEntity = userRepository.save(userEntity);

        return RegisterResponse
                .builder()
                .id(userEntity.getId())
                .email(userEntity.getEmail())
                .password(password)
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Find user by email
        UserEntity userEntity = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User with email '" + request.getEmail() + "' not found"));

        // Check if user is active
        if (!userEntity.isActive()) {
            throw new BadRequestException("User account is not active");
        }

        // Generate reset password token
        String resetToken = CommonUtils.randomPassword(32); // Generate a secure random token

        // Store reset token in Redis with expiration (default 1 hour)
        saveTokenToRedis(userEntity.getId(), resetToken, EUserTokenType.RESET_PASSWORD_TOKEN, resetPasswordTokenExpiration);

        log.info("Password reset token generated for user: {} (email: {})", userEntity.getId(), request.getEmail());
        
        // Send email with reset token/link
        emailService.sendPasswordResetEmail(request.getEmail(), resetToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Find the reset token in Redis
        List<UserTokenRedisEntity> resetTokens = userTokenRedisRepository
                .findByTokenType(EUserTokenType.RESET_PASSWORD_TOKEN);
        
        UserTokenRedisEntity resetTokenEntity = resetTokens.stream()
                .filter(token -> token.getToken().equals(request.getToken()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        Long userId = resetTokenEntity.getUserId();

        // Verify user exists
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Check if user is active
        if (!userEntity.isActive()) {
            throw new BadRequestException("User account is not active");
        }

        // Update password
        userEntity.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(userEntity);

        // Invalidate the reset token
        userTokenRedisRepository.delete(resetTokenEntity);

        log.info("Password reset successfully for user: {} (email: {})", userId, userEntity.getEmail());
    }
}
