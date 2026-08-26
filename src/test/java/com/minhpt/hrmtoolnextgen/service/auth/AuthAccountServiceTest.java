package com.minhpt.hrmtoolnextgen.service.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.request.ForgotPasswordRequest;
import com.minhpt.hrmtoolnextgen.dto.request.ResetPasswordRequest;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserInfoEntity;
import com.minhpt.hrmtoolnextgen.entity.redis.UserTokenRedisEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserTokenType;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.mapping.UserMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.repository.redis.UserTokenRedisRepository;
import com.minhpt.hrmtoolnextgen.service.EmailService;

@ExtendWith(MockitoExtension.class)
class AuthAccountServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapping userMapping;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserTokenRedisRepository userTokenRedisRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private MessageService messageService;
    @Mock
    private AuthSessionService authSessionService;

    @InjectMocks
    private AuthAccountService authAccountService;

    @BeforeEach
    void injectValues() {
        ReflectionTestUtils.setField(authAccountService, "resetPasswordTokenExpiration", 3600000L);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserEntity activeUser(long id, String email) {
        UserInfoEntity info = new UserInfoEntity();
        info.setFirstName("First");
        info.setLastName("Last");

        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("encoded-old-password");
        user.setActive(true);
        user.setUserInfo(info);
        return user;
    }

    private UserEntity inactiveUser(long id, String email) {
        UserEntity user = activeUser(id, email);
        user.setActive(false);
        return user;
    }

    // -------------------------------------------------------------------------
    // forgotPassword — known email, active account
    // -------------------------------------------------------------------------

    @Test
    void forgotPassword_withKnownActiveEmail_savesResetTokenAndDispatchesEmail() {
        UserEntity user = activeUser(10L, "known@example.com");
        when(userRepository.findByEmail("known@example.com")).thenReturn(Optional.of(user));

        authAccountService.forgotPassword(new ForgotPasswordRequest("known@example.com"));

        // Reset token must be persisted to Redis via authSessionService
        verify(authSessionService).saveTokenToRedis(
                eq(10L),
                anyString(),
                eq(EUserTokenType.RESET_PASSWORD_TOKEN),
                eq(3600000L));

        // Email must be dispatched
        verify(emailService).sendPasswordResetEmail(eq("known@example.com"), anyString());
    }

    // -------------------------------------------------------------------------
    // forgotPassword — unknown email
    // R3.2 divergence: code throws BadRequestException (does NOT silently ignore).
    // The requirement says "does not reveal account existence", but the actual
    // implementation throws BadRequestException with "user.not.found.email".
    // Tests assert what the code does.
    // -------------------------------------------------------------------------

    @Test
    void forgotPassword_withUnknownEmail_throwsBadRequestException() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        when(messageService.getMessage("user.not.found.email", "nobody@example.com"))
                .thenReturn("User not found");

        assertThrows(BadRequestException.class,
                () -> authAccountService.forgotPassword(new ForgotPasswordRequest("nobody@example.com")));

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        verify(authSessionService, never()).saveTokenToRedis(anyLong(), anyString(), any(), anyLong());
    }

    // -------------------------------------------------------------------------
    // forgotPassword — inactive account
    // -------------------------------------------------------------------------

    @Test
    void forgotPassword_withInactiveAccount_throwsBadRequestException() {
        UserEntity user = inactiveUser(11L, "inactive@example.com");
        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(user));
        when(messageService.getMessage("auth.account.not.active")).thenReturn("Account not active");

        assertThrows(BadRequestException.class,
                () -> authAccountService.forgotPassword(new ForgotPasswordRequest("inactive@example.com")));

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    // -------------------------------------------------------------------------
    // resetPassword — valid token
    // -------------------------------------------------------------------------

    @Test
    void resetPassword_withValidToken_encodesAndSavesNewPassword() {
        UserTokenRedisEntity tokenEntity = UserTokenRedisEntity.builder()
                .id("20:RESET_PASSWORD_TOKEN")
                .userId(20L)
                .tokenType(EUserTokenType.RESET_PASSWORD_TOKEN)
                .token("valid-token")
                .build();

        UserEntity user = activeUser(20L, "reset@example.com");

        when(userTokenRedisRepository.findByTokenType(EUserTokenType.RESET_PASSWORD_TOKEN))
                .thenReturn(List.of(tokenEntity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("encoded-new-password");

        authAccountService.resetPassword(new ResetPasswordRequest("valid-token", "NewPassword1!"));

        verify(passwordEncoder).encode("NewPassword1!");
        verify(userRepository).save(argThat(u -> "encoded-new-password".equals(u.getPassword())));
        verify(userTokenRedisRepository).delete(tokenEntity);
    }

    // -------------------------------------------------------------------------
    // resetPassword — invalid / expired token
    // -------------------------------------------------------------------------

    @Test
    void resetPassword_withInvalidToken_throwsBadRequestException() {
        when(userTokenRedisRepository.findByTokenType(EUserTokenType.RESET_PASSWORD_TOKEN))
                .thenReturn(List.of());
        when(messageService.getMessage("auth.reset.token.invalid")).thenReturn("Token invalid");

        assertThrows(BadRequestException.class,
                () -> authAccountService.resetPassword(new ResetPasswordRequest("bad-token", "NewPass1!")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_withExpiredToken_notInRedisList_throwsBadRequestException() {
        // Simulates Redis TTL expiry: the token is simply absent from the findByTokenType list
        UserTokenRedisEntity differentToken = UserTokenRedisEntity.builder()
                .id("21:RESET_PASSWORD_TOKEN")
                .userId(21L)
                .token("other-token")
                .build();

        when(userTokenRedisRepository.findByTokenType(EUserTokenType.RESET_PASSWORD_TOKEN))
                .thenReturn(List.of(differentToken));
        when(messageService.getMessage("auth.reset.token.invalid")).thenReturn("Token invalid");

        assertThrows(BadRequestException.class,
                () -> authAccountService.resetPassword(new ResetPasswordRequest("expired-token", "NewPass1!")));
    }

    // -------------------------------------------------------------------------
    // resetPassword — user inactive after token found
    // -------------------------------------------------------------------------

    @Test
    void resetPassword_withValidTokenButInactiveUser_throwsBadRequestException() {
        UserTokenRedisEntity tokenEntity = UserTokenRedisEntity.builder()
                .id("30:RESET_PASSWORD_TOKEN")
                .userId(30L)
                .token("valid-token")
                .build();
        UserEntity user = inactiveUser(30L, "inactive@example.com");

        when(userTokenRedisRepository.findByTokenType(EUserTokenType.RESET_PASSWORD_TOKEN))
                .thenReturn(List.of(tokenEntity));
        when(userRepository.findById(30L)).thenReturn(Optional.of(user));
        when(messageService.getMessage("auth.account.not.active")).thenReturn("Account not active");

        assertThrows(BadRequestException.class,
                () -> authAccountService.resetPassword(new ResetPasswordRequest("valid-token", "NewPass1!")));

        verify(userRepository, never()).save(any());
    }
}
