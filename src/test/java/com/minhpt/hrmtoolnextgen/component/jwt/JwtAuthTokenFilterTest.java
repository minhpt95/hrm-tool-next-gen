package com.minhpt.hrmtoolnextgen.component.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.entity.redis.UserTokenRedisEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserTokenType;
import com.minhpt.hrmtoolnextgen.repository.redis.UserTokenRedisRepository;
import com.minhpt.hrmtoolnextgen.service.security.UserDetailsServiceImpl;

import jakarta.servlet.FilterChain;

class JwtAuthTokenFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateWhenAccessTokenIsValidAndStored() throws Exception {
        JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);
        UserDetailsServiceImpl userDetailsService = Mockito.mock(UserDetailsServiceImpl.class);
        UserTokenRedisRepository tokenRepository = Mockito.mock(UserTokenRedisRepository.class);
        FilterChain filterChain = Mockito.mock(FilterChain.class);
        JwtAuthTokenFilter filter = new JwtAuthTokenFilter(jwtProvider, userDetailsService, tokenRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid-token");

        UserTokenRedisEntity storedToken = new UserTokenRedisEntity();
        storedToken.setToken("valid-token");

        UserPrincipalDto principal = UserPrincipalDto.internalBuilder()
                .id(1L)
                .email("user@example.com")
                .password("secret")
                .isEnabled(true)
                .roles(List.of("USER"))
                .authorities(List.of(new SimpleGrantedAuthority("USER")))
                .build();

        when(jwtProvider.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtProvider.isAccessToken("valid-token")).thenReturn(true);
        when(jwtProvider.getEmailFromJwtToken("valid-token")).thenReturn("user@example.com");
        when(jwtProvider.getIdFromJwtToken("valid-token")).thenReturn(1L);
        when(jwtProvider.getRemainTimeFromJwtToken("valid-token")).thenReturn(60_000L);
        when(tokenRepository.findUserByUserIdAndTokenType(1L, EUserTokenType.ACCESS_TOKEN)).thenReturn(storedToken);
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(principal);

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipAuthenticationForInvalidAuthorizationHeader() throws Exception {
        JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);
        UserDetailsServiceImpl userDetailsService = Mockito.mock(UserDetailsServiceImpl.class);
        UserTokenRedisRepository tokenRepository = Mockito.mock(UserTokenRedisRepository.class);
        FilterChain filterChain = Mockito.mock(FilterChain.class);
        JwtAuthTokenFilter filter = new JwtAuthTokenFilter(jwtProvider, userDetailsService, tokenRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Token invalid-format");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // Negative paths — filter must not authenticate and must continue the chain
    // -------------------------------------------------------------------------

    @Test
    void shouldSkipAuthenticationWhenAuthorizationHeaderIsAbsent() throws Exception {
        // getJwt() returns null for a blank/missing header → filter exits early via the
        // first null-check branch and delegates to the chain without authenticating.
        JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);
        UserDetailsServiceImpl userDetailsService = Mockito.mock(UserDetailsServiceImpl.class);
        UserTokenRedisRepository tokenRepository = Mockito.mock(UserTokenRedisRepository.class);
        FilterChain filterChain = Mockito.mock(FilterChain.class);
        JwtAuthTokenFilter filter = new JwtAuthTokenFilter(jwtProvider, userDetailsService, tokenRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // No Authorization header added — StringUtils.isBlank(null) → true → jwt == null

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipAuthenticationWhenTokenFailsValidation() throws Exception {
        // validateJwtToken returns false (expired, malformed signature, etc.) →
        // filter exits via the second early-return branch.
        JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);
        UserDetailsServiceImpl userDetailsService = Mockito.mock(UserDetailsServiceImpl.class);
        UserTokenRedisRepository tokenRepository = Mockito.mock(UserTokenRedisRepository.class);
        FilterChain filterChain = Mockito.mock(FilterChain.class);
        JwtAuthTokenFilter filter = new JwtAuthTokenFilter(jwtProvider, userDetailsService, tokenRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer expired-or-malformed-token");

        when(jwtProvider.validateJwtToken("expired-or-malformed-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipAuthenticationWhenTokenIsRefreshToken() throws Exception {
        // validateJwtToken returns true but isAccessToken returns false →
        // filter exits via the third early-return branch (refresh tokens must not grant access).
        JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);
        UserDetailsServiceImpl userDetailsService = Mockito.mock(UserDetailsServiceImpl.class);
        UserTokenRedisRepository tokenRepository = Mockito.mock(UserTokenRedisRepository.class);
        FilterChain filterChain = Mockito.mock(FilterChain.class);
        JwtAuthTokenFilter filter = new JwtAuthTokenFilter(jwtProvider, userDetailsService, tokenRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer refresh-token");

        when(jwtProvider.validateJwtToken("refresh-token")).thenReturn(true);
        when(jwtProvider.isAccessToken("refresh-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipAuthenticationWhenTokenNotFoundInRedis() throws Exception {
        // Token is structurally valid and passes validateJwtToken + isAccessToken,
        // but Redis has no stored entry for this user → storedToken == null →
        // filter exits via the fourth early-return branch (token was invalidated / logout).
        JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);
        UserDetailsServiceImpl userDetailsService = Mockito.mock(UserDetailsServiceImpl.class);
        UserTokenRedisRepository tokenRepository = Mockito.mock(UserTokenRedisRepository.class);
        FilterChain filterChain = Mockito.mock(FilterChain.class);
        JwtAuthTokenFilter filter = new JwtAuthTokenFilter(jwtProvider, userDetailsService, tokenRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid-but-invalidated-token");

        when(jwtProvider.validateJwtToken("valid-but-invalidated-token")).thenReturn(true);
        when(jwtProvider.isAccessToken("valid-but-invalidated-token")).thenReturn(true);
        when(jwtProvider.getEmailFromJwtToken("valid-but-invalidated-token")).thenReturn("user@example.com");
        when(jwtProvider.getIdFromJwtToken("valid-but-invalidated-token")).thenReturn(42L);
        // Redis returns null → token was revoked (e.g. user logged out)
        when(tokenRepository.findUserByUserIdAndTokenType(42L, EUserTokenType.ACCESS_TOKEN)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipAuthenticationWhenRedisTokenDoesNotMatchBearer() throws Exception {
        // Redis has a stored token for this user but it does not match the presented token
        // (e.g. user logged in again and got a new token; old token is no longer valid).
        // storedToken != null but storedToken.getToken().equals(jwt) is false → no auth.
        JwtProvider jwtProvider = Mockito.mock(JwtProvider.class);
        UserDetailsServiceImpl userDetailsService = Mockito.mock(UserDetailsServiceImpl.class);
        UserTokenRedisRepository tokenRepository = Mockito.mock(UserTokenRedisRepository.class);
        FilterChain filterChain = Mockito.mock(FilterChain.class);
        JwtAuthTokenFilter filter = new JwtAuthTokenFilter(jwtProvider, userDetailsService, tokenRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer stale-token");

        UserTokenRedisEntity storedToken = new UserTokenRedisEntity();
        storedToken.setToken("current-token"); // different from "stale-token"

        when(jwtProvider.validateJwtToken("stale-token")).thenReturn(true);
        when(jwtProvider.isAccessToken("stale-token")).thenReturn(true);
        when(jwtProvider.getEmailFromJwtToken("stale-token")).thenReturn("user@example.com");
        when(jwtProvider.getIdFromJwtToken("stale-token")).thenReturn(7L);
        when(tokenRepository.findUserByUserIdAndTokenType(7L, EUserTokenType.ACCESS_TOKEN)).thenReturn(storedToken);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}