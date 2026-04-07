package com.minhpt.hrmtoolnextgen.component;

import java.util.regex.Pattern;

import com.minhpt.hrmtoolnextgen.annotation.SafeString;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Rejects strings that contain HTML tags, common script-injection patterns,
 * or SQL-injection markers.
 *
 * <p>The check is intentionally lightweight: it targets the most common attack
 * vectors (XSS, HTML injection, JS event handlers, JNDI/EL injection) without
 * trying to be an exhaustive WAF. Pair this with a strict Content-Security-Policy
 * for defence-in-depth.
 */
public class SafeStringValidator implements ConstraintValidator<SafeString, String> {

    /**
     * Patterns that are rejected:
     * <ul>
     *   <li>HTML tags: {@code <...>}</li>
     *   <li>JavaScript protocol: {@code javascript:}</li>
     *   <li>Inline event handlers: {@code on<word>=}</li>
     *   <li>JNDI/EL injection: {@code ${...}} / {@code #{...}}</li>
     *   <li>SQL comment markers: {@code --} / {@code /*}</li>
     *   <li>Null-byte injection</li>
     * </ul>
     */
    private static final Pattern UNSAFE = Pattern.compile(
            // HTML tags
            "<[^>]*>"
            // javascript: protocol
            + "|javascript\\s*:"
            // event-handler attributes  (e.g. onerror=, onclick=)
            + "|\\bon\\w+\\s*="
            // JNDI / Spring EL / Thymeleaf expression injection
            + "|[\\$#]\\{"
            // SQL comment sequences
            + "|--|/\\*"
            // null byte
            + "|\\x00",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @Override
    public void initialize(SafeString annotation) {
        // no initialisation needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null handling is delegated to @NotNull / @NotBlank
        }
        return !UNSAFE.matcher(value).find();
    }
}
