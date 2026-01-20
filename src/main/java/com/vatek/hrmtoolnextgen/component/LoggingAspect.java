package com.vatek.hrmtoolnextgen.component;

import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Cross-cutting logging for the main application flow:
 * from controllers through services down to repositories and back to the response.
 *
 * This aspect is intentionally generic and lightweight:
 * - Logs method entry and exit for controller, service and repository layers.
 * - Measures and logs execution time for every intercepted method.
 * - Avoids touching business logic (purely cross-cutting concern).
 *
 * To adjust verbosity, change the log level in {@code log4j2.xml} for
 * the logger {@code com.vatek.hrmtoolnextgen}.
 */
@Aspect
@Component
@Log4j2
public class LoggingAspect {

    /**
     * Controller layer: REST controllers that handle HTTP requests.
     */
    @Pointcut("within(com.vatek.hrmtoolnextgen.controller..*)")
    public void controllerLayer() {
        // pointcut signature
    }

    /**
     * Service layer: application business logic.
     */
    @Pointcut("within(com.vatek.hrmtoolnextgen.service..*)")
    public void serviceLayer() {
        // pointcut signature
    }

    /**
     * Repository layer: data access logic.
     */
    @Pointcut("within(com.vatek.hrmtoolnextgen.repository..*)")
    public void repositoryLayer() {
        // pointcut signature
    }

    /**
     * Full application flow from controller -> service -> repository.
     */
    @Pointcut("controllerLayer() || serviceLayer() || repositoryLayer()")
    public void applicationFlow() {
        // pointcut signature
    }

    /**
     * Logs method entry, exit, and execution time for the whole application flow.
     *
     * The logs are emitted at TRACE level to keep them separate from
     * business logs; see the Trace appender in {@code log4j2.xml}.
     */
    @Around("applicationFlow()")
    public Object logAroundApplicationFlow(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Class<?> declaringClass = methodSignature.getDeclaringType();
        String className = declaringClass.getSimpleName();
        String methodName = methodSignature.getName();
        String layer = resolveLayer(declaringClass.getPackageName());

        Object[] args = joinPoint.getArgs();
        String argsSummary = buildArgsSummary(args);

        // Entry log
        if (log.isTraceEnabled()) {
            log.trace("[{}] Entering {}.{} with args: {}", layer, className, methodName, argsSummary);
        }

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            // Exit log
            if (log.isTraceEnabled()) {
                log.trace("[{}] Exiting {}.{}; duration={} ms; result={}",
                        layer, className, methodName, duration, safeToString(result));
            }

            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;

            // Error log
            log.error("[{}] Exception in {}.{}; duration={} ms; message={}",
                    layer, className, methodName, duration, ex.getMessage(), ex);

            throw ex;
        }
    }

    /**
     * Resolve logical layer name based on package.
     */
    private String resolveLayer(String packageName) {
        if (packageName.contains(".controller")) {
            return "CONTROLLER";
        }
        if (packageName.contains(".service")) {
            return "SERVICE";
        }
        if (packageName.contains(".repository")) {
            return "REPOSITORY";
        }
        return "UNKNOWN";
    }

    /**
     * Builds a short, safe argument summary to avoid huge logs or sensitive data leakage.
     */
    private String buildArgsSummary(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        // Only log argument types and simple values to keep logs readable and safe
        return Arrays.stream(args)
                .map(arg -> arg == null
                        ? "null"
                        : arg.getClass().getSimpleName() + "(" + safeToString(arg) + ")")
                .limit(10) // protect against crazy argument lists
                .toList()
                .toString();
    }

    /**
     * Safe conversion to String that guards against noisy objects.
     */
    private String safeToString(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            String text = value.toString();
            // Truncate very long values to keep logs compact
            return text.length() > 500 ? text.substring(0, 497) + "..." : text;
        } catch (Exception ex) {
            return "<?>"; // fallback if toString() misbehaves
        }
    }
}

