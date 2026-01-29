## HRM Tool Next Gen – Backend

Spring Boot 3.x backend for the **HRM Tool Next Gen** platform. It provides
REST APIs for authentication, user management, projects, timesheets, holidays,
and more, following a layered architecture (controller → service → repository).

### Tech stack

- **Language**: Java 21  
- **Framework**: Spring Boot 3.5.x  
- **Modules**:
  - Spring Web, Validation
  - Spring Data JPA, PostgreSQL
  - Spring Security, Spring Session (Redis / JDBC)
  - Spring AMQP (RabbitMQ)
  - Spring Modulith
  - Thymeleaf (for email templates)
  - Liquibase (DB migrations)
  - Log4j2 (JSON structured logging)

### Controller APIs (high level)

This is a high-level overview of the main REST entry points. For full details, use Swagger UI (see `SwaggerConfig`) or OpenAPI docs.

- **Authentication** – `AuthController`  
  - Base path: `/api/auth`  
  - Login, refresh token, logout, forgot password, reset password.

- **User self-service** – `UserController`  
  - Base path: `/api/user`  
  - Manage own timesheets, day-off requests, and personal project views.

- **Manager operations** – `ManagerController`  
  - Base path: `/api/manager`  
  - Manage team members, projects, timesheets, and approvals.

- **Admin operations** – `AdminController`  
  - Base path: `/api/admin`  
  - System-wide user management, project catalog, and configuration endpoints.

- **Admin dashboard** – `DashboardController`  
  - Base path: `/api/admin/dashboard`  
  - Aggregated KPIs and summary metrics for administrators.

- **Holidays** – `HolidayController`  
  - Base path: `/api/holidays`  
  - Vietnam public and lunar holidays by year, range, and date checks.

- **Real-time events (SSE)** – `SseController`  
  - Base path: `/sse`  
  - Server-Sent Events endpoints for real-time notifications and connection monitoring.

### Build & run

- **Build**:
  - `mvn clean install`
- **Run (local profile)**:
  - `mvn spring-boot:run`
  - or `java -jar target/hrm-tool-<profile>-<version>.jar`
- **Active profiles**:
  - `local` (default)
  - `test`
  - `stg`
  - `prod`

### Docker & infrastructure

- `compose.yaml` defines:
  - **mysql** – primary relational database
  - **rabbitmq** – messaging
  - **redis** – cache & session store

Ensure the image tags in `compose.yaml` match your environment before using
in non-local setups.

### Logging & observability

- Logging uses **Log4j2** with a **custom JSON layout** (see `log4j2.xml` and
  `docs/CUSTOM_LOGGING.md`).
- Log files:
  - `logs/app.log` – main application log
  - `logs/error/app-error.log` – errors only
  - `logs/debug/app-debug.log` – debug
  - `logs/info/app-info.log` – info
  - `logs/perf/app-perf.log` – performance/metrics
  - `logs/trace/app-trace.log` – low-level traces (including AOP flow)

#### Execution time logging via `@LogExecutionTime`

- **Annotation**: `com.vatek.hrmtoolnextgen.annotation.LogExecutionTime`  
- **Aspect**: `com.vatek.hrmtoolnextgen.component.LogExecutionTimeAspect`

Use this to measure and log execution time of any method:

```java
@LogExecutionTime
public void someBusinessMethod() {
    // ...
}
```

Features:

- Logs start/end of the method, execution time in ms.
- Optional `description` attribute for clearer log context.
- Logs errors with execution time if exceptions occur.

#### End-to-end flow logging (Controller → Service → Repository)

- **Aspect**: `com.vatek.hrmtoolnextgen.component.LoggingAspect`
- Applies to:
  - `com.vatek.hrmtoolnextgen.controller..*`
  - `com.vatek.hrmtoolnextgen.service..*`
  - `com.vatek.hrmtoolnextgen.repository..*`

What it provides:

- Entry/exit logs for each method in the above packages.
- Layer classification: `CONTROLLER`, `SERVICE`, `REPOSITORY`.
- Safe summaries of arguments and return values (truncated).
- Execution time per method.
- Exception logs with duration.

All flow logs are written at **TRACE** level; enable them by setting the
logger in `log4j2.xml` to `TRACE`:

```xml
<Logger name="com.vatek.hrmtoolnextgen" level="TRACE" additivity="false">
    <!-- appenders -->
</Logger>
```

### Code style & design principles

The project follows:

- **SOLID** principles
- **DRY** and **KISS**
- Clear separation of concerns between layers

When adding new features:

- Put HTTP-related logic in `controller` classes only.
- Keep business rules in `service` classes.
- Keep persistence logic in `repository` classes.
- Reuse the existing logging and AOP infrastructure for observability.

