package com.minhpt.hrmtoolnextgen.service.timesheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.request.ApprovalTimesheetRequest;
import com.minhpt.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.TimesheetMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.TimesheetRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.service.WorkHoursCalculatorService;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Unit tests for TimesheetCommandService.approvalTimesheet only.
 *
 * Create/update tests live in TimesheetCommandServiceTest — not duplicated here.
 *
 * NOTIFICATION DIVERGENCE (R10.5 SSE / R10.6 email):
 *   approvalTimesheet does NOT invoke any SSE service or EmailService.
 *   The TimesheetCommandService class has no such collaborator.
 *   The @Disabled test below documents the desired behavior and will auto-pass
 *   once notification is wired in.
 */
@ExtendWith(MockitoExtension.class)
class TimesheetApprovalTest {

    @Mock private TimesheetRepository       timesheetRepository;
    @Mock private UserRepository            userRepository;
    @Mock private ProjectRepository         projectRepository;
    @Mock private DayOffRepository          dayOffRepository;
    @Mock private TimesheetMapping          timesheetMapping;
    @Mock private WorkHoursCalculatorService workHoursCalculatorService;
    @Mock private MessageService            messageService;

    @InjectMocks
    private TimesheetCommandService commandService;

    // -------------------------------------------------------------------------
    // R10.1 — PENDING → APPROVED: status persisted, dto returned
    // -------------------------------------------------------------------------

    @Test
    void approvalTimesheet_pendingToApproved_savesAndReturnsDto() {
        TimesheetEntity entity = Fixtures.buildTimesheet(1L);
        entity.setStatus(ETimesheetStatus.PENDING);

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
    }

    // -------------------------------------------------------------------------
    // R10.1 — PENDING → REJECTED: status persisted, dto returned
    // -------------------------------------------------------------------------

    @Test
    void approvalTimesheet_pendingToRejected_savesAndReturnsDto() {
        TimesheetEntity entity = Fixtures.buildTimesheet(2L);
        entity.setStatus(ETimesheetStatus.PENDING);

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
    }

    // -------------------------------------------------------------------------
    // R10.4 — terminal REJECTED → BadRequestException("timesheet.cannot.change.status")
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
    // (message key: "timesheet.status.not.found")
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
    // Null id → BadRequestException (requireId guard, message key: "not.null")
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
    // KNOWN GAP R10.5 / R10.6 — approval path does not push SSE or send email.
    //
    // TimesheetCommandService has no SSE or EmailService collaborator.
    // This test documents the DESIRED side-effect and is disabled until
    // notification is added to the approval path.
    // -------------------------------------------------------------------------

    @org.junit.jupiter.api.Disabled(
            "KNOWN GAP: approval path does not push SSE (R10.5) / send email (R10.6) — " +
            "no notification collaborator is invoked in TimesheetCommandService.approvalTimesheet. " +
            "Re-enable and implement the assertion once notification is wired in.")
    @Test
    void approvalTimesheet_pendingToApproved_pushesSseAndSendsEmail() {
        // Desired: after a successful approval, an SSE event is broadcast to the
        // timesheet owner AND a confirmation email is dispatched.
        // Currently neither collaborator exists in TimesheetCommandService — test
        // cannot assert them without modifying src/main.
        throw new AssertionError("SSE / email notification not yet implemented on approval path");
    }
}
