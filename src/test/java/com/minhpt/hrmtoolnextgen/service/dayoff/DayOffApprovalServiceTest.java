package com.minhpt.hrmtoolnextgen.service.dayoff;

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
import com.minhpt.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.ApprovalDayOffRequest;
import com.minhpt.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.mapping.DayOffMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Unit tests for DayOffApprovalService.approveDayOffRequest.
 *
 * STATUS TRANSITIONS (verified from source):
 *   PENDING → APPROVED  : sets status, decidedAt, decidedBy, saves.
 *   PENDING → REJECTED  : sets status, decidedAt, decidedBy, saves.
 *   non-PENDING (any terminal status) → BadRequestException("dayoff.already.processed")
 *
 * NOTIFICATION DIVERGENCE (R10.5 SSE / R10.6 email):
 *   DayOffApprovalService has NO SSE service or EmailService collaborator.
 *   Neither side-effect is present in the implementation.
 *   The @Disabled test at the bottom documents the desired behavior.
 */
@ExtendWith(MockitoExtension.class)
class DayOffApprovalServiceTest {

    @Mock private DayOffRepository dayOffRepository;
    @Mock private UserRepository   userRepository;
    @Mock private MessageService   messageService;
    @Mock private DayOffMapping    dayOffMapping;

    @InjectMocks
    private DayOffApprovalService approvalService;

    private static final long MANAGER_ID = 10L;
    private static final long REQUEST_ID = 20L;

    private UserPrincipalDto managerPrincipal() {
        return UserPrincipalDto.internalBuilder()
                .id(MANAGER_ID)
                .email("manager@example.com")
                .build();
    }

    // -------------------------------------------------------------------------
    // R10.2 — PENDING → APPROVED: status set, decidedAt/decidedBy populated,
    //         save invoked, dto returned
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_pendingToApproved_savesWithApprovedStatus() {
        UserEntity manager   = Fixtures.buildUser(MANAGER_ID);
        DayOffEntity dayOff  = Fixtures.buildDayOff(REQUEST_ID, Fixtures.buildUser(5L));
        dayOff.setStatus(EDayOffStatus.PENDING);

        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(dayOffRepository.save(dayOff)).thenReturn(dayOff);

        DayOffDto expected = DayOffDto.builder()
                .requestId(REQUEST_ID)
                .status(EDayOffStatus.APPROVED)
                .build();
        when(dayOffMapping.toDto(dayOff)).thenReturn(expected);

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);
        DayOffDto result = approvalService.approveDayOffRequest(req, managerPrincipal());

        ArgumentCaptor<DayOffEntity> captor = ArgumentCaptor.forClass(DayOffEntity.class);
        verify(dayOffRepository).save(captor.capture());

        DayOffEntity saved = captor.getValue();
        assertEquals(EDayOffStatus.APPROVED, saved.getStatus());
        assertNotNull(saved.getDecidedAt());
        assertEquals(manager, saved.getDecidedBy());

        assertNotNull(result);
        assertEquals(EDayOffStatus.APPROVED, result.getStatus());
    }

    // -------------------------------------------------------------------------
    // R10.2 — PENDING → REJECTED: status set, decidedAt/decidedBy populated
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_pendingToRejected_savesWithRejectedStatus() {
        UserEntity manager  = Fixtures.buildUser(MANAGER_ID);
        DayOffEntity dayOff = Fixtures.buildDayOff(REQUEST_ID + 1, Fixtures.buildUser(6L));
        dayOff.setStatus(EDayOffStatus.PENDING);

        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID + 1)).thenReturn(Optional.of(dayOff));
        when(dayOffRepository.save(dayOff)).thenReturn(dayOff);

        DayOffDto expected = DayOffDto.builder()
                .requestId(REQUEST_ID + 1)
                .status(EDayOffStatus.REJECTED)
                .build();
        when(dayOffMapping.toDto(dayOff)).thenReturn(expected);

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID + 1, EDayOffStatus.REJECTED);
        DayOffDto result = approvalService.approveDayOffRequest(req, managerPrincipal());

        ArgumentCaptor<DayOffEntity> captor = ArgumentCaptor.forClass(DayOffEntity.class);
        verify(dayOffRepository).save(captor.capture());

        assertEquals(EDayOffStatus.REJECTED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getDecidedAt());

        assertEquals(EDayOffStatus.REJECTED, result.getStatus());
    }

    // -------------------------------------------------------------------------
    // R10.4 — already APPROVED (terminal) → BadRequestException("dayoff.already.processed")
    //
    // Code guards: if (status != PENDING) throw BadRequestException — applies to
    // APPROVED, REJECTED, or any other non-PENDING value.
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_alreadyApproved_throwsBadRequestException() {
        UserEntity manager  = Fixtures.buildUser(MANAGER_ID);
        DayOffEntity dayOff = Fixtures.buildDayOff(REQUEST_ID, Fixtures.buildUser(7L));
        dayOff.setStatus(EDayOffStatus.APPROVED);

        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(messageService.getMessage("dayoff.already.processed"))
                .thenReturn("Day off request already processed");

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.REJECTED);

        assertThrows(BadRequestException.class,
                () -> approvalService.approveDayOffRequest(req, managerPrincipal()));
        verify(dayOffRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // R10.4 — already REJECTED (terminal) → BadRequestException("dayoff.already.processed")
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_alreadyRejected_throwsBadRequestException() {
        UserEntity manager  = Fixtures.buildUser(MANAGER_ID);
        DayOffEntity dayOff = Fixtures.buildDayOff(REQUEST_ID + 2, Fixtures.buildUser(8L));
        dayOff.setStatus(EDayOffStatus.REJECTED);

        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID + 2)).thenReturn(Optional.of(dayOff));
        when(messageService.getMessage("dayoff.already.processed"))
                .thenReturn("Day off request already processed");

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID + 2, EDayOffStatus.APPROVED);

        assertThrows(BadRequestException.class,
                () -> approvalService.approveDayOffRequest(req, managerPrincipal()));
        verify(dayOffRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // day-off not found → BadRequestException("dayoff.not.found", id)
    // (code uses BadRequestException, not NotFoundException, for missing day-off)
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_dayOffNotFound_throwsBadRequestException() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);

        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage("dayoff.not.found", 999L)).thenReturn("Day off not found");

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(999L, EDayOffStatus.APPROVED);

        assertThrows(BadRequestException.class,
                () -> approvalService.approveDayOffRequest(req, managerPrincipal()));
        verify(dayOffRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Deciding user not found → BadRequestException("user.not.found", id)
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_managerNotFound_throwsBadRequestException() {
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.empty());
        when(messageService.getMessage("user.not.found", MANAGER_ID)).thenReturn("User not found");

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);

        assertThrows(BadRequestException.class,
                () -> approvalService.approveDayOffRequest(req, managerPrincipal()));
    }

    // -------------------------------------------------------------------------
    // KNOWN GAP R10.5 / R10.6 — day-off approval path does not push SSE or
    // send email.
    //
    // DayOffApprovalService has no SSE service or EmailService collaborator.
    // This is the same gap as on the timesheet path.
    // Re-enable and assert the collaborator once notification is wired in.
    // -------------------------------------------------------------------------

    @org.junit.jupiter.api.Disabled(
            "KNOWN GAP: day-off approval path does not push SSE (R10.5) / send email (R10.6) — " +
            "DayOffApprovalService has no notification collaborator. " +
            "Re-enable and implement the assertion once notification is wired in.")
    @Test
    void approveDayOffRequest_pendingToApproved_pushesSseAndSendsEmail() {
        throw new AssertionError("SSE / email notification not yet implemented on day-off approval path");
    }
}
