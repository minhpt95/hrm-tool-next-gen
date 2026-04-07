package com.minhpt.hrmtoolnextgen.service;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.constant.CommonConstant;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.ForgotPasswordRequest;
import com.minhpt.hrmtoolnextgen.dto.request.RegisterRequest;
import com.minhpt.hrmtoolnextgen.dto.request.ResetPasswordRequest;
import com.minhpt.hrmtoolnextgen.dto.response.RegisterResponse;
import com.minhpt.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.entity.redis.UserTokenRedisEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserTokenType;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.mapping.UserMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.repository.redis.UserTokenRedisRepository;
import com.minhpt.hrmtoolnextgen.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapping userMapping;
    private final RoleRepository roleRepository;
    private final UserTokenRedisRepository userTokenRedisRepository;
    private final EmailService emailService;
    private final MessageService messageService;
    private final AuthSessionService authSessionService;

    @Value("${hrm.app.resetPasswordTokenExpiration:3600000}")
    private long resetPasswordTokenExpiration;

    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest, UserPrincipalDto userPrincipal) {
        log.info("Register request for email: {}", registerRequest.getEmail());
        UserEntity userEntity = userRepository.findByEmail(registerRequest.getEmail()).orElse(null);
        if (userEntity != null) {
            throw new BadRequestException(messageService.getMessage("auth.email.already.in.use"));
        }

        userEntity = userMapping.createUser(registerRequest);
        userEntity.setActive(true);
        userEntity.getUserInfo().setCreatedBy(userPrincipal == null ? 0L : userPrincipal.getId());

        List<RoleEntity> roles = roleRepository.findByUserRoleIn(registerRequest.getRoles().stream().toList());
        if (!roles.isEmpty()) {
            userEntity.setRoles(roles);
        }

        String password = CommonUtils.randomPassword(CommonConstant.DEFAULT_PASSWORD_LENGTH);
        userEntity.setPassword(passwordEncoder.encode(password));
        userEntity.setCreatedBy(0L);
        userEntity.setCreatedDate(LocalDateTime.now());

        userEntity = userRepository.save(userEntity);
        log.info("User registered successfully with id: {} and email: {}", userEntity.getId(), userEntity.getEmail());

        return RegisterResponse.builder()
                .id(userEntity.getId())
                .email(userEntity.getEmail())
                .password(password)
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());
        UserEntity userEntity = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("user.not.found.email", request.getEmail())));

        if (!userEntity.isActive()) {
            throw new BadRequestException(messageService.getMessage("auth.account.not.active"));
        }

        String resetToken = CommonUtils.randomPassword(CommonConstant.RESET_TOKEN_LENGTH);
        authSessionService.saveTokenToRedis(
                userEntity.getId(),
                resetToken,
                EUserTokenType.RESET_PASSWORD_TOKEN,
                resetPasswordTokenExpiration
        );

        log.info("Password reset token generated for user: {} (email: {})", userEntity.getId(), request.getEmail());
        emailService.sendPasswordResetEmail(request.getEmail(), resetToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        List<UserTokenRedisEntity> resetTokens = userTokenRedisRepository.findByTokenType(EUserTokenType.RESET_PASSWORD_TOKEN);
        UserTokenRedisEntity resetTokenEntity = resetTokens.stream()
                .filter(token -> token.getToken().equals(request.getToken()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("auth.reset.token.invalid")));

        Long userId = resetTokenEntity.getUserId();
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("auth.user.not.found")));

        if (!userEntity.isActive()) {
            throw new BadRequestException(messageService.getMessage("auth.account.not.active"));
        }

        userEntity.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(userEntity);
        userTokenRedisRepository.delete(resetTokenEntity);

        log.info("Password reset successfully for user: {} (email: {})", userId, userEntity.getEmail());
    }
}
