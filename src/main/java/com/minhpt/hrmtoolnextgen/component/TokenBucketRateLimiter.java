package com.minhpt.hrmtoolnextgen.component;

import java.time.Instant;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Distributed token bucket rate limiter backed by Redis.
 *
 * <p>Each bucket is stored as a Redis hash with two fields:
 * <ul>
 *   <li>{@code tokens} – current token count (decimal)</li>
 *   <li>{@code lastRefillTime} – epoch-ms of the last refill</li>
 * </ul>
 * A single Lua script executes atomically so there are no race conditions
 * even across multiple application nodes.
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class TokenBucketRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Lua script – returns a two-element list: [allowed (0|1), remainingTokens].
     *
     * <p>ARGV: [capacity, refillRate (tokens/min), nowMs]
     */
    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>(
            """
            local key            = KEYS[1]
            local capacity       = tonumber(ARGV[1])
            local refillRate     = tonumber(ARGV[2])
            local now            = tonumber(ARGV[3])

            local data           = redis.call('HMGET', key, 'tokens', 'lastRefillTime')
            local tokens         = tonumber(data[1]) or capacity
            local lastRefillTime = tonumber(data[2]) or now

            local elapsedMs      = math.max(0, now - lastRefillTime)
            local tokensToAdd    = (elapsedMs / 60000) * refillRate
            tokens               = math.min(capacity, tokens + tokensToAdd)

            local allowed = 0
            if tokens >= 1 then
                tokens  = tokens - 1
                allowed = 1
            end

            redis.call('HMSET', key, 'tokens', tokens, 'lastRefillTime', now)
            -- TTL: 2 × the refill window (120 s) to auto-clean idle buckets
            redis.call('EXPIRE', key, 120)

            return {allowed, math.floor(tokens)}
            """,
            List.class
    );

    /**
     * Attempts to consume one token from the bucket identified by {@code key}.
     *
     * @param key        Redis key for this bucket (e.g. "ratelimit:login:192.168.1.1")
     * @param capacity   Maximum tokens the bucket can hold
     * @param refillRate Tokens added per minute
     * @return {@code true} if a token was consumed and the request is allowed
     */
    public boolean tryConsume(String key, int capacity, int refillRate) {
        try {
            long nowMs = Instant.now().toEpochMilli();
            List<?> result = stringRedisTemplate.execute(
                    TOKEN_BUCKET_SCRIPT,
                    List.of(key),
                    String.valueOf(capacity),
                    String.valueOf(refillRate),
                    String.valueOf(nowMs)
            );

            if (result.isEmpty()) {
                log.warn("Rate limiter Redis script returned null for key '{}'; allowing request", key);
                return true; // fail-open: don't block traffic if Redis is temporarily unavailable
            }

            long allowed = ((Number) result.get(0)).longValue();
            log.debug("Rate limit check – key: {}, allowed: {}, remaining: {}", key, allowed, result.get(1));
            return allowed == 1L;

        } catch (Exception e) {
            log.error("Rate limiter error for key '{}'; failing open: {}", key, e.getMessage());
            return true; // fail-open
        }
    }
}
