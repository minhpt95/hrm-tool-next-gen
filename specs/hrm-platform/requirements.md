# Requirements Document

## Introduction

HRM Tool (Next Gen) is a Spring Boot HR-management backend serving a development
organization. It exposes a versioned REST API (legacy `/api/*`, current `/api/v1/*`)
covering authentication, role-based administration, employee self-service
(timesheets, day-off, projects), asset/device management, holiday lookup, an admin
dashboard and real-time notifications over SSE. This document captures the
**as-implemented** behaviour of the system as a baseline for a comprehensive
automated-test effort, plus the one domain still scaffolded-but-unbuilt
(request-a-device), flagged explicitly.

Roles in the system: `ADMIN`, `IT_ADMIN`, `PROJECT_MANAGER`, `USER`, `HR`.
All endpoints are dual-mapped on `/api/*` (deprecated, sunset 2026-12-31) and
`/api/v1/*`. Audience: backend engineering (sole maintainer). Language of the
running product (i18n) is English + Vietnamese.

> **Coverage note** — Requirements R1–R23 describe shipped behaviour and are the
> targets for the test plan in `tasks.md`. Requirement R12 (request-a-device) is a
> **gap**: the entity + enum exist but no controller/service/repository is wired.
> Its criteria are written as the intended behaviour; tests for it depend on
> implementation landing first (called out in `design.md` and `tasks.md`).

## Requirements

### Requirement 1 — Authentication (login)
**User Story**: As an employee, I want to log in with my credentials, so that I receive tokens to call the API.

#### Acceptance Criteria
1. WHEN a client POSTs valid credentials to `/auth/login` THEN the system SHALL return an access token and a refresh token.
2. IF the credentials are invalid THEN the system SHALL respond `401 Unauthorized` without issuing tokens.
3. IF the account is soft-deleted or disabled THEN the system SHALL reject the login.
4. WHEN a login succeeds THEN the system SHALL persist a session record in Redis keyed to the user.
5. The system SHALL return error message bodies localized to the request `Accept-Language` (English or Vietnamese).

### Requirement 2 — Token refresh & logout
**User Story**: As a logged-in employee, I want to refresh or revoke my session, so that I stay authenticated securely.

#### Acceptance Criteria
1. WHEN a client POSTs a valid refresh token to `/auth/refresh` THEN the system SHALL issue a new access token.
2. IF the refresh token is expired, malformed, or unknown THEN the system SHALL respond `401 Unauthorized`.
3. WHEN a client POSTs to `/auth/logout` with a valid session THEN the system SHALL invalidate the session in Redis.
4. WHILE a session is invalidated THE system SHALL reject subsequent requests bearing its access token.

### Requirement 3 — Password recovery
**User Story**: As an employee who forgot my password, I want to request and complete a reset, so that I regain access.

#### Acceptance Criteria
1. WHEN a client POSTs a known email to `/auth/forgot-password` THEN the system SHALL email a time-limited reset token.
2. IF the email is unknown THEN the system SHALL respond without revealing whether the account exists.
3. WHEN a client POSTs a valid reset token and a new password to `/auth/reset-password` THEN the system SHALL update the password hash.
4. IF the reset token is expired or invalid THEN the system SHALL reject the reset.

### Requirement 4 — Authorization & role-based access
**User Story**: As the system owner, I want endpoints gated by role, so that users only reach actions they are entitled to.

#### Acceptance Criteria
1. The system SHALL require a valid JWT for every non-`/auth/**`, non-public endpoint.
2. WHEN an unauthenticated request hits a protected endpoint THEN the system SHALL respond `401 Unauthorized`.
3. WHEN an authenticated user lacks the required role THEN the system SHALL respond `403 Forbidden` with a localized body.
4. The system SHALL restrict `/admin/**` to `ADMIN` and `HR` roles.
5. The system SHALL restrict device write and assignment endpoints to administrative roles.
6. The system SHALL restrict `/manager/**` to `PROJECT_MANAGER` and elevated roles.

### Requirement 5 — Rate limiting
**User Story**: As the system owner, I want sensitive endpoints rate-limited, so that abuse and brute-force are contained.

#### Acceptance Criteria
1. WHEN a client exceeds the configured request rate on a rate-limited endpoint THEN the system SHALL respond `429 Too Many Requests`.
2. WHILE rate limiting is active THE system SHALL record rate-limit metrics for Prometheus.
3. The system SHALL track per-client limits using Redis.

### Requirement 6 — API versioning & legacy deprecation
**User Story**: As an API consumer, I want a stable versioned API with clear deprecation signals, so that I can migrate off legacy paths.

#### Acceptance Criteria
1. The system SHALL serve every endpoint on both `/api/*` and `/api/v1/*`.
2. WHEN a request targets a legacy `/api/*` path THEN the system SHALL add deprecation/sunset response headers.
3. WHERE the path is `/api/v1/*` THE system SHALL NOT add deprecation headers.
4. The system SHALL advertise the sunset date `2026-12-31` for legacy paths.

### Requirement 7 — User administration
**User Story**: As an ADMIN, I want to manage user accounts, so that I can onboard, update and offboard staff.

#### Acceptance Criteria
1. WHEN an ADMIN POSTs a multipart user payload to `/admin/user` THEN the system SHALL create the user and their profile.
2. WHEN an ADMIN GETs `/admin/user/{id}` THEN the system SHALL return the user, or `404` if absent/soft-deleted.
3. WHEN an ADMIN PUTs `/admin/user/{id}` THEN the system SHALL update the profile fields.
4. WHEN an ADMIN DELETEs `/admin/user/{id}` THEN the system SHALL soft-delete the user (set deleted timestamp), not hard-delete.
5. WHEN an ADMIN PUTs `/admin/user/{id}/password` THEN the system SHALL reset that user's password.
6. WHEN an ADMIN GETs `/admin/users` THEN the system SHALL return a paginated user list.
7. IF a uniqueness constraint (identity card or phone number) is violated THEN the system SHALL respond with a localized conflict error.

### Requirement 8 — Employee self-service profile & reference data
**User Story**: As a USER, I want to read reference data and my profile context, so that forms can be populated correctly.

#### Acceptance Criteria
1. WHEN a USER GETs `/user/roles` THEN the system SHALL return the available roles.
2. WHEN a USER GETs `/user/positions` THEN the system SHALL return the available positions.
3. WHEN a USER GETs `/user/levels` THEN the system SHALL return the available levels.
4. WHEN a USER GETs `/user/project` THEN the system SHALL return the projects the user is assigned to.

### Requirement 9 — Timesheet submission & query
**User Story**: As a USER, I want to submit and view my timesheets, so that my worked hours are recorded.

#### Acceptance Criteria
1. WHEN a USER POSTs a timesheet to `/user/timesheet` THEN the system SHALL persist it with status pending.
2. WHEN a USER PUTs `/user/timesheet` THEN the system SHALL update an existing timesheet the user owns.
3. The system SHALL compute working hours from start/end times via the work-hours calculator.
4. IF a timesheet overlaps an existing one for the same period THEN the system SHALL reject it with a localized error.
5. WHEN a USER queries timesheets THEN the system SHALL return only timesheets the user owns.

### Requirement 10 — Timesheet & day-off approval (manager)
**User Story**: As a PROJECT_MANAGER, I want to approve or reject timesheets and day-off requests, so that records are validated.

#### Acceptance Criteria
1. WHEN a manager PUTs `/manager/timesheet/approval` with a decision THEN the system SHALL set the timesheet status to approved or rejected.
2. WHEN a manager PUTs `/manager/dayoff/approval` with a decision THEN the system SHALL set the day-off status accordingly.
3. WHEN a manager GETs `/manager/timesheet` THEN the system SHALL return timesheets for the manager's scope.
4. IF the target record is already in a terminal status THEN the system SHALL reject re-approval with a localized error.
5. WHEN an approval decision is recorded THEN the system SHALL push an SSE notification to the affected user.
6. WHEN an approval decision is recorded THEN the system SHALL send an email notification to the affected user.

### Requirement 11 — Day-off requests
**User Story**: As a USER, I want to request day-off, so that my absences are tracked and approved.

#### Acceptance Criteria
1. WHEN a USER POSTs `/user/dayoff` with start/end times and type THEN the system SHALL create a pending day-off request.
2. IF the requested range overlaps an existing day-off for the user THEN the system SHALL reject it.
3. The system SHALL classify the day-off request by `EDayOffType`.
4. The system SHALL create the day-off request in `EDayOffStatus` pending.

### Requirement 12 — Request-a-device flow *(GAP: not yet implemented)*
**User Story**: As a USER, I want to request a device and have IT approve and assign it, so that I receive equipment through a tracked workflow.

#### Acceptance Criteria
1. WHEN a USER submits a device request THEN the system SHALL create a `RequestDeviceEntity` in `ERequestDeviceStatus` pending.
2. WHEN an `IT_ADMIN` approves a request THEN the system SHALL transition it to approved and assign an available device.
3. WHEN an `IT_ADMIN` rejects a request THEN the system SHALL transition it to rejected with a reason.
4. IF no device of the requested type is available THEN the system SHALL keep the request pending and surface the shortage.
5. WHEN a request's status changes THEN the system SHALL notify the requesting user.

### Requirement 13 — Project management (manager)
**User Story**: As a PROJECT_MANAGER, I want to manage projects, so that work and assignments are organized.

#### Acceptance Criteria
1. WHEN a manager POSTs `/manager/project` THEN the system SHALL create a project.
2. WHEN a manager PUTs `/manager/project/{id}` THEN the system SHALL update the project.
3. WHEN a manager DELETEs `/manager/project/{id}` THEN the system SHALL soft-delete the project.
4. WHEN a manager GETs `/manager/project` THEN the system SHALL return a paginated project list.
5. WHEN an ADMIN GETs `/admin/projects` THEN the system SHALL return all projects.

### Requirement 14 — Device CRUD
**User Story**: As an administrative user, I want to manage devices, so that the asset inventory is accurate.

#### Acceptance Criteria
1. WHEN an authorized user POSTs `/device` THEN the system SHALL create a device with type and status.
2. WHEN an authorized user PUTs `/device/{id}` THEN the system SHALL update the device.
3. WHEN an authorized user DELETEs `/device/{id}` THEN the system SHALL soft-delete the device.
4. WHEN any user GETs `/device/{id}` THEN the system SHALL return the device or `404`.
5. WHEN any user GETs `/device` THEN the system SHALL return a paginated/filterable device list.
6. IF a device write is attempted by a non-administrative role THEN the system SHALL respond `403 Forbidden`.

### Requirement 15 — Device-user assignment
**User Story**: As an administrative user, I want to assign and unassign users to a device, so that custody is tracked.

#### Acceptance Criteria
1. WHEN an authorized user POSTs user IDs to `/device/{id}/users` THEN the system SHALL set the device's assigned users.
2. WHEN an authorized user GETs `/device/{id}/users` THEN the system SHALL return the assigned users.
3. IF any supplied user ID does not exist THEN the system SHALL reject the request and name the invalid IDs.
4. The system SHALL resolve the user set in a single batched query (no N+1 per user).
5. IF the device was modified concurrently THEN the system SHALL fail with an optimistic-locking error (`@Version`) rather than overwrite.

### Requirement 16 — Optimistic locking
**User Story**: As the system owner, I want concurrent edits to fail safely, so that lost updates are prevented.

#### Acceptance Criteria
1. WHILE two transactions edit the same `DeviceEntity` or `UserEntity` THE system SHALL allow only the first commit to succeed.
2. WHEN a stale-version write is committed THEN the system SHALL raise an optimistic-locking exception.
3. The system SHALL increment the `@Version` column on each successful update.

### Requirement 17 — Soft delete
**User Story**: As the system owner, I want deletes to be recoverable and excluded from reads, so that data is not lost.

#### Acceptance Criteria
1. WHEN any entity is deleted THEN the system SHALL set its deleted timestamp instead of removing the row.
2. WHILE an entity is soft-deleted THE system SHALL exclude it from standard queries.
3. The system SHALL prevent re-deleting an already soft-deleted entity.

### Requirement 18 — Auditing
**User Story**: As the system owner, I want create/update auditing, so that record provenance is captured.

#### Acceptance Criteria
1. WHEN an auditable entity is persisted THEN the system SHALL stamp created/modified timestamps and actor.
2. IF no authenticated user is present THEN the system SHALL record actor id `0` (system).

### Requirement 19 — Holiday lookup
**User Story**: As a client, I want holiday data, so that scheduling reflects public holidays.

#### Acceptance Criteria
1. WHEN a client GETs `/holidays/year/{year}` THEN the system SHALL return that year's holidays.
2. WHEN a client GETs `/holidays/current` THEN the system SHALL return the current period's holidays.
3. WHEN a client GETs `/holidays/range` with start/end THEN the system SHALL return holidays in range.
4. WHEN a client GETs `/holidays/check` for a date THEN the system SHALL return whether it is a holiday.
5. The system SHALL source holiday data from Calendarific.
6. The system SHALL cache Calendarific responses in Redis.

### Requirement 20 — Admin dashboard
**User Story**: As an ADMIN, I want a summary dashboard, so that I can see headline metrics.

#### Acceptance Criteria
1. WHEN an ADMIN GETs `/admin/dashboard/summary` THEN the system SHALL return aggregate counts/metrics.
2. IF the caller is not an ADMIN THEN the system SHALL respond `403 Forbidden`.

### Requirement 21 — Real-time notifications (SSE)
**User Story**: As a logged-in user, I want a live event stream, so that I receive notifications without polling.

#### Acceptance Criteria
1. WHEN an authenticated client GETs `/sse/connect` THEN the system SHALL open a `text/event-stream` connection.
2. WHEN a domain event targets a user THEN the system SHALL push it to that user's open SSE connections.
3. WHEN a client GETs `/sse/connections/count` THEN the system SHALL return the active connection count.
4. WHILE a connection is idle beyond timeout THE system SHALL close it and release resources.

### Requirement 22 — Email & scheduled jobs
**User Story**: As the system owner, I want transactional email and scheduled notifications, so that users are kept informed.

#### Acceptance Criteria
1. WHEN a password reset or approval event occurs THEN the system SHALL send the corresponding email via SMTP.
2. WHEN the birthday schedule runs THEN the system SHALL identify users with birthdays today or upcoming.
3. WHEN the birthday schedule runs THEN the system SHALL send notifications to the identified users.
4. WHEN a USER GETs `/user/birthday/today` or `/user/birthday/upcoming` THEN the system SHALL return matching users.

### Requirement 23 — Internationalization
**User Story**: As a user, I want messages in my language, so that responses are understandable.

#### Acceptance Criteria
1. WHEN a request carries `Accept-Language: vi` THEN the system SHALL return message bodies from `messages_vi.properties`.
2. WHERE no language is specified THE system SHALL default to English (`messages.properties`).
3. The system SHALL localize both success and error message bodies.
