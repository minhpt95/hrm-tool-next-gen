package com.vatek.hrmtoolnextgen.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark endpoints as idempotent.
 * When this annotation is present, the IdempotencyInterceptor will check for
 * an idempotency-key header and return cached responses for duplicate requests.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    /**
     * Time to live in hours for the idempotency key.
     * Default is 24 hours.
     */
    long ttlHours() default 24;
}

