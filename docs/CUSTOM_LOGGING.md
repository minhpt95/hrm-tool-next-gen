# Custom JSON Logging Guide

This project includes a custom JSON logging format that produces structured logs matching the requested format:

```json
{
  "timestamp": "2025-12-04T03:45:03.633Z",
  "level": "INFO",
  "module": "UserService",
  "file": "src/main/java/com/vatek/hrmtoolnextgen/service/UserService.java",
  "line": 57,
  "message": "User created successfully",
  "operation": "createUser",
  "context": {"userId": 12345, "email": "user@example.com"}
}
```

## Components

### 1. CustomJsonLayout
A custom Log4j2 layout (`CustomJsonLayout.java`) that formats log events as JSON with:
- **timestamp**: ISO 8601 format (UTC)
- **level**: Log level (INFO, DEBUG, ERROR, etc.)
- **module**: Service/component name (from MDC)
- **file**: Source file path
- **line**: Line number in source file
- **message**: Log message
- **operation**: Method/function name (from MDC)
- **context**: Additional contextual data as JSON object (from MDC)

### 2. LoggingUtils
Utility class (`LoggingUtils.java`) for managing logging context:
- `setModule(String module)` - Set the module/service name
- `setOperation(String operation)` - Set the operation/method name
- `setContext(Map<String, Object> context)` - Set context map
- `addContext(String key, Object value)` - Add a single context key-value pair
- `clearContext()` - Clear all context
- `withContext(...)` - Execute code block with automatic context cleanup

### 3. log4j2.xml Configuration
The Log4j2 configuration file uses `CustomJsonLayout` for all appenders (console and file).

## Usage Examples

### Basic Usage

```java
@Service
@Log4j2
public class UserService {
    
    public void createUser(CreateUserRequest request) {
        // Set module and operation
        LoggingUtils.setModule("UserService");
        LoggingUtils.setOperation("createUser");
        
        // Set context
        Map<String, Object> context = new HashMap<>();
        context.put("email", request.getEmail());
        context.put("role", request.getRole());
        LoggingUtils.setContext(context);
        
        // Log with structured format
        log.info("Creating new user");
        
        // ... business logic ...
        
        // Clear context when done
        LoggingUtils.clearContext();
    }
}
```

### Using withContext Helper (Recommended)

```java
public void cleanupSessions() {
    Map<String, Object> context = new HashMap<>();
    context.put("cronIntervalMinutes", 1);
    context.put("scheduledTask", "cleanup");
    
    LoggingUtils.withContext(
        "TableSessionCleanupService",
        "cleanupSessions",
        context,
        () -> {
            log.info("Table session cleanup cron job scheduled");
            // ... cleanup logic ...
        }
    );
    // Context automatically cleared
}
```

### Incremental Context

```java
public void processData() {
    LoggingUtils.setModule("DataProcessingService");
    LoggingUtils.setOperation("processData");
    
    LoggingUtils.addContext("step", "initialization");
    log.debug("Starting initialization");
    
    LoggingUtils.addContext("step", "processing");
    log.info("Processing data");
    
    LoggingUtils.addContext("step", "completion");
    log.info("Process completed");
    
    LoggingUtils.clearContext();
}
```

### Without Context (Still Works)

If you don't set module, operation, or context, the log will still work but those fields will be omitted:

```java
// No context set
log.warn("This is a warning without structured context");
```

Output:
```json
{
  "timestamp": "2025-12-04T03:45:03.633Z",
  "level": "WARN",
  "file": "src/main/java/com/vatek/hrmtoolnextgen/service/UserService.java",
  "line": 42,
  "message": "This is a warning without structured context"
}
```

## Log Output Locations

Logs are written to multiple locations based on level:

- **Console**: All logs with JSON format
- **logs/app.log**: All application logs
- **logs/error/app-error.log**: ERROR level and above
- **logs/debug/app-debug.log**: DEBUG level
- **logs/info/app-info.log**: INFO level
- **logs/perf/app-perf.log**: Performance logs
- **logs/trace/app-trace.log**: TRACE level

## Best Practices

1. **Always clear context** after use, especially in long-running threads
2. **Use withContext()** helper method for automatic cleanup
3. **Set module** to the service/component class name
4. **Set operation** to the method/function name
5. **Include relevant context** data that helps with debugging
6. **Don't include sensitive data** in context (passwords, tokens, etc.)

## Color Support

The custom logger supports colored output for console logs using ANSI color codes:

- **TRACE**: Gray
- **DEBUG**: Cyan
- **INFO**: Bright Green
- **WARN**: Bright Yellow
- **ERROR/FATAL**: Bright Red
- **Keys**: Bright White
- **Module**: Cyan
- **Operation**: Magenta
- **Timestamp**: Gray

Colors are automatically applied to console output but **not** to file logs (files remain plain JSON for parsing).

### Enabling/Disabling Colors

In `log4j2.xml`, you can control colors per appender:

```xml
<!-- Console with colors -->
<Console name="Console" target="SYSTEM_OUT">
    <CustomJsonLayout charset="UTF-8" colorEnabled="true"/>
</Console>

<!-- File without colors (plain JSON) -->
<RollingFile name="FileAppender" fileName="${LOG_DIR}/app.log">
    <CustomJsonLayout charset="UTF-8" colorEnabled="false"/>
</RollingFile>
```

**Note**: Colors only work in terminals that support ANSI escape codes. Most modern terminals (Windows Terminal, PowerShell, Git Bash, Linux/Mac terminals) support this.

## Configuration

The logging configuration is in `src/main/resources/log4j2.xml`. To modify:

- **Log levels**: Change the `level` attribute in `<Logger>` or `<Root>` elements
- **File locations**: Modify the `fileName` attribute in `<RollingFile>` appenders
- **Rollover policy**: Adjust `<Policies>` for time/size-based rollover
- **Colors**: Set `colorEnabled="true"` or `colorEnabled="false"` in `CustomJsonLayout`

## Troubleshooting

### Logs not appearing in JSON format
- Ensure `log4j2.xml` is in `src/main/resources/`
- Check that `CustomJsonLayout` is used in appenders
- Verify Log4j2 dependencies are in `pom.xml`

### Context not appearing in logs
- Ensure `LoggingUtils.setModule()`, `setOperation()`, or `setContext()` is called before logging
- Check that context is not cleared before the log statement
- Verify MDC is working (thread-local storage)

### File/line information missing
- Ensure location information is enabled in Log4j2 (default: enabled)
- Check that source files are compiled with debug information (`-g` flag)

