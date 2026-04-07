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
}