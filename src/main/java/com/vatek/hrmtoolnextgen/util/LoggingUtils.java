package com.vatek.hrmtoolnextgen.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for structured logging with module, operation, and context information.
 * This helps create logs in the format:
 * {
 *   "timestamp": "...",
 *   "level": "LOG",
 *   "module": "ServiceName",
 *   "file": "...",
 *   "line": 123,
 *   "message": "...",
 *   "operation": "methodName",
 *   "context": {...}
 * }
 */
public class LoggingUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String MDC_MODULE = "log.module";
    private static final String MDC_OPERATION = "log.operation";
    private static final String MDC_CONTEXT = "log.context";

    /**
     * Set the module name for the current thread's logging context.
     * 
     * @param module The module/service name (e.g., "UserService", "TableSessionCleanupService")
     */
    public static void setModule(String module) {
        MDC.put(MDC_MODULE, module);
    }

    /**
     * Set the operation/method name for the current thread's logging context.
     * 
     * @param operation The operation name (e.g., "onModuleInit", "createUser", "cleanupSessions")
     */
    public static void setOperation(String operation) {
        MDC.put(MDC_OPERATION, operation);
    }

    /**
     * Set the context map for the current thread's logging context.
     * 
     * @param context A map of contextual information (e.g., {"cronIntervalMinutes": 1})
     */
    public static void setContext(Map<String, Object> context) {
        if (context != null && !context.isEmpty()) {
            try {
                MDC.put(MDC_CONTEXT, objectMapper.writeValueAsString(context));
            } catch (JsonProcessingException e) {
                // Fallback to toString if JSON serialization fails
                MDC.put(MDC_CONTEXT, context.toString());
            }
        }
    }

    /**
     * Set a single context key-value pair.
     * 
     * @param key The context key
     * @param value The context value
     */
    public static void addContext(String key, Object value) {
        Map<String, Object> context = getContext();
        context.put(key, value);
        setContext(context);
    }

    /**
     * Get the current context map from MDC.
     * 
     * @return The context map, or empty map if none exists
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getContext() {
        String contextJson = MDC.get(MDC_CONTEXT);
        if (contextJson != null && !contextJson.isEmpty()) {
            try {
                return objectMapper.readValue(contextJson, Map.class);
            } catch (JsonProcessingException e) {
                return new HashMap<>();
            }
        }
        return new HashMap<>();
    }

    /**
     * Clear all logging context (module, operation, context) for the current thread.
     */
    public static void clearContext() {
        MDC.remove(MDC_MODULE);
        MDC.remove(MDC_OPERATION);
        MDC.remove(MDC_CONTEXT);
    }

    /**
     * Execute a block of code with logging context set.
     * Automatically clears context after execution.
     * 
     * @param module The module name
     * @param operation The operation name
     * @param context The context map
     * @param runnable The code to execute
     */
    public static void withContext(String module, String operation, Map<String, Object> context, Runnable runnable) {
        try {
            setModule(module);
            setOperation(operation);
            setContext(context);
            runnable.run();
        } finally {
            clearContext();
        }
    }

    /**
     * Get the module name from MDC.
     * 
     * @return The module name, or null if not set
     */
    public static String getModule() {
        return MDC.get(MDC_MODULE);
    }

    /**
     * Get the operation name from MDC.
     * 
     * @return The operation name, or null if not set
     */
    public static String getOperation() {
        return MDC.get(MDC_OPERATION);
    }
}

