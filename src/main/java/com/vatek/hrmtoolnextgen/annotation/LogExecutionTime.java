package com.vatek.hrmtoolnextgen.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to log method execution time.
 * When applied to a method, it will automatically log the execution time
 * using AOP (Aspect-Oriented Programming).
 * 
 * Usage:
 * <pre>
 * {@code
 * @LogExecutionTime
 * public void myMethod() {
 *     // method implementation
 * }
 * }
 * </pre>
 * 
 * @author HRM Tool Next Gen
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {
    /**
     * Optional description to include in the log message.
     * If not provided, the method name will be used.
     * 
     * @return description string
     */
    String description() default "";
}
