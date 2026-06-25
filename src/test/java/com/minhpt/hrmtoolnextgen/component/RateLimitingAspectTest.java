package com.minhpt.hrmtoolnextgen.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.minhpt.hrmtoolnextgen.annotation.RateLimit;
import com.minhpt.hrmtoolnextgen.exception.RateLimitException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class RateLimitingAspectTest {

    private final TokenBucketRateLimiter rateLimiter = Mockito.mock(TokenBucketRateLimiter.class);
    private final MessageService messageService = Mockito.mock(MessageService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private RateLimitingAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitingAspect(rateLimiter, messageService, meterRegistry);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldProceedWhenRateLimitAllowsRequest() throws Throwable {
        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        RateLimit rateLimit = createRateLimit("ratelimit:login", 10, 10, RateLimit.RateLimitStrategy.IP);

        when(signature.toShortString()).thenReturn("AuthController.login(..)");
        when(pjp.getSignature()).thenReturn(signature);
        when(rateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");

        Object result = aspect.enforce(pjp, rateLimit);

        assertEquals("ok", result);
        verify(pjp).proceed();
    }

    @Test
    void shouldThrowLocalizedRateLimitExceptionAndIncrementMetric() throws Throwable {
        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        RateLimit rateLimit = createRateLimit("ratelimit:login", 10, 10, RateLimit.RateLimitStrategy.GLOBAL);

        when(signature.toShortString()).thenReturn("AuthController.login(..)");
        when(signature.getName()).thenReturn("login");
        when(pjp.getSignature()).thenReturn(signature);
        when(rateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(messageService.getMessage("rate.limit.exceeded")).thenReturn("localized limit message");

        RateLimitException exception = assertThrows(RateLimitException.class, () -> aspect.enforce(pjp, rateLimit));

        assertEquals("localized limit message", exception.getMessage());
        Counter counter = meterRegistry.find("hrm.rate_limit.violations")
                .tag("key_prefix", "ratelimit:login")
                .tag("strategy", "GLOBAL")
                .tag("method", "login")
                .counter();
        assertEquals(1.0, counter == null ? null : counter.count());
    }

    // -----------------------------------------------------------------------
    // 4.1 — key composition per strategy (R5.3)
    // -----------------------------------------------------------------------

    @Test
    void ipStrategyKeyShouldIncorporateClientRemoteAddr() throws Throwable {
        // Arrange: bind a mock HTTP request so extractClientIp() can read remoteAddr
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("192.168.1.42");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));

        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        RateLimit rateLimit = createRateLimit("ratelimit:login", 10, 10, RateLimit.RateLimitStrategy.IP);

        when(signature.toShortString()).thenReturn("AuthController.login(..)");
        when(pjp.getSignature()).thenReturn(signature);
        when(rateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");

        // Act
        aspect.enforce(pjp, rateLimit);

        // Assert: key passed to tryConsume encodes the client IP
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimiter).tryConsume(keyCaptor.capture(), anyInt(), anyInt());
        assertEquals("ratelimit:login:192.168.1.42", keyCaptor.getValue());
    }

    @Test
    void ipStrategyKeyShouldPreferXForwardedForOverRemoteAddr() throws Throwable {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("10.0.0.1");
        httpRequest.addHeader("X-Forwarded-For", "203.0.113.55, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));

        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        RateLimit rateLimit = createRateLimit("ratelimit:login", 10, 10, RateLimit.RateLimitStrategy.IP);

        when(signature.toShortString()).thenReturn("AuthController.login(..)");
        when(pjp.getSignature()).thenReturn(signature);
        when(rateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");

        aspect.enforce(pjp, rateLimit);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimiter).tryConsume(keyCaptor.capture(), anyInt(), anyInt());
        // Only the leftmost (originating) IP is used — not the proxy address
        assertEquals("ratelimit:login:203.0.113.55", keyCaptor.getValue());
    }

    @Test
    void userStrategyKeyShouldIncorporateAuthenticatedUsername() throws Throwable {
        // Arrange: authenticated principal in SecurityContext
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("alice@example.com", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        RateLimit rateLimit = createRateLimit("ratelimit:login", 10, 10, RateLimit.RateLimitStrategy.USER);

        when(signature.toShortString()).thenReturn("AuthController.login(..)");
        when(pjp.getSignature()).thenReturn(signature);
        when(rateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");

        aspect.enforce(pjp, rateLimit);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimiter).tryConsume(keyCaptor.capture(), anyInt(), anyInt());
        assertEquals("ratelimit:login:alice@example.com", keyCaptor.getValue());
    }

    @Test
    void globalStrategyKeyShouldBeSharedMethodSignatureNotClientSpecific() throws Throwable {
        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        RateLimit rateLimit = createRateLimit("ratelimit:login", 10, 10, RateLimit.RateLimitStrategy.GLOBAL);

        when(signature.toShortString()).thenReturn("AuthController.login(..)");
        when(signature.getName()).thenReturn("login");
        when(pjp.getSignature()).thenReturn(signature);
        when(rateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");

        aspect.enforce(pjp, rateLimit);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimiter).tryConsume(keyCaptor.capture(), anyInt(), anyInt());
        // GLOBAL key uses the method short-string — shared for every caller
        assertEquals("ratelimit:login:AuthController.login(..)", keyCaptor.getValue());
    }

    @Test
    void ipStrategyViolationShouldTagMetricWithIpStrategy() throws Throwable {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("1.2.3.4");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));

        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        RateLimit rateLimit = createRateLimit("ratelimit:login", 10, 10, RateLimit.RateLimitStrategy.IP);

        when(signature.toShortString()).thenReturn("AuthController.login(..)");
        when(signature.getName()).thenReturn("login");
        when(pjp.getSignature()).thenReturn(signature);
        when(rateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(messageService.getMessage("rate.limit.exceeded")).thenReturn("too many");

        assertThrows(RateLimitException.class, () -> aspect.enforce(pjp, rateLimit));

        Counter counter = meterRegistry.find("hrm.rate_limit.violations")
                .tag("strategy", "IP")
                .tag("method", "login")
                .counter();
        assertEquals(1.0, counter == null ? null : counter.count());
    }

    @Test
    void userStrategyViolationShouldTagMetricWithUserStrategy() throws Throwable {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("bob@example.com", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        RateLimit rateLimit = createRateLimit("ratelimit:login", 10, 10, RateLimit.RateLimitStrategy.USER);

        when(signature.toShortString()).thenReturn("AuthController.login(..)");
        when(signature.getName()).thenReturn("login");
        when(pjp.getSignature()).thenReturn(signature);
        when(rateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(messageService.getMessage("rate.limit.exceeded")).thenReturn("too many");

        assertThrows(RateLimitException.class, () -> aspect.enforce(pjp, rateLimit));

        Counter counter = meterRegistry.find("hrm.rate_limit.violations")
                .tag("strategy", "USER")
                .tag("method", "login")
                .counter();
        assertEquals(1.0, counter == null ? null : counter.count());
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private RateLimit createRateLimit(String keyPrefix, int capacity, int refillRate, RateLimit.RateLimitStrategy strategy) {
        return new RateLimit() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RateLimit.class;
            }

            @Override
            public int capacity() {
                return capacity;
            }

            @Override
            public int refillRate() {
                return refillRate;
            }

            @Override
            public String keyPrefix() {
                return keyPrefix;
            }

            @Override
            public RateLimit.RateLimitStrategy strategy() {
                return strategy;
            }
        };
    }
}