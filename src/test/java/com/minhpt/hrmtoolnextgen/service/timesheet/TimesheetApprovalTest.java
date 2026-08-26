package com.minhpt.hrmtoolnextgen.service.timesheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.request.ApprovalTimesheetRequest;
import com.minhpt.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import org.springframework.security.access.AccessDeniedException;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.TimesheetMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.TimesheetRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.service.EmailService;
import com.minhpt.hrmtoolnextgen.service.SseService;
import com.minhpt.hrmtoolnextgen.service.WorkHoursCalculatorService;
import com.minhpt.hrmtoolnextgen.support.Fixtures;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import java.util.Map;
import com.minhpt.hrmtoolnextgen.support.TransactionSyncTestSupport;
import org.junit.jupiter.api.AfterEach;

/**
 * Unit tests for TimesheetCommandService.approvalTimesheet only.
 *
 * R10.1 status transition, R10.4 terminal guards, R10.5 SSE push, R10.6 email.
 */
@ExtendWith(MockitoExtension.class)
class TimesheetApprovalTest {

    @Mock private TimesheetRepository timesheetRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private DayOffRepository dayOffRepository;
    @Mock private TimesheetMapping timesheetMapping;
    @Mock private WorkHoursCalculatorService workHoursCalculatorService;
    @Mock private MessageService messageService;
    @Mock private EmailService emailService;
    @Mock private SseService sseService;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private TimesheetCommandService commandService;

    private UserPrincipalDto managerPrincipal() {
        return com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto.internalBuilder()
                .id(1L)
                .email("manager@example.com")
                .authorities(java.util.List.of(EUserRole.PROJECT_MANAGER))
                .build();
    }

    @BeforeEach
    void setUp() {
        // Services defer SSE/email to afterCommit; open a synchronization scope so
        // registerSynchronization() succeeds outside a real transaction.
        TransactionSyncTestSupport.begin();
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(managerPrincipal());
    }

    @AfterEach
    void tearDown() {
        TransactionSyncTestSupport.end();
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // R10.1 — PENDING → APPROVED: status persisted, dto returned, SSE + email sent
    // -------------------------------------------------------------------------

    @Test
    void approvalTimesheet_pendingToApproved_savesAndReturnsDto() {
        TimesheetEntity entity = Fixtures.buildTimesheet(1L);
        entity.setId(1L); // afterCommit payload uses Map.of, which rejects a null id
        entity.setStatus(ETimesheetStatus.PENDING);
        UserEntity owner = Fixtures.buildUser(2L);
        owner.setId(2L);
        entity.setUserEntity(owner);

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(timesheetRepository.save(entity)).thenReturn(entity);

        TimesheetDto expected = new TimesheetDto();
        expected.setStatus(ETimesheetStatus.APPROVED);
        when(timesheetMapping.toDto(entity)).thenReturn(expected);

        ApprovalTimesheetRequest req = new ApprovalTimesheetRequest(1L, ETimesheetStatus.APPROVED);
        TimesheetDto result = commandService.approvalTimesheet(req);

        ArgumentCaptor<TimesheetEntity> captor = ArgumentCaptor.forClass(TimesheetEntity.class);
        verify(timesheetRepository).save(captor.capture());
        assertEquals(ETimesheetStatus.APPROVED, captor.getValue().getStatus());

        assertNotNull(result);
        assertEquals(ETimesheetStatus.APPROVED, result.getStatus());

        // R10.5 — SSE notification
        TransactionSyncTestSupport.triggerAfterCommit();
        verify(sseService).sendEvent(eq(String.valueOf(owner.getId())), eq("timesheet.approval"),
                argThat(data -> "APPROVED".equals(((Map<?, ?>) data).get("status"))));
        // R10.6 — email notification
        verify(emailService).sendApprovalNotificationEmail(
                eq(owner.getEmail()), anyString(), eq("Timesheet"), eq(ETimesheetStatus.APPROVED.name()));
    }

    // -------------------------------------------------------------------------
    // R10.1 — PENDING → REJECTED: status persisted, dto returned
    // -------------------------------------------------------------------------

    @Test
    void approvalTimesheet_pendingToRejected_savesAndReturnsDto() {
        TimesheetEntity entity = Fixtures.buildTimesheet(2L);
        entity.setId(2L); // afterCommit payload uses Map.of, which rejects a null id
        entity.setStatus(ETimesheetStatus.PENDING);
        UserEntity owner = Fixtures.buildUser(3L);
        owner.setId(3L);
        entity.setUserEntity(owner);

        when(timesheetRepository.findById(2L)).thenReturn(Optional.of(entity));
        when(timesheetRepository.save(entity)).thenReturn(entity);

        TimesheetDto expected = new TimesheetDto();
        expected.setStatus(ETimesheetStatus.REJECTED);
        when(timesheetMapping.toDto(entity)).thenReturn(expected);

        ApprovalTimesheetRequest req = new ApprovalTimesheetRequest(2L, ETimesheetStatus.REJECTED);
        TimesheetDto result = commandService.approvalTimesheet(req);

        ArgumentCaptor<TimesheetEntity> captor = ArgumentCaptor.forClass(TimesheetEntity.class);
        verify(timesheetRepository).save(captor.capture());
        assertEquals(ETimesheetStatus.REJECTED, captor.getValue().getStatus());

        assertNotNull(result);
        assertEquals(ETimesheetStatus.REJECTED, result.getStatus());

        TransactionSyncTestSupport.triggerAfterCommit();
        verify(sseService).sendEvent(anyString(), eq("timesheet.approval"),
                argThat(data -> "REJECTED".equals(((Map<?, ?>) data).get("status"))));
        verify(emailService).sendApprovalNotificationEmail(
                anyString(), anyString(), eq("Timesheet"), eq(ETimesheetStatus.REJECTED.name()));
    }

    // -------------------------------------------------------------------------
    // R10.4 — terminal APPROVED → BadRequestException("timesheet.cannot.change.status")
    // -------------------------------------------------------------------------

    @Test
    void approvalTimesheet_alreadyApproved_throwsBadRequestException() {
        TimesheetEntity entity = Fixtures.buildTimesheet(3L);
        entity.setStatus(ETimesheetStatus.APPROVED);

        when(timesheetRepository.findById(3L)).thenReturn(Optional.of(entity));
        when(messageService.getMessage("timesheet.cannot.change.status"))
                .thenReturn("Cannot change status");

        ApprovalTimesheetRequest req = new ApprovalTimesheetRequest(3L, ETimesheetStatus.REJECTED);

        assertThrows(BadRequestException.class, () -> commandService.approvalTimesheet(req));
        verify(timesheetRepository, never()).save(any());
        verify(sseService, never()).sendEvent(anyString(), anyString(), any());
        verify(emailService, never()).sendApprovalNotificationEmail(anyString(), anyString(), anyString(), anyString());
    }

    // -------------------------------------------------------------------------
    // R10.4 — terminal REJECTED → BadRequestException
    // -------------------------------------------------------------------------

    @Test
    void approvalTimesheet_alreadyRejected_throwsBadRequestException() {
        TimesheetEntity entity = Fixtures.buildTimesheet(4L);
        entity.setStatus(ETimesheetStatus.REJECTED);

        when(timesheetRepository.findById(4L)).thenReturn(Optional.of(entity));
        when(messageService.getMessage("timesheet.cannot.change.status"))
                .thenReturn("Cannot change status");

        ApprovalTimesheetRequest req = new ApprovalTimesheetRequest(4L, ETimesheetStatus.APPROVED);

        assertThrows(BadRequestException.class, () -> commandService.approvalTimesheet(req));
        verify(timesheetRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Null requested status on a PENDING timesheet → BadRequestException
    // -------------------------------------------------------------------------

    @Test
    void approvalTimesheet_pendingWithNullRequestedStatus_throwsBadRequestException() {
        TimesheetEntity entity = Fixtures.buildTimesheet(5L);
        entity.setStatus(ETimesheetStatus.PENDING);

        when(timesheetRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(messageService.getMessage("timesheet.status.not.found"))
                .thenReturn("Status not found");

        ApprovalTimesheetRequest req = new ApprovalTimesheetRequest(5L, null);

        assertThrows(BadRequestException.class, () -> commandService.approvalTimesheet(req));
        verify(timesheetRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Null id → BadRequestException (requireId guard)
    // -------------------------------------------------------------------------

    @Test
    void approvalTimesheet_nullId_throwsBadRequestException() {
        when(messageService.getMessage("not.null")).thenReturn("ID must not be null");

        ApprovalTimesheetRequest req = new ApprovalTimesheetRequest(null, ETimesheetStatus.APPROVED);

        assertThrows(BadRequestException.class, () -> commandService.approvalTimesheet(req));
        verify(timesheetRepository, never()).findById(any());
    }

    // -------------------------------------------------------------------------
    // Not-found id → NotFoundException
    // -------------------------------------------------------------------------

    @Test
    void approvalTimesheet_timesheetNotFound_throwsNotFoundException() {
        when(timesheetRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage("timesheet.not.found", 99L)).thenReturn("Not found");

        ApprovalTimesheetRequest req = new ApprovalTimesheetRequest(99L, ETimesheetStatus.APPROVED);

        assertThrows(NotFoundException.class, () -> commandService.approvalTimesheet(req));
        verify(timesheetRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Authorization: regular USER role → AccessDeniedException
    // -------------------------------------------------------------------------

    @Test
    void approvalTimesheet_regularUser_throwsAccessDeniedException() {
        com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto userPrincipal =
                com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto.internalBuilder()
                        .id(99L)
                        .email("user@example.com")
                        .authorities(java.util.List.of(EUserRole.USER))
                        .build();
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        // The authorization gate runs before the repository lookup, so no findById stub.
        when(messageService.getMessage("error.access.denied")).thenReturn("Denied");

        ApprovalTimesheetRequest req = new ApprovalTimesheetRequest(1L, ETimesheetStatus.APPROVED);

        assertThrows(AccessDeniedException.class, () -> commandService.approvalTimesheet(req));
        verify(timesheetRepository, never()).save(any());
        verify(sseService, never()).sendEvent(anyString(), anyString(), any());
        verify(emailService, never()).sendApprovalNotificationEmail(anyString(), anyString(), anyString(), anyString());
    }

    // -------------------------------------------------------------------------
    // R10.5 / R10.6 — SSE + email notifications on approved path
    // -------------------------------------------------------------------------

    @Test
    void approvalTimesheet_pendingToApproved_pushesSseAndSendsEmail() {
        TimesheetEntity entity = Fixtures.buildTimesheet(1L);
        entity.setId(1L); // afterCommit payload uses Map.of, which rejects a null id
        entity.setStatus(ETimesheetStatus.PENDING);
        UserEntity owner = Fixtures.buildUser(2L);
        owner.setId(2L);
        entity.setUserEntity(owner);

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(timesheetRepository.save(entity)).thenReturn(entity);

        TimesheetDto expected = new TimesheetDto();
        expected.setStatus(ETimesheetStatus.APPROVED);
        when(timesheetMapping.toDto(entity)).thenReturn(expected);

        ApprovalTimesheetRequest req = new ApprovalTimesheetRequest(1L, ETimesheetStatus.APPROVED);
        commandService.approvalTimesheet(req);

        // R10.5 — SSE event payload
        TransactionSyncTestSupport.triggerAfterCommit();
        verify(sseService).sendEvent(eq(String.valueOf(owner.getId())), eq("timesheet.approval"),
                argThat(data -> {
                    Map<?, ?> map = (Map<?, ?>) data;
                    return Long.valueOf(1L).equals(map.get("timesheetId"))
                            && "APPROVED".equals(map.get("status"));
                }));

        // R10.6 — email notification
        verify(emailService).sendApprovalNotificationEmail(
                eq(owner.getEmail()),
                anyString(),
                eq("Timesheet"),
                eq(ETimesheetStatus.APPROVED.name()));
    }
}
