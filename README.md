## HRM Tool Next Gen – Backend

### My responsibilities

As the sole backend developer on this project, I designed and implemented the entire backend from scratch. Key areas of ownership:

#### Architecture & infrastructure
- Set up Spring Boot 3.x project structure with layered architecture (controller → service → repository).
- Migrated database from MySQL to **PostgreSQL** and configured **Liquibase** for schema versioning and seed data.
- Configured **Docker Compose** with PostgreSQL, RabbitMQ, and Redis services.
- Implemented **idempotency** support and **device-ID** request tracking headers.

#### Security
- Implemented **JWT authentication** (stateless access token + refresh token + logout flow).
- Configured **Spring Security** with role-based access control (`ADMIN`, `PROJECT_MANAGER`, `USER`, `HR`, `IT_ADMIN`) using `hasAuthority` guards.
- Built `JwtProvider`, `JwtAuthTokenFilter`, and `UnauthorizedHandler` for the full auth pipeline.
- Exposed forgot-password and password-reset endpoints with secure email flows.

#### REST API
- **`AuthController`** – login, refresh token, logout, forgot/reset password.
- **`UserController`** – personal timesheets, day-off requests, project views.
- **`ManagerController`** – team member management, project assignments, approval workflows.
- **`AdminController`** – system-wide user/project/role management.
- **`DashboardController`** – aggregated KPI metrics for admins.
- **`HolidayController`** – Vietnam public and lunar holidays via Calendarific API (with caching).
- **`SseController`** – Server-Sent Events for real-time notifications.

#### Cross-cutting concerns
- Built custom **`@LogExecutionTime`** annotation + AOP aspect for method-level performance logging.
- Implemented **`LoggingAspect`** for controller/service-layer request tracing.
- Configured **Log4j2** with a custom JSON layout across five log files (info, debug, error, perf, trace).
- Set up **async email processing** (`AsyncConfig`) and email templates (birthday, welcome, password-reset) via Thymeleaf.
- Implemented **`BirthdaySchedule`** — scheduled job to send birthday emails to users.
- Added **Spring Cache** (Redis) with custom `ObjectMapper` for holiday data and other cacheable responses.

#### Internationalisation & observability
- Configured i18n (`messages.properties`, `messages_vi.properties`) for all system and error messages.
- Integrated Swagger / OpenAPI (`SwaggerConfig`) for full API documentation.

---

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

Versioning and deprecation:

- Legacy endpoints remain available under `/api/...` and `/sse...` for backward compatibility.
- Versioned endpoints are available under `/api/v1/...`.
- Legacy endpoints are marked as deprecated in OpenAPI and emit runtime response headers:
  - `Deprecation: true`
  - `Sunset: Wed, 31 Dec 2026 23:59:59 GMT`
  - `Link: <successor-path>; rel="successor-version"`
- New client integrations should target `/api/v1/...` only.

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

- **Annotation**: `com.minhpt.hrmtoolnextgen.annotation.LogExecutionTime`  
- **Aspect**: `com.minhpt.hrmtoolnextgen.component.LogExecutionTimeAspect`

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

- **Aspect**: `com.minhpt.hrmtoolnextgen.component.LoggingAspect`
- Applies to:
  - `com.minhpt.hrmtoolnextgen.controller..*`
  - `com.minhpt.hrmtoolnextgen.service..*`
  - `com.minhpt.hrmtoolnextgen.repository..*`

What it provides:

- Entry/exit logs for each method in the above packages.
- Layer classification: `CONTROLLER`, `SERVICE`, `REPOSITORY`.
- Safe summaries of arguments and return values (truncated).
- Execution time per method.
- Exception logs with duration.

All flow logs are written at **TRACE** level; enable them by setting the
logger in `log4j2.xml` to `TRACE`:

```xml
<Logger name="com.minhpt.hrmtoolnextgen" level="TRACE" additivity="false">
    <!-- appenders -->
</Logger>
```

### Monitoring metrics

Spring Boot Actuator is enabled and the application emits rate-limit violation metrics.

- Metric name: `hrm.rate_limit.violations`
- Tags:
  - `key_prefix` – logical limiter key prefix such as `ratelimit:login`
  - `strategy` – `IP`, `USER`, or `GLOBAL`
  - `method` – intercepted method name

This metric is incremented whenever a request is rejected by the rate limiter.

Prometheus scraping:

- Prometheus-format metrics are exposed at `/actuator/prometheus`.
- Example scrape config:

```yaml
scrape_configs:
  - job_name: hrm-tool-next-gen
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - localhost:9800
```

- To inspect the series locally, request `/actuator/prometheus` and search for `hrm_rate_limit_violations_total`.

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

