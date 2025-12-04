package com.vatek.hrmtoolnextgen.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Custom JSON layout for Log4j2 that formats logs in the requested structure:
 * {
 *   "timestamp": "2025-12-04T03:45:03.633Z",
 *   "level": "LOG",
 *   "module": "TableSessionCleanupService",
 *   "file": "src/modules/restaurant/services/table-session-cleanup.service.ts",
 *   "line": 57,
 *   "message": "Table session cleanup cron job scheduled",
 *   "operation": "onModuleInit",
 *   "context": {"cronIntervalMinutes": 1}
 * }
 * 
 * Supports colored output for console with ANSI color codes.
 */
@Plugin(name = "CustomJsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class CustomJsonLayout extends AbstractStringLayout {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    private static final String MDC_MODULE = "log.module";
    private static final String MDC_OPERATION = "log.operation";
    private static final String MDC_CONTEXT = "log.context";

    // ANSI Color Codes
    private static final String RESET = "\u001B[0m";
    
    // Text Colors
    private static final String YELLOW = "\u001B[33m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    
    // Bright Colors
    private static final String BRIGHT_BLACK = "\u001B[90m";
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_WHITE = "\u001B[97m";

    private final boolean colorEnabled;

    protected CustomJsonLayout(Charset charset, boolean colorEnabled) {
        super(charset);
        this.colorEnabled = colorEnabled;
    }

    @PluginFactory
    public static CustomJsonLayout createLayout(
            @PluginAttribute(value = "charset", defaultString = "UTF-8") final String charset,
            @PluginAttribute(value = "colorEnabled", defaultBoolean = false) final boolean colorEnabled) {
        return new CustomJsonLayout(Charset.forName(charset), colorEnabled);
    }

    @Override
    public String toSerializable(LogEvent event) {
        ObjectNode json = objectMapper.createObjectNode();

        // Timestamp in ISO 8601 format
        Instant instant = Instant.ofEpochMilli(event.getTimeMillis());
        String timestamp = ISO_FORMATTER.format(instant);
        json.put("timestamp", timestamp);

        // Level - convert to uppercase string (INFO -> INFO, DEBUG -> DEBUG, etc.)
        String level = event.getLevel().toString();
        json.put("level", level);

        // Module from MDC
        ReadOnlyStringMap contextData = event.getContextData();
        String module = contextData.getValue(MDC_MODULE);
        if (module != null) {
            json.put("module", module);
        }

        // File and line from location information
        StackTraceElement location = event.getSource();
        if (location != null) {
            String fileName = location.getFileName();
            if (fileName != null) {
                // Convert package path to file path format
                String className = location.getClassName();
                String filePath = convertClassNameToFilePath(className, fileName);
                json.put("file", filePath);
            }
            int lineNumber = location.getLineNumber();
            if (lineNumber > 0) {
                json.put("line", lineNumber);
            }
        }

        // Message
        String message = event.getMessage().getFormattedMessage();
        if (message != null) {
            json.put("message", message);
        }

        // Operation from MDC
        String operation = contextData.getValue(MDC_OPERATION);
        if (operation != null) {
            json.put("operation", operation);
        }

        // Context from MDC (already JSON string)
        String contextJson = contextData.getValue(MDC_CONTEXT);
        if (contextJson != null && !contextJson.isEmpty()) {
            try {
                // Parse the JSON string and add as object
                Object contextObj = objectMapper.readValue(contextJson, Object.class);
                json.set("context", objectMapper.valueToTree(contextObj));
            } catch (Exception e) {
                // If parsing fails, add as string
                json.put("context", contextJson);
            }
        }

        String jsonString = json.toString();
        
        // Apply colors if enabled
        if (colorEnabled) {
            jsonString = applyColors(jsonString, event.getLevel(), timestamp, level, module, message, operation);
        }

        return jsonString + "\n";
    }

    /**
     * Apply ANSI color codes to the JSON string based on log level.
     */
    private String applyColors(String json, Level level, String timestamp, String levelStr, 
                               String module, String message, String operation) {
        // Get color for log level
        String levelColor = getLevelColor(level);
        String timestampColor = BRIGHT_BLACK;
        String moduleColor = CYAN;
        String messageColor = getMessageColor(level);
        String operationColor = MAGENTA;
        String keyColor = BRIGHT_WHITE;
        String reset = RESET;
        
        // Colorize the JSON by replacing key-value pairs
        // This is a simple approach - for more complex coloring, consider using a JSON parser
        
        // Colorize timestamp
        json = json.replace("\"timestamp\":\"" + timestamp + "\"", 
            keyColor + "\"timestamp\"" + reset + ":" + timestampColor + "\"" + timestamp + "\"" + reset);
        
        // Colorize level
        json = json.replace("\"level\":\"" + levelStr + "\"", 
            keyColor + "\"level\"" + reset + ":" + levelColor + "\"" + levelStr + "\"" + reset);
        
        // Colorize module
        if (module != null) {
            json = json.replace("\"module\":\"" + module + "\"", 
                keyColor + "\"module\"" + reset + ":" + moduleColor + "\"" + module + "\"" + reset);
        }
        
        // Colorize message
        // Note: The message in JSON is already properly escaped by Jackson
        // We need to find the message value in the JSON and colorize it
        if (message != null && json.contains("\"message\"")) {
            // Use a more robust approach: find the message value in JSON
            // The message might contain escaped characters, so we need to be careful
            int messageKeyIndex = json.indexOf("\"message\":\"");
            if (messageKeyIndex >= 0) {
                int messageStart = messageKeyIndex + 11; // length of "message":"
                int messageEnd = json.indexOf("\"", messageStart);
                // Find the end of the message value (accounting for escaped quotes)
                while (messageEnd > 0 && messageEnd < json.length() - 1 && 
                       json.charAt(messageEnd - 1) == '\\') {
                    messageEnd = json.indexOf("\"", messageEnd + 1);
                }
                if (messageEnd > messageStart) {
                    String beforeMessage = json.substring(0, messageStart);
                    String messageValue = json.substring(messageStart, messageEnd);
                    String afterMessage = json.substring(messageEnd);
                    json = beforeMessage + messageColor + messageValue + reset + afterMessage;
                    // Also colorize the key
                    json = json.replace("\"message\":", keyColor + "\"message\"" + reset + ":");
                }
            }
        }
        
        // Colorize operation
        if (operation != null) {
            json = json.replace("\"operation\":\"" + operation + "\"", 
                keyColor + "\"operation\"" + reset + ":" + operationColor + "\"" + operation + "\"" + reset);
        }
        
        // Colorize other keys (file, line, context)
        json = json.replace("\"file\":", keyColor + "\"file\"" + reset + ":");
        json = json.replace("\"line\":", keyColor + "\"line\"" + reset + ":");
        json = json.replace("\"context\":", keyColor + "\"context\"" + reset + ":");
        
        return json;
    }

    /**
     * Get ANSI color code for log level.
     */
    private String getLevelColor(Level level) {
        if (level == Level.TRACE) {
            return BRIGHT_BLACK;
        } else if (level == Level.DEBUG) {
            return CYAN;
        } else if (level == Level.INFO) {
            return BRIGHT_GREEN;
        } else if (level == Level.WARN) {
            return BRIGHT_YELLOW;
        } else if (level == Level.ERROR || level == Level.FATAL) {
            return BRIGHT_RED;
        }
        return WHITE;
    }

    /**
     * Get ANSI color code for message based on log level.
     */
    private String getMessageColor(Level level) {
        if (level == Level.TRACE) {
            return BRIGHT_BLACK;
        } else if (level == Level.DEBUG) {
            return CYAN;
        } else if (level == Level.INFO) {
            return WHITE;
        } else if (level == Level.WARN) {
            return YELLOW;
        } else if (level == Level.ERROR || level == Level.FATAL) {
            return BRIGHT_RED;
        }
        return WHITE;
    }

    /**
     * Convert Java class name to file path format.
     * Example: com.vatek.hrmtoolnextgen.service.UserService -> src/main/java/com/vatek/hrmtoolnextgen/service/UserService.java
     */
    private String convertClassNameToFilePath(String className, String fileName) {
        if (className == null || fileName == null) {
            return fileName != null ? fileName : "unknown";
        }

        try {
            // Replace dots with slashes for package structure
            int lastDotIndex = className.lastIndexOf('.');
            if (lastDotIndex > 0) {
                String packagePath = className.substring(0, lastDotIndex);
                packagePath = packagePath.replace('.', '/');
                // Build the full path
                return "src/main/java/" + packagePath + "/" + fileName;
            }
        } catch (Exception e) {
            // Fallback to just the filename
        }
        
        return fileName;
    }
}

