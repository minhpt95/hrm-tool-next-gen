package com.minhpt.hrmtoolnextgen.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minhpt.hrmtoolnextgen.annotation.RateLimit;
import com.minhpt.hrmtoolnextgen.exception.RateLimitException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.mockito.Mockito;

class RateLimitingAspectTest {

    private final TokenBucketRateLimiter rateLimiter = Mockito.mock(TokenBucketRateLimiter.class);
    private final MessageService messageService = Mockito.mock(MessageService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private RateLimitingAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitingAspect(rateLimiter, messageService, meterRegistry);
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