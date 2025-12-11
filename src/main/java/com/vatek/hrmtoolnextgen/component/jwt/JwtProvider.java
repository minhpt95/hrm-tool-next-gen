package com.vatek.hrmtoolnextgen.component.jwt;

import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.repository.redis.UserTokenRedisRepository;
import com.vatek.hrmtoolnextgen.util.DateUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.time.Instant;

@Component
@Log4j2
@RequiredArgsConstructor
public class JwtProvider {

    @Value("${hrm.app.jwtSecret}")
    private String jwtSecret;

    @Value("${hrm.app.jwtExpiration}")
    private long jwtExpiration;

    @Value("${hrm.app.refreshTokenExpiration}")
    private long refreshTokenExpiration;

    public String generateJwtToken(Authentication authentication) {
        UserPrincipalDto userPrincipal = (UserPrincipalDto) authentication.getPrincipal();
        return generateJwtToken(userPrincipal);
    }

    public String generateJwtToken(UserPrincipalDto userPrincipal) {
        return generateTokenFromEmail(userPrincipal.getEmail(),userPrincipal.getId());
    }

    public String generateTokenFromEmail(String email, Long id) {
        Instant expired = Instant.now().plusMillis(jwtExpiration);

        return Jwts
                .builder()
                .id(String.valueOf(id))
                .subject(email)
                .claim("token_type", "access")
                .issuedAt(DateUtils.convertInstantToDate(Instant.now()))
                .expiration(DateUtils.convertInstantToDate(expired))
                .signWith(getSecretKey())
                .compact();
    }

    public String generateRefreshToken(String email, Long id) {
        Instant expired = Instant.now().plusMillis(refreshTokenExpiration);

        return Jwts
                .builder()
                .id(String.valueOf(id))
                .subject(email)
                .claim("token_type", "refresh")
                .issuedAt(DateUtils.convertInstantToDate(Instant.now()))
                .expiration(DateUtils.convertInstantToDate(expired))
                .signWith(getSecretKey())
                .compact();
    }

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
        return getSignedClaims(token)
                .getPayload()
                .getExpiration()
                .getTime() - DateUtils.getInstantLong();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            return getRemainTimeFromJwtToken(authToken) > 0;
        } catch (Exception e) {
            log.error("Error validateJwtToken -> Message : ",e);
        }
        return false;
    }

    private Jws<Claims> getSignedClaims(String authToken) {
        return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(authToken);
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
