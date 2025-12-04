package com.vatek.hrmtoolnextgen.service.example;

import com.vatek.hrmtoolnextgen.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Example service demonstrating how to use the custom JSON logging format.
 * 
 * This shows how to log with module, operation, and context information
 * that will appear in the structured JSON logs.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ExampleService {

    /**
     * Example method showing structured logging with context.
     */
    public void exampleMethod() {
        // Set module name (typically the service class name)
        LoggingUtils.setModule("ExampleService");
        
        // Set operation name (the method name)
        LoggingUtils.setOperation("exampleMethod");
        
        // Set context information
        Map<String, Object> context = new HashMap<>();
        context.put("userId", 12345);
        context.put("action", "createUser");
        LoggingUtils.setContext(context);
        
        // Now log - the custom layout will include module, operation, and context
        log.info("Processing user creation request");
        
        // Clear context when done (or use try-finally)
        LoggingUtils.clearContext();
    }

    /**
     * Example using the withContext helper method for automatic cleanup.
     */
    public void exampleWithContextHelper() {
        Map<String, Object> context = new HashMap<>();
        context.put("cronIntervalMinutes", 1);
        context.put("scheduledTask", "cleanup");
        
        LoggingUtils.withContext(
            "ExampleService",
            "exampleWithContextHelper",
            context,
            () -> {
                log.info("Scheduled task executed successfully");
            }
        );
        // Context is automatically cleared after execution
    }

    /**
     * Example of adding context incrementally.
     */
    public void exampleIncrementalContext() {
        LoggingUtils.setModule("ExampleService");
        LoggingUtils.setOperation("exampleIncrementalContext");
        
        // Add context values one by one
        LoggingUtils.addContext("step", "initialization");
        log.debug("Starting initialization");
        
        LoggingUtils.addContext("step", "processing");
        log.info("Processing data");
        
        LoggingUtils.addContext("step", "completion");
        log.info("Process completed");
        
        LoggingUtils.clearContext();
    }

    /**
     * Example showing how logs will appear without setting context.
     * Only timestamp, level, file, line, and message will be included.
     */
    public void exampleWithoutContext() {
        // No context set - log will still work but without module/operation/context fields
        log.warn("This is a warning without structured context");
    }
}

