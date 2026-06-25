package com.minhpt.hrmtoolnextgen.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.minhpt.hrmtoolnextgen.HrmToolNextGenApplication;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;

/**
 * Tests for {@link JpaAuditingConfig}'s SpringSecurityAuditorAware logic. _R18.1, R18.2_
 *
 * <p>The inner class SpringSecurityAuditorAware is private; the bean is accessed via
 * the {@code AuditorAware<Long>} bean registered under name "auditorProvider" so no
 * reflection is needed. SecurityContextHolder is cleared in @AfterEach.
 */
@SpringBootTest(classes = {HrmToolNextGenApplication.class, AuditorAwareTest.MailTestConfig.class})
class AuditorAwareTest {

    @SuppressWarnings("unused")
    @TestConfiguration
    static class MailTestConfig {
        @SuppressWarnings("unused")
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }

    /** Injected from the application context — registered by JpaAuditingConfig.auditorProvider(). */
    @Autowired
    private AuditorAware<Long> auditorProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // R18.2 — no authentication → 0L
    // -------------------------------------------------------------------------

    /**
     * When SecurityContextHolder holds no Authentication at all,
     * getCurrentAuditor() must return Optional.of(0L). _R18.2_
     */
    @Test
    void noAuthentication_returnsZero() {
        // SecurityContextHolder is empty (cleared by @AfterEach of previous test or fresh).
        SecurityContextHolder.clearContext();

        Optional<Long> auditor = auditorProvider.getCurrentAuditor();

        assertTrue(auditor.isPresent(), "getCurrentAuditor() must never be empty");
        assertEquals(0L, auditor.get(),
                "Expected 0L (system) when no authentication is present");
    }

    // -------------------------------------------------------------------------
    // R18.1 — valid UserPrincipalDto with id → returns userId
    // -------------------------------------------------------------------------

    /**
     * When the SecurityContext holds a fully authenticated UserPrincipalDto with a
     * non-null id, getCurrentAuditor() must return Optional.of(userId). _R18.1_
     */
    @Test
    void validUserPrincipalWithId_returnsUserId() {
        UserPrincipalDto principal = UserPrincipalDto.internalBuilder()
                .id(42L)
                .email("auditor@example.com")
                .password("secret")
                .isEnabled(true)
                .roles(List.of("USER"))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Long> auditor = auditorProvider.getCurrentAuditor();

        assertTrue(auditor.isPresent(), "getCurrentAuditor() must never be empty");
        assertEquals(42L, auditor.get(),
                "Expected userId 42 from authenticated UserPrincipalDto");
    }

    // -------------------------------------------------------------------------
    // R18.2 — principal is NOT a UserPrincipalDto → 0L
    // -------------------------------------------------------------------------

    /**
     * When the principal is a plain String ("anonymous") rather than a
     * UserPrincipalDto, getCurrentAuditor() falls through to the default
     * and must return Optional.of(0L). _R18.2_
     */
    @Test
    void nonUserPrincipalDtoPrincipal_returnsZero() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "anonymous",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Long> auditor = auditorProvider.getCurrentAuditor();

        assertTrue(auditor.isPresent(), "getCurrentAuditor() must never be empty");
        assertEquals(0L, auditor.get(),
                "Expected 0L when principal is not a UserPrincipalDto");
    }

    // -------------------------------------------------------------------------
    // R18.2 — UserPrincipalDto with null id → 0L
    // -------------------------------------------------------------------------

    /**
     * When the principal IS a UserPrincipalDto but its id is null,
     * getCurrentAuditor() must return Optional.of(0L). _R18.2_
     */
    @Test
    void userPrincipalWithNullId_returnsZero() {
        UserPrincipalDto principal = UserPrincipalDto.internalBuilder()
                .id(null)
                .email("no-id@example.com")
                .password("secret")
                .isEnabled(true)
                .roles(List.of("USER"))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Long> auditor = auditorProvider.getCurrentAuditor();

        assertTrue(auditor.isPresent(), "getCurrentAuditor() must never be empty");
        assertEquals(0L, auditor.get(),
                "Expected 0L when UserPrincipalDto has null id");
    }

    // -------------------------------------------------------------------------
    // R18.2 — authentication present but isAuthenticated() = false → 0L
    // -------------------------------------------------------------------------

    /**
     * When an Authentication object is present but isAuthenticated() returns false
     * (explicitly unauthenticated token), getCurrentAuditor() must return 0L. _R18.2_
     */
    @Test
    void unauthenticatedAuthenticationObject_returnsZero() {
        // AnonymousAuthenticationToken always returns isAuthenticated()=true; use a
        // UsernamePasswordAuthenticationToken and call setAuthenticated(false) instead.
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("anonymous", null);
        auth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Long> auditor = auditorProvider.getCurrentAuditor();

        assertTrue(auditor.isPresent(), "getCurrentAuditor() must never be empty");
        assertEquals(0L, auditor.get(),
                "Expected 0L when authentication is present but isAuthenticated() = false");
    }
}
