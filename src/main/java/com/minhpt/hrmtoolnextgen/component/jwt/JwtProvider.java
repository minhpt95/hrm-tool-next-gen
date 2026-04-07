package com.minhpt.hrmtoolnextgen.component.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.SecretKey;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.util.DateUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Generates and validates JWT tokens with built-in key-rotation support.
 *
 * <h3>Key rotation</h3>
 * <p>Two secrets are supported simultaneously:
 * <ul>
 *   <li><b>current</b> ({@code hrm.app.jwtSecret}) – used for all newly issued tokens.
 *       Its SHA-256 fingerprint (first 8 hex chars) is stored in the {@code kid} JWT header.</li>
 *   <li><b>previous</b> ({@code hrm.app.jwtSecretPrevious}, optional) – accepted for
 *       validation to allow in-flight tokens to keep working after a rotation.</li>
 * </ul>
 *
 * <h3>Rotating a secret</h3>
 * <ol>
 *   <li>Set {@code hrm.app.jwtSecretPrevious} to the current value of {@code hrm.app.jwtSecret}.</li>
 *   <li>Set {@code hrm.app.jwtSecret} to the new secret.</li>
 *   <li>Redeploy. Tokens signed with the old key remain valid until they expire naturally;
 *       new tokens are signed with the new key.</li>
 *   <li>Once all old tokens have expired you can clear {@code jwtSecretPrevious}.</li>
 * </ol>
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class JwtProvider {

    /** The currently active signing secret. Must never be empty. */
    @Value("${hrm.app.jwtSecret}")
    private String jwtSecret;

    /**
     * The previous signing secret kept during a rotation window.
     * May be empty/null – in that case only the current key is used for validation.
     */
    @Value("${hrm.app.jwtSecretPrevious:}")
    private String jwtSecretPrevious;

    @Value("${hrm.app.jwtExpiration}")
    private long jwtExpiration;

    @Value("${hrm.app.refreshTokenExpiration}")
    private long refreshTokenExpiration;

    // ------------------------------------------------------------------
    // Token generation
    // ------------------------------------------------------------------

    public String generateJwtToken(Authentication authentication) {
        return generateJwtToken((UserPrincipalDto) authentication.getPrincipal());
    }

    public String generateJwtToken(UserPrincipalDto userPrincipal) {
        return generateTokenFromEmail(userPrincipal.getEmail(), userPrincipal.getId());
    }

    public String generateTokenFromEmail(String email, Long id) {
        Instant expired = Instant.now().plusMillis(jwtExpiration);
        return Jwts.builder()
                .header().add("kid", fingerprint(jwtSecret)).and()
                .id(String.valueOf(id))
                .subject(email)
                .claim("token_type", "access")
                .issuedAt(DateUtils.convertInstantToDate(Instant.now()))
                .expiration(DateUtils.convertInstantToDate(expired))
                .signWith(buildKey(jwtSecret))
                .compact();
    }

    public String generateRefreshToken(String email, Long id) {
        Instant expired = Instant.now().plusMillis(refreshTokenExpiration);
        return Jwts.builder()
                .header().add("kid", fingerprint(jwtSecret)).and()
                .id(String.valueOf(id))
                .subject(email)
                .claim("token_type", "refresh")
                .issuedAt(DateUtils.convertInstantToDate(Instant.now()))
                .expiration(DateUtils.convertInstantToDate(expired))
                .signWith(buildKey(jwtSecret))
                .compact();
    }

    // ------------------------------------------------------------------
    // Token inspection
    // ------------------------------------------------------------------

    public Long getIdFromJwtToken(String token) {
        try {
            return Long.parseLong(getSignedClaims(token).getPayload().getId());
        } catch (Exception e) {
            log.error("Error getting ID from JWT token", e);
            return null;
        }
    }

    public String getTokenTypeFromJwtToken(String token) {
        try {
            Object tokenType = getSignedClaims(token).getPayload().get("token_type");
            return tokenType != null ? tokenType.toString() : null;
        } catch (Exception e) {
            log.error("Error getting token type from JWT token", e);
            return null;
        }
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getTokenTypeFromJwtToken(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenTypeFromJwtToken(token));
    }

    public String getEmailFromJwtToken(String token) {
        return getSignedClaims(token).getPayload().getSubject();
    }

    public Long getRemainTimeFromJwtToken(String token) {
        return getSignedClaims(token).getPayload().getExpiration().getTime()
                - DateUtils.getInstantLong();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            return getRemainTimeFromJwtToken(authToken) > 0;
        } catch (Exception e) {
            log.error("Error validateJwtToken -> Message: ", e);
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Parses and verifies a token.
     *
     * <p>Tries the current key first; if that fails and a previous key is
     * configured, retries with the previous key (rotation window support).
     */
    private Jws<Claims> getSignedClaims(String authToken) {
        try {
            return parse(authToken, buildKey(jwtSecret));
        } catch (JwtException primaryFail) {
            if (StringUtils.isNotBlank(jwtSecretPrevious)) {
                try {
                    log.debug("Token did not validate with current key; trying previous key");
                    return parse(authToken, buildKey(jwtSecretPrevious));
                } catch (JwtException secondaryFail) {
                    // throw the original failure for cleaner error messages
                    log.error("the original failure for cleaner error messages",secondaryFail);
                }
            }
            throw primaryFail;
        }
    }

    private Jws<Claims> parse(String token, SecretKey key) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    private SecretKey buildKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns the first 8 hex characters of the SHA-256 hash of the secret.
     * This is safe to embed in the public JWT header because it reveals
     * nothing about the actual secret value.
     */
    private static String fingerprint(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 8);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
