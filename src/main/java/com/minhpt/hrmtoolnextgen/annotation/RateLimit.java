package com.minhpt.hrmtoolnextgen.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable rate limiting on controller methods using the token bucket algorithm.
 * <p>
 * This annotation uses a token bucket algorithm where:
 * - capacity: Maximum number of tokens the bucket can hold
 * - refillRate: Number of tokens added to the bucket per minute
 * <p>
 * When a request is made, a token is consumed from the bucket. If no tokens are available,
 * the request is rate limited and a {@link com.minhpt.hrmtoolnextgen.exception.RateLimitException}
 * is thrown.
 * <p>
 * The rate limiting is distributed using Redis, making it work across multiple application instances.
 *
 * @author HRM Tool Next Gen
 * @see com.minhpt.hrmtoolnextgen.component.TokenBucketRateLimiter
 * @see com.minhpt.hrmtoolnextgen.component.RateLimitingAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Maximum number of tokens the bucket can hold (burst capacity).
     * <p>
     * This represents the maximum number of requests that can be made in a short burst.
     * Default is 100 requests.
     *
     * @return the capacity of the token bucket
     */
    int capacity() default 100;

    /**
     * Number of tokens added to the bucket per minute (refill rate).
     * <p>
     * This represents the sustained rate of requests allowed per minute.
     * Default is 100 tokens per minute.
     *
     * @return the refill rate in tokens per minute
     */
    int refillRate() default 100;

    /**
     * Key prefix for Redis storage of rate limit state.
     * <p>
     * This allows customization of the Redis key used to store rate limit state.
     * The actual key will be constructed as: {keyPrefix}:{identifier}
     * where identifier is typically the IP address or user ID.
     * <p>
     * Default is "ratelimit".
     *
     * @return the key prefix for Redis storage
     */
    String keyPrefix() default "ratelimit";

    /**
     * Strategy for identifying the rate limit subject.
     * <p>
     * IP: Rate limit per IP address
     * USER: Rate limit per authenticated user
     * GLOBAL: Rate limit across all requests
     * <p>
     * Default is IP.
     *
     * @return the rate limit strategy
     */
    RateLimitStrategy strategy() default RateLimitStrategy.IP;

    /**
     * Rate limit strategies for identifying the subject of rate limiting.
     */
    enum RateLimitStrategy {
        /**
         * Rate limit per IP address.
         * Useful for public endpoints and preventing abuse from specific IPs.
         */
        IP,

        /**
         * Rate limit per authenticated user.
         * Useful for user-specific operations and preventing individual user abuse.
         */
        USER,

        /**
         * Global rate limit across all requests.
         * Useful for protecting system resources and preventing overall system overload.
         */
        GLOBAL
    }
}