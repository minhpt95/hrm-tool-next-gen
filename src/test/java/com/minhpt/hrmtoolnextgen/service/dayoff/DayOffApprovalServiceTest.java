package com.minhpt.hrmtoolnextgen.service.dayoff;

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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.ApprovalDayOffRequest;
import com.minhpt.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import org.springframework.security.access.AccessDeniedException;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.mapping.DayOffMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.service.EmailService;
import com.minhpt.hrmtoolnextgen.service.SseService;
import com.minhpt.hrmtoolnextgen.support.Fixtures;
import java.util.Map;
import com.minhpt.hrmtoolnextgen.support.TransactionSyncTestSupport;
import org.junit.jupiter.api.AfterEach;

/**
 * Unit tests for DayOffApprovalService.approveDayOffRequest.
 *
 * STATUS TRANSITIONS (verified from source):
 * PENDING → APPROVED : sets status, decidedAt, decidedBy, saves, sends SSE + email.
 * PENDING → REJECTED : sets status, decidedAt, decidedBy, saves, sends SSE + email.
 * non-PENDING (any terminal status) → BadRequestException("dayoff.already.processed")
 */
@ExtendWith(MockitoExtension.class)
class DayOffApprovalServiceTest {

    @Mock private DayOffRepository dayOffRepository;
    @Mock private UserRepository userRepository;
    @Mock private MessageService messageService;
    @Mock private DayOffMapping dayOffMapping;
    @Mock private SseService sseService;
    @Mock private EmailService emailService;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private DayOffApprovalService approvalService;

    private static final long MANAGER_ID = 10L;
    private static final long REQUEST_ID = 20L;
    private static final long REQUESTER_ID = 5L;

    private UserPrincipalDto managerPrincipal() {
        return UserPrincipalDto.internalBuilder()
                .id(MANAGER_ID)
                .email("manager@example.com")
                .authorities(List.of(EUserRole.PROJECT_MANAGER))
                .build();
    }

    private UserPrincipalDto regularUserPrincipal() {
        return UserPrincipalDto.internalBuilder()
                .id(99L)
                .email("user@example.com")
                .authorities(List.of(EUserRole.USER))
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
    // R10.2 — PENDING → APPROVED: status set, decidedAt/decidedBy populated,
    // save invoked, SSE + email sent, dto returned
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_pendingToApproved_savesWithApprovedStatus() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        DayOffEntity dayOff = Fixtures.buildDayOff(REQUEST_ID, Fixtures.buildUser(5L));
        dayOff.setStatus(EDayOffStatus.PENDING);
        dayOff.setId(REQUEST_ID); // afterCommit payload uses Map.of, which rejects a null id

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

        // Verify SSE notification sent
        TransactionSyncTestSupport.triggerAfterCommit();
        verify(sseService).sendEvent(anyString(), eq("dayoff.approval"),
                argThat(data -> {
                    Map<?, ?> map = (Map<?, ?>) data;
                    return Long.valueOf(REQUEST_ID).equals(map.get("dayOffId"))
                            && EDayOffStatus.APPROVED.name().equals(map.get("status"));
                }));
        // Verify email notification sent
        verify(emailService).sendApprovalNotificationEmail(anyString(), anyString(), eq("Day Off Request"),
                eq(EDayOffStatus.APPROVED.name()));
    }

    // -------------------------------------------------------------------------
    // R10.2 — PENDING → REJECTED: status set, decidedAt/decidedBy populated
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_pendingToRejected_savesWithRejectedStatus() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        DayOffEntity dayOff = Fixtures.buildDayOff(REQUEST_ID + 1, Fixtures.buildUser(6L));
        dayOff.setStatus(EDayOffStatus.PENDING);
        dayOff.setId(REQUEST_ID); // afterCommit payload uses Map.of, which rejects a null id

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

        // Verify SSE notification sent with REJECTED status
        TransactionSyncTestSupport.triggerAfterCommit();
        verify(sseService).sendEvent(anyString(), eq("dayoff.approval"),
                argThat(data -> EDayOffStatus.REJECTED.name()
                        .equals(((Map<?, ?>) data).get("status"))));
        verify(emailService).sendApprovalNotificationEmail(anyString(), anyString(), eq("Day Off Request"),
                eq(EDayOffStatus.REJECTED.name()));
    }

    // -------------------------------------------------------------------------
    // R10.4 — already APPROVED (terminal) → BadRequestException("dayoff.already.processed")
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_alreadyApproved_throwsBadRequestException() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
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
        verify(sseService, never()).sendEvent(anyString(), anyString(), any());
        verify(emailService, never()).sendApprovalNotificationEmail(anyString(), anyString(), anyString(), anyString());
    }

    // -------------------------------------------------------------------------
    // R10.4 — already REJECTED (terminal) → BadRequestException("dayoff.already.processed")
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_alreadyRejected_throwsBadRequestException() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
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
    // Authorization: regular USER role → AccessDeniedException
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_regularUser_throwsAccessDeniedException() {
        DayOffEntity dayOff = Fixtures.buildDayOff(REQUEST_ID, Fixtures.buildUser(5L));
        dayOff.setStatus(EDayOffStatus.PENDING);
        dayOff.setId(REQUEST_ID); // afterCommit payload uses Map.of, which rejects a null id

        when(authentication.getPrincipal()).thenReturn(regularUserPrincipal());
        // The decider is resolved before the authorization gate, so it must resolve.
        when(userRepository.findById(99L)).thenReturn(Optional.of(Fixtures.buildUser(99L)));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(messageService.getMessage("dayoff.approval.denied"))
                .thenReturn("Not authorized");

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);

        assertThrows(AccessDeniedException.class,
                () -> approvalService.approveDayOffRequest(req, regularUserPrincipal()));
        verify(dayOffRepository, never()).save(any());
        verify(sseService, never()).sendEvent(anyString(), anyString(), any());
        verify(emailService, never()).sendApprovalNotificationEmail(anyString(), anyString(), anyString(), anyString());
    }

    // -------------------------------------------------------------------------
    // Self-approval: even a MANAGER cannot approve their own day-off request
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_selfApproval_throwsAccessDeniedException() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        manager.setId(MANAGER_ID); // requester and decider must be the same identity
        DayOffEntity dayOff = Fixtures.buildDayOff(REQUEST_ID, manager);
        dayOff.setStatus(EDayOffStatus.PENDING);
        dayOff.setId(REQUEST_ID); // afterCommit payload uses Map.of, which rejects a null id

        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(messageService.getMessage("dayoff.approval.self.denied"))
                .thenReturn("Self-approval denied");

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);

        assertThrows(AccessDeniedException.class,
                () -> approvalService.approveDayOffRequest(req, managerPrincipal()));
        verify(dayOffRepository, never()).save(any());
        verify(sseService, never()).sendEvent(anyString(), anyString(), any());
        verify(emailService, never()).sendApprovalNotificationEmail(anyString(), anyString(), anyString(), anyString());
    }

    // -------------------------------------------------------------------------
    // Null requestedBy → BadRequestException("dayoff.requester.missing")
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_nullRequestedBy_throwsBadRequestException() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        DayOffEntity dayOff = Fixtures.buildDayOff(REQUEST_ID, Fixtures.buildUser(5L));
        dayOff.setStatus(EDayOffStatus.PENDING);
        dayOff.setId(REQUEST_ID); // afterCommit payload uses Map.of, which rejects a null id
        dayOff.setRequestedBy(null); // simulate missing requester

        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(messageService.getMessage("dayoff.requester.missing"))
                .thenReturn("Requester missing");

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);

        assertThrows(BadRequestException.class,
                () -> approvalService.approveDayOffRequest(req, managerPrincipal()));
        verify(dayOffRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // R10.5 / R10.6 — SSE + email notifications verified on approved path
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_pendingToApproved_pushesSseAndSendsEmail() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        UserEntity requester = Fixtures.buildUser(5L);
        requester.setId(REQUESTER_ID);
        DayOffEntity dayOff = Fixtures.buildDayOff(REQUEST_ID, requester);
        dayOff.setStatus(EDayOffStatus.PENDING);
        dayOff.setId(REQUEST_ID); // afterCommit payload uses Map.of, which rejects a null id

        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(dayOffRepository.save(dayOff)).thenReturn(dayOff);

        DayOffDto expected = DayOffDto.builder()
                .requestId(REQUEST_ID)
                .status(EDayOffStatus.APPROVED)
                .build();
        when(dayOffMapping.toDto(dayOff)).thenReturn(expected);

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);
        approvalService.approveDayOffRequest(req, managerPrincipal());

        // The event is addressed to the REQUESTER's channel, not the request id.
        TransactionSyncTestSupport.triggerAfterCommit();
        verify(sseService).sendEvent(eq(String.valueOf(REQUESTER_ID)), eq("dayoff.approval"),
                argThat(data -> {
                    Map<?, ?> map = (Map<?, ?>) data;
                    return Long.valueOf(REQUEST_ID).equals(map.get("dayOffId"))
                            && "APPROVED".equals(map.get("status"));
                }));

        // Verify email notification payload
        verify(emailService).sendApprovalNotificationEmail(
                eq(requester.getEmail()),
                anyString(),
                eq("Day Off Request"),
                eq(EDayOffStatus.APPROVED.name()));
    }
}
