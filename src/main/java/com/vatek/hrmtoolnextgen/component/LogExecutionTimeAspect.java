package com.vatek.hrmtoolnextgen.component;

import com.vatek.hrmtoolnextgen.annotation.LogExecutionTime;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * Aspect class that intercepts methods annotated with @LogExecutionTime
 * and logs their execution time.
 * 
 * This aspect uses Spring AOP to provide cross-cutting concern for
 * performance monitoring without modifying the actual business logic.
 * 
 * @author HRM Tool Next Gen
 */
@Aspect
@Component
@Log4j2
public class LogExecutionTimeAspect {

    /**
     * Around advice that intercepts methods annotated with @LogExecutionTime.
     * Measures execution time and logs it using the performance logger.
     * 
     * @param joinPoint the join point representing the intercepted method
     * @param logExecutionTime the annotation instance
     * @return the result of the method execution
     * @throws Throwable if the intercepted method throws an exception
     */
    @Around("@annotation(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        String methodName = getMethodName(joinPoint);
        String description = getDescription(logExecutionTime, methodName);
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            log.debug("Starting execution of method: {}.{}", className, methodName);
            
            // Execute the actual method
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log execution time using performance logger
            log.info("Method execution completed - Class: {}, Method: {}, Description: {}, Execution Time: {} ms", 
                    className, methodName, description, executionTime);
            
            return result;
            
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log execution time even if method throws an exception
            log.error("Method execution failed - Class: {}, Method: {}, Description: {}, Execution Time: {} ms, Error: {}", 
                    className, methodName, description, executionTime, throwable.getMessage(), throwable);
            
            throw throwable;
        }
    }

    /**
     * Extracts the method name from the join point.
     * 
     * @param joinPoint the join point
     * @return the method name
     */
    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod().getName();
    }

    /**
     * Gets the description from the annotation or uses the method name as fallback.
     * 
     * @param logExecutionTime the annotation instance
     * @param methodName the method name as fallback
     * @return the description to use in log messages
     */
    private String getDescription(LogExecutionTime logExecutionTime, String methodName) {
        String description = logExecutionTime.description();
        return StringUtils.hasText(description) ? description : methodName;
    }
}
