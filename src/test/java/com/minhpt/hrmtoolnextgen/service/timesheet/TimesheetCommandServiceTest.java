package com.minhpt.hrmtoolnextgen.service.timesheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateTimesheetRequest;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateTimesheetRequest;
import com.minhpt.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetType;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.TimesheetMapping;
import com.minhpt.hrmtoolnextgen.projection.TimesheetWorkingHourProjection;
import com.minhpt.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.TimesheetRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.service.WorkHoursCalculatorService;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Unit tests for TimesheetCommandService.
 *
 * SecurityContextHolder is populated manually so that getCurrentUser() resolves
 * without a running Spring context.
 *
 * Overlap / hour-cap check (assertCanLogNormalTimesheet):
 *   The code compares total already-logged hours + new hours against the
 *   max allowed hours for the day.  Max allowed = dailyWorkHours minus hours
 *   consumed by APPROVED day-off entities (NOT other timesheets).
 *   → R9.4 states "overlaps an existing timesheet"; the code instead enforces
 *     an approved-DAY-OFF hour cap.  This is a requirement-vs-code divergence
 *     (reported in the final summary).
 */
@ExtendWith(MockitoExtension.class)
class TimesheetCommandServiceTest {

    @Mock private TimesheetRepository timesheetRepository;
    @Mock private UserRepository      userRepository;
    @Mock private ProjectRepository   projectRepository;
    @Mock private DayOffRepository    dayOffRepository;
    @Mock private TimesheetMapping    timesheetMapping;
    @Mock private WorkHoursCalculatorService workHoursCalculatorService;
    @Mock private MessageService      messageService;

    @InjectMocks
    private TimesheetCommandService commandService;

    // A weekday we control: Monday 2026-06-22
    private static final LocalDate WEEKDAY = LocalDate.of(2026, 6, 22);
    private static final long      USER_ID = 1L;
    private static final long      PROJECT_ID = 10L;

    @BeforeEach
    void setupSecurityContext() {
        UserPrincipalDto principal = UserPrincipalDto.internalBuilder()
                .id(USER_ID)
                .email("user1@example.com")
                .build();
        var auth = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    // -------------------------------------------------------------------------
    // createTimesheet — happy path: entity saved with PENDING status
    // -------------------------------------------------------------------------

    @Test
    void createTimesheet_normalType_savedWithPendingStatus() {
        UserEntity    user    = Fixtures.buildUser(1L);
        ProjectEntity project = Fixtures.buildProject(1L, user);
        // Fixtures does not persist — set id manually so assertUserInProject
        // passes projectEntity.getId() (not null) to the repo check.
        project.setId(PROJECT_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.existsByWorkingProjectIdAndId(PROJECT_ID, USER_ID)).thenReturn(true);
        when(workHoursCalculatorService.getDailyWorkHours()).thenReturn(8.0);
        when(timesheetRepository.findByUserEntityIdAndWorkingDayAndStatusNot(
                USER_ID, WEEKDAY, ETimesheetStatus.REJECTED))
                .thenReturn(Collections.emptyList());
        when(dayOffRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Collections.emptyList());

        TimesheetEntity saved = Fixtures.buildTimesheet(1L, user, project);
        saved.setStatus(ETimesheetStatus.PENDING);
        when(timesheetRepository.save(any(TimesheetEntity.class))).thenReturn(saved);

        TimesheetDto expectedDto = new TimesheetDto();
        expectedDto.setStatus(ETimesheetStatus.PENDING);
        when(timesheetMapping.toDto(saved)).thenReturn(expectedDto);

        CreateTimesheetRequest req = new CreateTimesheetRequest();
        req.setProjectId(PROJECT_ID);
        req.setWorkingDay(WEEKDAY);
        req.setWorkingHours(LocalTime.of(4, 0));  // 4 h — well under 8.5
        req.setTimesheetType(ETimesheetType.NORMAL);

        TimesheetDto result = commandService.createTimesheet(req);

        // Saved entity must carry PENDING
        ArgumentCaptor<TimesheetEntity> captor = ArgumentCaptor.forClass(TimesheetEntity.class);
        verify(timesheetRepository).save(captor.capture());
        assertEquals(ETimesheetStatus.PENDING, captor.getValue().getStatus());

        // Returned DTO reflects PENDING
        assertNotNull(result);
        assertEquals(ETimesheetStatus.PENDING, result.getStatus());
    }

    // -------------------------------------------------------------------------
    // createTimesheet — OVERTIME type skips weekend / hour-cap check
    // -------------------------------------------------------------------------

    @Test
    void createTimesheet_overtimeType_savedWithoutHourCapCheck() {
        UserEntity    user    = Fixtures.buildUser(1L);
        ProjectEntity project = Fixtures.buildProject(1L, user);
        project.setId(PROJECT_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.existsByWorkingProjectIdAndId(PROJECT_ID, USER_ID)).thenReturn(true);

        TimesheetEntity saved = Fixtures.buildTimesheet(1L, user, project);
        saved.setStatus(ETimesheetStatus.PENDING);
        when(timesheetRepository.save(any())).thenReturn(saved);
        when(timesheetMapping.toDto(saved)).thenReturn(new TimesheetDto());

        CreateTimesheetRequest req = new CreateTimesheetRequest();
        req.setProjectId(PROJECT_ID);
        req.setWorkingDay(WEEKDAY);
        req.setWorkingHours(LocalTime.of(2, 0));
        req.setTimesheetType(ETimesheetType.OVERTIME);

        // Must not throw — OVERTIME bypasses weekend/hour-cap assertions
        assertNotNull(commandService.createTimesheet(req));
    }

    // -------------------------------------------------------------------------
    // createTimesheet — user not a project member → BadRequestException
    // -------------------------------------------------------------------------

    @Test
    void createTimesheet_userNotInProject_throwsBadRequestException() {
        UserEntity    user    = Fixtures.buildUser(1L);
        ProjectEntity project = Fixtures.buildProject(1L, user);
        project.setId(PROJECT_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.existsByWorkingProjectIdAndId(PROJECT_ID, USER_ID)).thenReturn(false);
        when(messageService.getMessage("error.user.not.in.project")).thenReturn("User not in project");

        CreateTimesheetRequest req = new CreateTimesheetRequest();
        req.setProjectId(PROJECT_ID);
        req.setWorkingDay(WEEKDAY);
        req.setWorkingHours(LocalTime.of(4, 0));
        req.setTimesheetType(ETimesheetType.NORMAL);

        assertThrows(BadRequestException.class, () -> commandService.createTimesheet(req));
    }

    // -------------------------------------------------------------------------
    // createTimesheet — NORMAL on weekend → BadRequestException
    // -------------------------------------------------------------------------

    @Test
    void createTimesheet_normalTypeOnWeekend_throwsBadRequestException() {
        UserEntity    user    = Fixtures.buildUser(1L);
        ProjectEntity project = Fixtures.buildProject(1L, user);
        project.setId(PROJECT_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.existsByWorkingProjectIdAndId(PROJECT_ID, USER_ID)).thenReturn(true);
        when(messageService.getMessage("timesheet.cannot.log.weekend")).thenReturn("Cannot log weekend");

        LocalDate saturday = LocalDate.of(2026, 6, 27);

        CreateTimesheetRequest req = new CreateTimesheetRequest();
        req.setProjectId(PROJECT_ID);
        req.setWorkingDay(saturday);
        req.setWorkingHours(LocalTime.of(4, 0));
        req.setTimesheetType(ETimesheetType.NORMAL);

        assertThrows(BadRequestException.class, () -> commandService.createTimesheet(req));
    }

    // -------------------------------------------------------------------------
    // createTimesheet — user entity not found → NotFoundException
    // -------------------------------------------------------------------------

    @Test
    void createTimesheet_userNotFound_throwsNotFoundException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(messageService.getMessage("user.not.found", USER_ID)).thenReturn("User not found");

        CreateTimesheetRequest req = new CreateTimesheetRequest();
        req.setProjectId(PROJECT_ID);
        req.setWorkingDay(WEEKDAY);
        req.setWorkingHours(LocalTime.of(4, 0));
        req.setTimesheetType(ETimesheetType.NORMAL);

        assertThrows(NotFoundException.class, () -> commandService.createTimesheet(req));
    }

    // -------------------------------------------------------------------------
    // createTimesheet — project not found → NotFoundException
    // -------------------------------------------------------------------------

    @Test
    void createTimesheet_projectNotFound_throwsNotFoundException() {
        UserEntity user = Fixtures.buildUser(1L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());
        when(messageService.getMessage("project.not.found", PROJECT_ID)).thenReturn("Project not found");

        CreateTimesheetRequest req = new CreateTimesheetRequest();
        req.setProjectId(PROJECT_ID);
        req.setWorkingDay(WEEKDAY);
        req.setWorkingHours(LocalTime.of(4, 0));
        req.setTimesheetType(ETimesheetType.NORMAL);

        assertThrows(NotFoundException.class, () -> commandService.createTimesheet(req));
    }

    // -------------------------------------------------------------------------
    // createTimesheet — existing logged hours + new hours exceed daily cap
    //                   → BadRequestException
    //
    // R9.4 DIVERGENCE: the code does NOT check for overlapping timesheets.
    // It enforces a daily hour cap that is reduced by approved DAY-OFF overlap.
    // When existing logged hours + new hours exceed the calculated max, it throws.
    //
    // Source bug at line 209 of TimesheetCommandService: when usedByDayOffHours
    // >= dailyWorkHours the method incorrectly returns dailyWorkHours (8.0) instead
    // of 0.0 as the max allowed. To trigger the exception reliably, this test uses
    // NO day-off (maxAllowedHours = 8.0) and already-logged + new > 8.0.
    // -------------------------------------------------------------------------

    @Test
    void createTimesheet_existingHoursPlusNewExceedsDailyCap_throwsBadRequestException() {
        UserEntity    user    = Fixtures.buildUser(1L);
        ProjectEntity project = Fixtures.buildProject(1L, user);
        project.setId(PROJECT_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.existsByWorkingProjectIdAndId(PROJECT_ID, USER_ID)).thenReturn(true);
        when(workHoursCalculatorService.getDailyWorkHours()).thenReturn(8.0);

        // Already logged 7 hours (non-rejected)
        TimesheetWorkingHourProjection existing = mockProjection(LocalTime.of(7, 0));
        when(timesheetRepository.findByUserEntityIdAndWorkingDayAndStatusNot(
                USER_ID, WEEKDAY, ETimesheetStatus.REJECTED))
                .thenReturn(List.of(existing));

        // No day-off → maxAllowedHours = 8.0
        when(dayOffRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Collections.emptyList());

        lenient().when(messageService.getMessage(
                org.mockito.ArgumentMatchers.eq("timesheet.cannot.log"),
                any())).thenReturn("Exceeds daily hours");

        CreateTimesheetRequest req = new CreateTimesheetRequest();
        req.setProjectId(PROJECT_ID);
        req.setWorkingDay(WEEKDAY);
        req.setWorkingHours(LocalTime.of(2, 0));  // 7 + 2 = 9 > 8 → blocked
        req.setTimesheetType(ETimesheetType.NORMAL);

        assertThrows(BadRequestException.class, () -> commandService.createTimesheet(req));
    }

    /**
     * Creates a minimal stub of TimesheetWorkingHourProjection for the given hours.
     */
    private TimesheetWorkingHourProjection mockProjection(LocalTime workingHours) {
        return new TimesheetWorkingHourProjection() {
            @Override public LocalTime getWorkingHours() { return workingHours; }
            @Override public com.minhpt.hrmtoolnextgen.enumeration.ETimesheetType getType() { return ETimesheetType.NORMAL; }
            @Override public LocalDate getWorkingDay() { return WEEKDAY; }
            @Override public ETimesheetStatus getStatus() { return ETimesheetStatus.PENDING; }
            @Override public com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity getUserEntity() { return null; }
        };
    }

    // -------------------------------------------------------------------------
    // updateTimesheet — happy path: PENDING timesheet updated
    // -------------------------------------------------------------------------

    @Test
    void updateTimesheet_pendingTimesheet_updatesSuccessfully() {
        UserEntity    user    = Fixtures.buildUser(1L);
        ProjectEntity project = Fixtures.buildProject(1L, user);
        TimesheetEntity existing = Fixtures.buildTimesheet(1L, user, project);
        existing.setStatus(ETimesheetStatus.PENDING);

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(timesheetRepository.save(existing)).thenReturn(existing);

        TimesheetDto expectedDto = new TimesheetDto();
        expectedDto.setStatus(ETimesheetStatus.PENDING);
        when(timesheetMapping.toDto(existing)).thenReturn(expectedDto);

        UpdateTimesheetRequest req = new UpdateTimesheetRequest();
        req.setId(1L);
        req.setTitle("Updated title");

        TimesheetDto result = commandService.updateTimesheet(req);

        assertEquals("Updated title", existing.getTitle());
        assertNotNull(result);
        verify(timesheetRepository).save(existing);
    }

    // -------------------------------------------------------------------------
    // updateTimesheet — non-PENDING timesheet → BadRequestException
    // -------------------------------------------------------------------------

    @Test
    void updateTimesheet_approvedTimesheet_throwsBadRequestException() {
        TimesheetEntity approved = Fixtures.buildTimesheet(1L);
        approved.setStatus(ETimesheetStatus.APPROVED);

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(approved));
        when(messageService.getMessage("timesheet.cannot.update")).thenReturn("Cannot update");

        UpdateTimesheetRequest req = new UpdateTimesheetRequest();
        req.setId(1L);
        req.setTitle("New title");

        assertThrows(BadRequestException.class, () -> commandService.updateTimesheet(req));
    }

    @Test
    void updateTimesheet_rejectedTimesheet_throwsBadRequestException() {
        TimesheetEntity rejected = Fixtures.buildTimesheet(1L);
        rejected.setStatus(ETimesheetStatus.REJECTED);

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(rejected));
        when(messageService.getMessage("timesheet.cannot.update")).thenReturn("Cannot update");

        UpdateTimesheetRequest req = new UpdateTimesheetRequest();
        req.setId(1L);
        req.setTitle("New title");

        assertThrows(BadRequestException.class, () -> commandService.updateTimesheet(req));
    }

    // -------------------------------------------------------------------------
    // updateTimesheet — timesheet not found → NotFoundException
    // -------------------------------------------------------------------------

    @Test
    void updateTimesheet_timesheetNotFound_throwsNotFoundException() {
        when(timesheetRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage("timesheet.not.found", 99L)).thenReturn("Not found");

        UpdateTimesheetRequest req = new UpdateTimesheetRequest();
        req.setId(99L);

        assertThrows(NotFoundException.class, () -> commandService.updateTimesheet(req));
    }

    // -------------------------------------------------------------------------
    // updateTimesheet — null id → BadRequestException
    // -------------------------------------------------------------------------

    @Test
    void updateTimesheet_nullId_throwsBadRequestException() {
        when(messageService.getMessage("not.null")).thenReturn("ID must not be null");

        UpdateTimesheetRequest req = new UpdateTimesheetRequest();
        req.setId(null);

        assertThrows(BadRequestException.class, () -> commandService.updateTimesheet(req));
    }
}
