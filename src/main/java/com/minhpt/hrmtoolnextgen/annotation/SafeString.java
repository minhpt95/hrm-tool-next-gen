package com.minhpt.hrmtoolnextgen.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.minhpt.hrmtoolnextgen.component.SafeStringValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates that a string field does not contain HTML or script-injection patterns.
 *
 * <p>Null values are considered valid (combine with {@code @NotNull} / {@code @NotBlank}
 * if the field is mandatory).
 */
@Documented
@Constraint(validatedBy = SafeStringValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeString {

    String message() default "Input contains invalid or potentially unsafe characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
