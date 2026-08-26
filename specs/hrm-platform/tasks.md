# Implementation Plan

Scope: author the automated test suite that validates requirements R1–R23 against
the as-implemented system (see `design.md` Testing Strategy). Every task writes or
extends test code under `src/test/java/com/minhpt/hrmtoolnextgen/...`, mirroring the
`src/main` package layout and reusing the existing test DB/config + `@AfterEach`
FK-ordered cleanup. External systems (Calendarific, SMTP, S3) are mocked; Redis
behaviour uses the existing integration harness. R12 (request-a-device) is excluded —
no implementation to test.

Legend: existing tests are extended, not duplicated (`DeviceControllerAuthorizationTest`,
`*FetchPlanTest`, `OptimisticLockingTest`, `RateLimit*`, `LegacyApiDeprecation*`,
`Swagger*`, `Jwt*`).

- [x] 1. Test fixtures & shared harness
  - [x] 1.1 Test data builders for User/UserInfo, Device, Project, Timesheet, DayOff, Role
    - `src/test/java/.../support/Fixtures.java` (or per-domain builders); reuse FK-ordered cleanup from `OptimisticLockingTest`
    - _Requirements: 7.1, 14.1, 13.1, 9.1, 11.1_
  - [x] 1.2 MockMvc + security test helper (authenticate as role, attach JWT)
    - `src/test/java/.../support/MockMvcAuth.java`; issue tokens for ADMIN/IT_ADMIN/PROJECT_MANAGER/USER/HR
    - _Requirements: 4.1, 4.2, 4.3_

- [x] 2. Authentication & session tests
  - [x] 2.1 `service/auth/AuthServiceTest` — login success/invalid/disabled, token issue
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - [x] 2.2 `service/auth/AuthSessionServiceTest` — Redis session create/invalidate, post-logout rejection
    - _Requirements: 2.3, 2.4_
  - [x] 2.3 `controller/AuthControllerIntegrationTest` — `/auth/login|refresh|logout` contracts, 401 paths, localized bodies
    - _Requirements: 1.1, 1.2, 1.5, 2.1, 2.2, 2.3_
  - [x] 2.4 `service/auth/AuthAccountServiceTest` — forgot/reset password, token expiry, account-non-disclosure
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 3. Authorization & security matrix tests
  - [x] 3.1 `controller/AuthorizationMatrixTest` — per-endpoint role matrix (401 unauth, 403 wrong role) across admin/manager/user/device
    - extends `DeviceControllerAuthorizationTest`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
  - [x] 3.2 `component/jwt/JwtAuthTokenFilterTest` — extend: expired/malformed/missing token → 401
    - _Requirements: 4.1, 4.2_

- [x] 4. Rate limiting & API versioning tests
  - [x] 4.1 Extend `component/RateLimitingAspectTest` + `config/actuator/RateLimitMetricsIntegrationTest` — 429 on breach, metric recorded, Redis-backed counter
    - _Requirements: 5.1, 5.2, 5.3_
  - [x] 4.2 Extend `config/interceptor/LegacyApiDeprecation*` — headers present on `/api/*`, absent on `/api/v1/*`, sunset date asserted
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 5. User administration tests
  - [x] 5.1 `service/user/UserServiceTest` — create, update, soft-delete, password reset, uniqueness conflict (identity card/phone)
    - _Requirements: 7.1, 7.3, 7.4, 7.5, 7.7, 17.1, 17.3_
  - [x] 5.2 `controller/AdminControllerIntegrationTest` — multipart create, get/404, list pagination, admin-only gating
    - _Requirements: 7.1, 7.2, 7.6, 4.4_
  - [x] 5.3 `service/user/UserServiceReferenceDataTest` — roles/positions/levels, user projects
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 6. Timesheet tests
  - [x] 6.1 `service/timesheet/WorkHoursCalculatorServiceTest` — hours from start/end, edge cases
    - _Requirements: 9.3_
  - [x] 6.2 `service/timesheet/TimesheetCommandServiceTest` — create pending, update own, overlap rejection
    - _Requirements: 9.1, 9.2, 9.4_
  - [x] 6.3 `service/timesheet/TimesheetQueryServiceTest` — owner-scoped reads, manager-scoped reads
    - _Requirements: 9.5, 10.3_
  - [x] 6.4 `controller/UserTimesheetIntegrationTest` — POST/PUT `/user/timesheet` contracts
    - _Requirements: 9.1, 9.2_

- [x] 7. Approval (manager) tests
  - [x] 7.1 `service/timesheet/TimesheetApprovalTest` — approve/reject transitions, terminal-status re-approval rejected
    - _Requirements: 10.1, 10.4_
  - [x] 7.2 `service/dayoff/DayOffApprovalServiceTest` — approve/reject transitions
    - _Requirements: 10.2, 10.4_
  - [x] 7.3 `controller/ManagerControllerIntegrationTest` — approval endpoints + manager-only gating, notification side-effect verified (mocked SSE/email)
    - _Requirements: 10.1, 10.2, 10.5, 10.6, 4.5_

- [x] 8. Day-off request tests
  - [x] 8.1 `service/dayoff/DayOffCommandServiceTest` — create pending, overlap rejection, type/status classification
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

- [x] 9. Project tests
  - [x] 9.1 `service/project/ProjectCommandServiceTest` — create, update, soft-delete
    - _Requirements: 13.1, 13.2, 13.3, 17.1_
  - [x] 9.2 `service/project/ProjectQueryServiceTest` — paginated list, by-user
    - extends `repository/ProjectRepositoryFetchPlanTest`
    - _Requirements: 13.4, 8.4_
  - [x] 9.3 `controller/ManagerProjectIntegrationTest` + admin `/admin/projects` — contracts + gating
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

- [x] 10. Device tests (extend existing)
  - [x] 10.1 `service/device/DeviceCommandServiceTest` — create/update/soft-delete, soft-delete guard
    - _Requirements: 14.1, 14.2, 14.3, 17.3_
  - [x] 10.2 Extend `service/device/DeviceCommandServiceResolveUsersTest`/`ManageUsersBatchTest` — invalid-ID naming, single batched query
    - _Requirements: 15.1, 15.3, 15.4_
  - [x] 10.3 `service/device/DeviceQueryServiceTest` — get/404, list/filter
    - _Requirements: 14.4, 14.5_
  - [x] 10.4 Extend `controller/DeviceControllerAuthorizationTest` — write/assignment 403 for non-admin, assignment read
    - _Requirements: 14.6, 15.1, 15.2_
  - [x] 10.5 Extend `repository/OptimisticLockingTest` — assignment-under-concurrency conflict (Device + User `@Version`)
    - _Requirements: 15.5, 16.1, 16.2, 16.3_

- [x] 11. Persistence cross-cutting tests
  - [x] 11.1 `repository/SoftDeleteExclusionTest` — soft-deleted rows excluded from standard reads across entities
    - _Requirements: 17.1, 17.2_
  - [x] 11.2 `config/AuditorAwareTest` — created/modified stamps; actor `0L` when unauthenticated
    - _Requirements: 18.1, 18.2_

- [x] 12. Holiday tests
  - [x] 12.1 `service/HolidayServiceTest` — year/current/range/check with mocked Calendarific, Redis cache hit/miss
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 19.6_
  - [x] 12.2 `controller/HolidayControllerIntegrationTest` — four endpoints' contracts
    - _Requirements: 19.1, 19.2, 19.3, 19.4_

- [x] 13. Dashboard tests
  - [x] 13.1 `service/DashboardServiceTest` — summary aggregation
    - _Requirements: 20.1_
  - [x] 13.2 `controller/DashboardControllerIntegrationTest` — admin-only `/admin/dashboard/summary`, 403 otherwise
    - _Requirements: 20.1, 20.2_

- [x] 14. SSE & notification tests
  - [x] 14.1 `service/SseServiceTest` — register emitter, push to user, connection count, timeout cleanup
    - _Requirements: 21.2, 21.3, 21.4_
  - [x] 14.2 `controller/SseControllerIntegrationTest` — `/sse/connect` opens event-stream, count endpoint
    - _Requirements: 21.1, 21.3_

- [x] 15. Email & scheduled job tests
  - [x] 15.1 `service/EmailServiceTest` — reset/approval emails rendered + dispatched via mocked SMTP
    - _Requirements: 22.1_
  - [x] 15.2 `service/user/UserBirthdayServiceTest` — birthday schedule identifies + notifies; today/upcoming queries
    - _Requirements: 22.2, 22.3, 22.4_

- [x] 16. i18n tests
  - [x] 16.1 `config/I18nMessageTest` — `Accept-Language: vi` → vi bodies, default en, error + success localized
    - _Requirements: 23.1, 23.2, 23.3_

- [x] 17. Suite hardening
  - [x] 17.1 Fix flakiness and FK-ordering in `@AfterEach` cleanup across new tests (e.g. `users_roles` before `users`)
    - touch the new test classes; mirror `OptimisticLockingTest` ordering
    - _Requirements: 17.1, 17.2, 18.1, 18.2_
  - [x] 17.2 Update Maven Surefire config to keep external systems mocked (no live Calendarific/SMTP/S3 calls)
    - `pom.xml` Surefire config / test profile
    - _Requirements: 19.5, 19.6, 22.1_
