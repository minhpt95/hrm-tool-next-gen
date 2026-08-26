package com.minhpt.hrmtoolnextgen.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.component.jwt.JwtProvider;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.LoginRequest;
import com.minhpt.hrmtoolnextgen.dto.request.RefreshTokenRequest;
import com.minhpt.hrmtoolnextgen.dto.response.LoginResponse;
import com.minhpt.hrmtoolnextgen.dto.response.RefreshTokenResponse;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.entity.redis.UserTokenRedisEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserTokenType;
import com.minhpt.hrmtoolnextgen.exception.UnauthorizedException;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.repository.redis.UserTokenRedisRepository;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private UserTokenRedisRepository userTokenRedisRepository;
    @Mock
    private MessageService messageService;

    @InjectMocks
    private AuthSessionService authSessionService;

    @BeforeEach
    void injectValues() {
        ReflectionTestUtils.setField(authSessionService, "jwtExpiration", 3600000L);
        ReflectionTestUtils.setField(authSessionService, "refreshTokenExpiration", 3600000L);
    }

    // -------------------------------------------------------------------------
    // login — success
    // -------------------------------------------------------------------------

    @Test
    void login_withValidCredentials_returnsTokensAndPersistsBothToRedis() {
        UserPrincipalDto principal = UserPrincipalDto.internalBuilder()
                .id(1L)
                .email("user@example.com")
                .firstName("First")
                .lastName("Last")
                .roles(List.of("USER"))
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtProvider.generateJwtToken(auth)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken("user@example.com", 1L)).thenReturn("refresh-token");

        LoginResponse response = authSessionService.login(new LoginRequest("user@example.com", "secret"));

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("user@example.com", response.getEmail());
        assertEquals(1L, response.getId());

        // Both token types must be persisted to Redis
        verify(userTokenRedisRepository).save(argThat(e ->
                e.getUserId().equals(1L) && e.getTokenType() == EUserTokenType.ACCESS_TOKEN
                        && "access-token".equals(e.getToken())));
        verify(userTokenRedisRepository).save(argThat(e ->
                e.getUserId().equals(1L) && e.getTokenType() == EUserTokenType.REFRESH_TOKEN
                        && "refresh-token".equals(e.getToken())));
    }

    // -------------------------------------------------------------------------
    // login — bad credentials
    // -------------------------------------------------------------------------

    @Test
    void login_withBadCredentials_throwsUnauthorizedException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad creds"));
        when(messageService.getMessage("auth.login.invalid")).thenReturn("Invalid credentials");

        assertThrows(UnauthorizedException.class,
                () -> authSessionService.login(new LoginRequest("user@example.com", "wrong")));

        verify(userTokenRedisRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // saveTokenToRedis — correct composite key shape
    // -------------------------------------------------------------------------

    @Test
    void saveTokenToRedis_buildsCompositeKeyAndDelegatestoRepository() {
        authSessionService.saveTokenToRedis(42L, "tok", EUserTokenType.ACCESS_TOKEN, 3600000L);

        verify(userTokenRedisRepository).save(argThat(e ->
                "42:ACCESS_TOKEN".equals(e.getId())
                        && e.getUserId().equals(42L)
                        && e.getTokenType() == EUserTokenType.ACCESS_TOKEN
                        && "tok".equals(e.getToken())
                        && e.getTtl() == 3600000L));
    }

    // -------------------------------------------------------------------------
    // invalidateUserTokens / logout — removes both token types
    // -------------------------------------------------------------------------

    @Test
    void invalidateUserTokens_deletesBothAccessAndRefreshEntities() {
        UserTokenRedisEntity accessEntity = UserTokenRedisEntity.builder()
                .id("1:ACCESS_TOKEN").userId(1L).tokenType(EUserTokenType.ACCESS_TOKEN).token("a").build();
        UserTokenRedisEntity refreshEntity = UserTokenRedisEntity.builder()
                .id("1:REFRESH_TOKEN").userId(1L).tokenType(EUserTokenType.REFRESH_TOKEN).token("r").build();

        when(userTokenRedisRepository.findUserByUserIdAndTokenType(1L, EUserTokenType.ACCESS_TOKEN))
                .thenReturn(accessEntity);
        when(userTokenRedisRepository.findUserByUserIdAndTokenType(1L, EUserTokenType.REFRESH_TOKEN))
                .thenReturn(refreshEntity);

        authSessionService.invalidateUserTokens(1L);

        verify(userTokenRedisRepository).delete(accessEntity);
        verify(userTokenRedisRepository).delete(refreshEntity);
    }

    @Test
    void logout_delegatesToInvalidateUserTokens() {
        UserPrincipalDto principal = UserPrincipalDto.internalBuilder()
                .id(5L).email("e@example.com").build();

        when(userTokenRedisRepository.findUserByUserIdAndTokenType(5L, EUserTokenType.ACCESS_TOKEN))
                .thenReturn(null);
        when(userTokenRedisRepository.findUserByUserIdAndTokenType(5L, EUserTokenType.REFRESH_TOKEN))
                .thenReturn(null);

        authSessionService.logout(principal);

        verify(userTokenRedisRepository, times(2)).findUserByUserIdAndTokenType(any(), any());
        verify(userTokenRedisRepository, never()).delete(any(UserTokenRedisEntity.class));
    }

    @Test
    void logout_withNullPrincipal_doesNothing() {
        authSessionService.logout(null);
        verify(userTokenRedisRepository, never()).findUserByUserIdAndTokenType(any(), any());
    }

    // -------------------------------------------------------------------------
    // invalidateUserTokens — tokens absent (null from repo) skips delete
    // -------------------------------------------------------------------------

    @Test
    void invalidateUserTokens_whenTokensAbsent_skipsDelete() {
        when(userTokenRedisRepository.findUserByUserIdAndTokenType(any(), any())).thenReturn(null);

        authSessionService.invalidateUserTokens(99L);

        verify(userTokenRedisRepository, never()).delete(any(UserTokenRedisEntity.class));
    }

    // -------------------------------------------------------------------------
    // refreshToken — invalid / non-refresh token rejected
    // -------------------------------------------------------------------------

    @Test
    void refreshToken_withInvalidToken_throwsUnauthorizedException() {
        when(jwtProvider.validateJwtToken("bad")).thenReturn(false);
        when(messageService.getMessage("auth.refresh.token.invalid")).thenReturn("Invalid refresh token");

        assertThrows(UnauthorizedException.class,
                () -> authSessionService.refreshToken(new RefreshTokenRequest("bad")));
    }

    @Test
    void refreshToken_withAccessTokenPassedAsRefresh_throwsUnauthorizedException() {
        when(jwtProvider.validateJwtToken("access-tok")).thenReturn(true);
        when(jwtProvider.isRefreshToken("access-tok")).thenReturn(false);
        when(messageService.getMessage("auth.refresh.token.not.refresh")).thenReturn("Not a refresh token");

        assertThrows(UnauthorizedException.class,
                () -> authSessionService.refreshToken(new RefreshTokenRequest("access-tok")));
    }

    @Test
    void refreshToken_whenRedisEntryMissing_throwsUnauthorizedException() {
        when(jwtProvider.validateJwtToken("refresh-tok")).thenReturn(true);
        when(jwtProvider.isRefreshToken("refresh-tok")).thenReturn(true);
        when(jwtProvider.getIdFromJwtToken("refresh-tok")).thenReturn(7L);
        when(userTokenRedisRepository.findUserByUserIdAndTokenType(7L, EUserTokenType.REFRESH_TOKEN))
                .thenReturn(null);
        when(messageService.getMessage("auth.refresh.token.not.found")).thenReturn("Token not found");

        assertThrows(UnauthorizedException.class,
                () -> authSessionService.refreshToken(new RefreshTokenRequest("refresh-tok")));
    }

    @Test
    void refreshToken_whenUserNotFoundInDb_throwsUnauthorizedException() {
        String tok = "refresh-tok";
        UserTokenRedisEntity stored = UserTokenRedisEntity.builder()
                .id("7:REFRESH_TOKEN").userId(7L).token(tok).build();

        when(jwtProvider.validateJwtToken(tok)).thenReturn(true);
        when(jwtProvider.isRefreshToken(tok)).thenReturn(true);
        when(jwtProvider.getIdFromJwtToken(tok)).thenReturn(7L);
        when(userTokenRedisRepository.findUserByUserIdAndTokenType(7L, EUserTokenType.REFRESH_TOKEN))
                .thenReturn(stored);
        when(jwtProvider.getEmailFromJwtToken(tok)).thenReturn("gone@example.com");
        when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.empty());
        when(messageService.getMessage("auth.user.not.found")).thenReturn("User not found");

        assertThrows(UnauthorizedException.class,
                () -> authSessionService.refreshToken(new RefreshTokenRequest(tok)));
    }

    @Test
    void refreshToken_withValidToken_returnsNewTokensAndRotatesRedisEntries() {
        String oldRefreshTok = "old-refresh";
        UserTokenRedisEntity stored = UserTokenRedisEntity.builder()
                .id("3:REFRESH_TOKEN").userId(3L).token(oldRefreshTok).build();
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("real@example.com");

        when(jwtProvider.validateJwtToken(oldRefreshTok)).thenReturn(true);
        when(jwtProvider.isRefreshToken(oldRefreshTok)).thenReturn(true);
        when(jwtProvider.getIdFromJwtToken(oldRefreshTok)).thenReturn(3L);
        when(userTokenRedisRepository.findUserByUserIdAndTokenType(3L, EUserTokenType.REFRESH_TOKEN))
                .thenReturn(stored);
        when(jwtProvider.getEmailFromJwtToken(oldRefreshTok)).thenReturn("real@example.com");
        when(userRepository.findByEmail("real@example.com")).thenReturn(Optional.of(userEntity));
        when(jwtProvider.generateTokenFromEmail("real@example.com", 3L)).thenReturn("new-access");
        when(jwtProvider.generateRefreshToken("real@example.com", 3L)).thenReturn("new-refresh");
        // old tokens: REFRESH_TOKEN entry is present (returns stored); ACCESS_TOKEN is absent (skips delete)
        when(userTokenRedisRepository.findUserByUserIdAndTokenType(3L, EUserTokenType.ACCESS_TOKEN))
                .thenReturn(null);

        RefreshTokenResponse response = authSessionService.refreshToken(new RefreshTokenRequest(oldRefreshTok));

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals("Bearer", response.getType());

        // Old refresh entry must be deleted (rotation)
        verify(userTokenRedisRepository).delete(stored);

        // New tokens saved to Redis
        verify(userTokenRedisRepository).save(argThat(e ->
                e.getTokenType() == EUserTokenType.ACCESS_TOKEN && "new-access".equals(e.getToken())));
        verify(userTokenRedisRepository).save(argThat(e ->
                e.getTokenType() == EUserTokenType.REFRESH_TOKEN && "new-refresh".equals(e.getToken())));
    }
}
