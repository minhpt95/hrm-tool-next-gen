package com.minhpt.hrmtoolnextgen.component;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.minhpt.hrmtoolnextgen.annotation.RateLimit;
import com.minhpt.hrmtoolnextgen.annotation.RateLimit.RateLimitStrategy;
import com.minhpt.hrmtoolnextgen.exception.RateLimitException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * AOP aspect that enforces rate limits declared via {@link RateLimit}.
 *
 * <p>Rate limit subjects:
 * <ul>
 *   <li>{@code IP}     – keyed by the client's effective IP address</li>
 *   <li>{@code USER}   – keyed by the authenticated user's name</li>
 *   <li>{@code GLOBAL} – single shared bucket for the annotated method</li>
 * </ul>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Log4j2
public class RateLimitingAspect {

    private final TokenBucketRateLimiter rateLimiter;
    private final MessageService messageService;

    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String identifier = resolveIdentifier(rateLimit.strategy(), pjp);
        String redisKey   = rateLimit.keyPrefix() + ":" + identifier;

        if (!rateLimiter.tryConsume(redisKey, rateLimit.capacity(), rateLimit.refillRate())) {
            log.warn("Rate limit exceeded – key: {}, capacity: {}, refillRate: {}/min",
                    redisKey, rateLimit.capacity(), rateLimit.refillRate());
            throw new RateLimitException(messageService.getMessage("rate.limit.exceeded"));
        }

        return pjp.proceed();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String resolveIdentifier(RateLimitStrategy strategy, ProceedingJoinPoint pjp) {
        return switch (strategy) {
            case IP     -> extractClientIp();
            case USER   -> extractUsername();
            case GLOBAL -> pjp.getSignature().toShortString();
        };
    }

    /**
     * Extracts the real client IP, respecting common reverse-proxy headers.
     * Only the first (left-most) address in X-Forwarded-For is used to
     * avoid header injection by intermediate proxies.
     */
    private String extractClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();

            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.isNotBlank(xForwardedFor)) {
                // Take only the first IP – the originating client
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (StringUtils.isNotBlank(xRealIp)) {
                return xRealIp.trim();
            }

            return request.getRemoteAddr();
        } catch (Exception e) {
            log.warn("Cannot determine client IP; falling back to 'unknown': {}", e.getMessage());
            return "unknown";
        }
    }

    private String extractUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
                return auth.getName();
            }
        } catch (Exception e) {
            log.warn("Cannot determine authenticated user; falling back to IP: {}", e.getMessage());
        }
        return extractClientIp();
    }
}
