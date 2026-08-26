package com.minhpt.hrmtoolnextgen.component.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;

import io.jsonwebtoken.JwtException;

/**
 * Unit tests for {@link JwtProvider} — issuing, inspecting and validating tokens,
 * plus the two-key rotation window.
 *
 * <p>The {@code @Value} fields are populated with ReflectionTestUtils; secrets are at
 * least 32 bytes so HS256 key construction succeeds.
 */
class JwtProviderTest {

    private static final String CURRENT_SECRET = "current-secret-key-that-is-long-enough-for-hs256";
    private static final String PREVIOUS_SECRET = "previous-secret-key-that-is-long-enough-for-hs256";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtProvider provider;

    @BeforeEach
    void setUp() {
        provider = newProvider(CURRENT_SECRET, "", ONE_HOUR_MS, ONE_HOUR_MS * 24);
    }

    private JwtProvider newProvider(String secret, String previous, long expiry, long refreshExpiry) {
        JwtProvider p = new JwtProvider();
        ReflectionTestUtils.setField(p, "jwtSecret", secret);
        ReflectionTestUtils.setField(p, "jwtSecretPrevious", previous);
        ReflectionTestUtils.setField(p, "jwtExpiration", expiry);
        ReflectionTestUtils.setField(p, "refreshTokenExpiration", refreshExpiry);
        return p;
    }

    private UserPrincipalDto principal(long id, String email) {
        return UserPrincipalDto.internalBuilder()
                .id(id)
                .email(email)
                .authorities(List.of(EUserRole.USER))
                .build();
    }

    /** Decodes the JOSE header of a compact JWT without verifying it. */
    private Map<?, ?> decodeHeader(String token) throws Exception {
        String headerJson = new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
        return new ObjectMapper().readValue(headerJson, Map.class);
    }

    // -------------------------------------------------------------------------
    // Generation
    // -------------------------------------------------------------------------

    @Test
    void generateTokenFromEmail_embedsSubjectAndId() {
        String token = provider.generateTokenFromEmail("user@example.com", 42L);

        assertNotNull(token);
        assertEquals("user@example.com", provider.getEmailFromJwtToken(token));
        assertEquals(42L, provider.getIdFromJwtToken(token));
    }

    @Test
    void generateJwtToken_fromPrincipal_matchesEmailAndId() {
        String token = provider.generateJwtToken(principal(7L, "principal@example.com"));

        assertEquals("principal@example.com", provider.getEmailFromJwtToken(token));
        assertEquals(7L, provider.getIdFromJwtToken(token));
    }

    @Test
    void generateJwtToken_fromAuthentication_readsThePrincipal() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal(9L, "auth@example.com"));

        String token = provider.generateJwtToken(authentication);

        assertEquals("auth@example.com", provider.getEmailFromJwtToken(token));
        assertEquals(9L, provider.getIdFromJwtToken(token));
    }

    @Test
    void generatedTokenCarriesKeyFingerprintInKidHeader() throws Exception {
        String token = provider.generateTokenFromEmail("kid@example.com", 1L);

        Object kid = decodeHeader(token).get("kid");

        assertNotNull(kid);
        assertEquals(8, kid.toString().length(), "kid is the first 8 hex chars of the SHA-256 digest");
        assertTrue(kid.toString().matches("[0-9a-f]{8}"));
    }

    @Test
    void differentSecrets_produceDifferentKidFingerprints() throws Exception {
        JwtProvider other = newProvider(PREVIOUS_SECRET, "", ONE_HOUR_MS, ONE_HOUR_MS);

        assertNotEquals(
                decodeHeader(provider.generateTokenFromEmail("a@example.com", 1L)).get("kid"),
                decodeHeader(other.generateTokenFromEmail("a@example.com", 1L)).get("kid"));
    }

    // -------------------------------------------------------------------------
    // Token type
    // -------------------------------------------------------------------------

    @Test
    void accessToken_isTypedAccess() {
        String token = provider.generateTokenFromEmail("a@example.com", 1L);

        assertEquals("access", provider.getTokenTypeFromJwtToken(token));
        assertTrue(provider.isAccessToken(token));
        assertFalse(provider.isRefreshToken(token));
    }

    @Test
    void refreshToken_isTypedRefresh() {
        String token = provider.generateRefreshToken("a@example.com", 1L);

        assertEquals("refresh", provider.getTokenTypeFromJwtToken(token));
        assertTrue(provider.isRefreshToken(token));
        assertFalse(provider.isAccessToken(token));
    }

    @Test
    void refreshToken_carriesSubjectAndId() {
        String token = provider.generateRefreshToken("refresh@example.com", 55L);

        assertEquals("refresh@example.com", provider.getEmailFromJwtToken(token));
        assertEquals(55L, provider.getIdFromJwtToken(token));
    }

    @Test
    void tokenTypeAndIdAccessors_swallowErrorsOnGarbage() {
        assertNull(provider.getTokenTypeFromJwtToken("not.a.jwt"));
        assertNull(provider.getIdFromJwtToken("not.a.jwt"));
        assertFalse(provider.isAccessToken("not.a.jwt"));
        assertFalse(provider.isRefreshToken("not.a.jwt"));
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    @Test
    void validateJwtToken_acceptsAFreshToken() {
        assertTrue(provider.validateJwtToken(provider.generateTokenFromEmail("a@example.com", 1L)));
    }

    @Test
    void validateJwtToken_rejectsMalformedInput() {
        assertFalse(provider.validateJwtToken("garbage"));
        assertFalse(provider.validateJwtToken(""));
    }

    @Test
    void validateJwtToken_rejectsATokenSignedWithAnotherSecret() {
        String foreign = newProvider(PREVIOUS_SECRET, "", ONE_HOUR_MS, ONE_HOUR_MS)
                .generateTokenFromEmail("a@example.com", 1L);

        assertFalse(provider.validateJwtToken(foreign));
    }

    @Test
    void validateJwtToken_rejectsAnExpiredToken() {
        // Negative expiry produces a token whose exp is already in the past.
        JwtProvider expiring = newProvider(CURRENT_SECRET, "", -1000L, -1000L);
        String expired = expiring.generateTokenFromEmail("a@example.com", 1L);

        assertFalse(expiring.validateJwtToken(expired));
    }

    @Test
    void getRemainTimeFromJwtToken_isPositiveAndWithinTheConfiguredWindow() {
        String token = provider.generateTokenFromEmail("a@example.com", 1L);

        Long remaining = provider.getRemainTimeFromJwtToken(token);

        assertTrue(remaining > 0);
        assertTrue(remaining <= ONE_HOUR_MS);
    }

    @Test
    void getEmailFromJwtToken_propagatesFailureOnGarbage() {
        // Unlike the id/type accessors, this one does not swallow parse failures.
        assertThrows(JwtException.class, () -> provider.getEmailFromJwtToken("not.a.jwt"));
    }

    // -------------------------------------------------------------------------
    // Key rotation window
    // -------------------------------------------------------------------------

    @Test
    void tokenSignedWithPreviousSecret_isStillAcceptedDuringRotation() {
        String oldToken = newProvider(PREVIOUS_SECRET, "", ONE_HOUR_MS, ONE_HOUR_MS)
                .generateTokenFromEmail("rotating@example.com", 3L);

        JwtProvider rotating = newProvider(CURRENT_SECRET, PREVIOUS_SECRET, ONE_HOUR_MS, ONE_HOUR_MS);

        assertTrue(rotating.validateJwtToken(oldToken));
        assertEquals("rotating@example.com", rotating.getEmailFromJwtToken(oldToken));
        assertEquals(3L, rotating.getIdFromJwtToken(oldToken));
    }

    @Test
    void tokenSignedWithCurrentSecret_isAcceptedWhilePreviousIsConfigured() {
        JwtProvider rotating = newProvider(CURRENT_SECRET, PREVIOUS_SECRET, ONE_HOUR_MS, ONE_HOUR_MS);
        String token = rotating.generateTokenFromEmail("current@example.com", 4L);

        assertTrue(rotating.validateJwtToken(token));
        assertEquals("current@example.com", rotating.getEmailFromJwtToken(token));
    }

    @Test
    void tokenSignedWithAThirdSecret_isRejectedEvenDuringRotation() {
        String foreign = newProvider("a-completely-unrelated-secret-key-32-bytes+", "",
                ONE_HOUR_MS, ONE_HOUR_MS).generateTokenFromEmail("x@example.com", 1L);

        JwtProvider rotating = newProvider(CURRENT_SECRET, PREVIOUS_SECRET, ONE_HOUR_MS, ONE_HOUR_MS);

        assertFalse(rotating.validateJwtToken(foreign));
    }

    @Test
    void withoutPreviousSecret_anOldTokenIsRejected() {
        String oldToken = newProvider(PREVIOUS_SECRET, "", ONE_HOUR_MS, ONE_HOUR_MS)
                .generateTokenFromEmail("old@example.com", 1L);

        // provider has a blank jwtSecretPrevious
        assertFalse(provider.validateJwtToken(oldToken));
    }

    @Test
    void newlyIssuedTokensAlwaysUseTheCurrentKey() throws Exception {
        JwtProvider rotating = newProvider(CURRENT_SECRET, PREVIOUS_SECRET, ONE_HOUR_MS, ONE_HOUR_MS);
        JwtProvider currentOnly = newProvider(CURRENT_SECRET, "", ONE_HOUR_MS, ONE_HOUR_MS);

        assertEquals(
                decodeHeader(currentOnly.generateTokenFromEmail("a@example.com", 1L)).get("kid"),
                decodeHeader(rotating.generateTokenFromEmail("a@example.com", 1L)).get("kid"));
    }
}
