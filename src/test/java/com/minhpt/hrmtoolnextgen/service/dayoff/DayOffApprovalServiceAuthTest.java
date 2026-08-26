package com.minhpt.hrmtoolnextgen.service.dayoff;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import com.minhpt.hrmtoolnextgen.support.TransactionSyncTestSupport;
import org.junit.jupiter.api.AfterEach;

/**
 * Role-based authorization tests for DayOffApprovalService.
 *
 * Verifies that each role (ADMIN, HR, PROJECT_MANAGER, IT_ADMIN, USER) is correctly
 * allowed or denied when approving/rejecting day-off requests.
 */
@ExtendWith(MockitoExtension.class)
class DayOffApprovalServiceAuthTest {

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

    private UserPrincipalDto principalFor(EUserRole... roles) {
        return UserPrincipalDto.internalBuilder()
                .id(MANAGER_ID)
                .email("manager@example.com")
                .authorities(List.of(roles))
                .build();
    }

    private void mockSecurityContext(UserPrincipalDto principal) {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
    }

    @BeforeEach
    void setUp() {
        // Services defer SSE/email to afterCommit; open a synchronization scope so
        // registerSynchronization() succeeds outside a real transaction.
        TransactionSyncTestSupport.begin();
    }

    @AfterEach
    void tearDown() {
        TransactionSyncTestSupport.end();
        SecurityContextHolder.clearContext();
    }

    private DayOffEntity pendingDayOff(long requestId, UserEntity requester) {
        DayOffEntity dayOff = Fixtures.buildDayOff(requestId, requester);
        dayOff.setStatus(EDayOffStatus.PENDING);
        // afterCommit payload uses Map.of, which rejects a null id
        dayOff.setId(requestId);
        return dayOff;
    }

    // -------------------------------------------------------------------------
    // ADMIN role — allowed
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_adminRole_allowed() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        UserEntity requester = Fixtures.buildUser(5L);
        DayOffEntity dayOff = pendingDayOff(REQUEST_ID, requester);

        mockSecurityContext(principalFor(EUserRole.ADMIN));
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(dayOffRepository.save(dayOff)).thenReturn(dayOff);
        when(dayOffMapping.toDto(dayOff)).thenReturn(DayOffDto.builder().requestId(REQUEST_ID).status(EDayOffStatus.APPROVED).build());

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);
        assertDoesNotThrow(() -> approvalService.approveDayOffRequest(req, principalFor(EUserRole.ADMIN)));
        verify(dayOffRepository).save(any());
    }

    // -------------------------------------------------------------------------
    // HR role — allowed
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_hrRole_allowed() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        UserEntity requester = Fixtures.buildUser(5L);
        DayOffEntity dayOff = pendingDayOff(REQUEST_ID, requester);

        mockSecurityContext(principalFor(EUserRole.HR));
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(dayOffRepository.save(dayOff)).thenReturn(dayOff);
        when(dayOffMapping.toDto(dayOff)).thenReturn(DayOffDto.builder().requestId(REQUEST_ID).status(EDayOffStatus.APPROVED).build());

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);
        assertDoesNotThrow(() -> approvalService.approveDayOffRequest(req, principalFor(EUserRole.HR)));
        verify(dayOffRepository).save(any());
    }

    // -------------------------------------------------------------------------
    // PROJECT_MANAGER role — allowed (approving another user's request)
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_projectManagerRole_allowed() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        UserEntity requester = Fixtures.buildUser(5L);
        DayOffEntity dayOff = pendingDayOff(REQUEST_ID, requester);

        mockSecurityContext(principalFor(EUserRole.PROJECT_MANAGER));
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(dayOffRepository.save(dayOff)).thenReturn(dayOff);
        when(dayOffMapping.toDto(dayOff)).thenReturn(DayOffDto.builder().requestId(REQUEST_ID).status(EDayOffStatus.APPROVED).build());

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);
        assertDoesNotThrow(() -> approvalService.approveDayOffRequest(req, principalFor(EUserRole.PROJECT_MANAGER)));
        verify(dayOffRepository).save(any());
    }

    // -------------------------------------------------------------------------
    // IT_ADMIN role — allowed
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_itAdminRole_allowed() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        UserEntity requester = Fixtures.buildUser(5L);
        DayOffEntity dayOff = pendingDayOff(REQUEST_ID, requester);

        mockSecurityContext(principalFor(EUserRole.IT_ADMIN));
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(dayOffRepository.save(dayOff)).thenReturn(dayOff);
        when(dayOffMapping.toDto(dayOff)).thenReturn(DayOffDto.builder().requestId(REQUEST_ID).status(EDayOffStatus.APPROVED).build());

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);
        assertDoesNotThrow(() -> approvalService.approveDayOffRequest(req, principalFor(EUserRole.IT_ADMIN)));
        verify(dayOffRepository).save(any());
    }

    // -------------------------------------------------------------------------
    // USER role — denied
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_userRole_throwsAccessDenied() {
        DayOffEntity dayOff = pendingDayOff(REQUEST_ID, Fixtures.buildUser(5L));

        mockSecurityContext(principalFor(EUserRole.USER));
        // The decider is resolved before the authorization gate, so it must resolve.
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(Fixtures.buildUser(MANAGER_ID)));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(messageService.getMessage("dayoff.approval.denied")).thenReturn("Denied");

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);
        assertThrows(AccessDeniedException.class,
                () -> approvalService.approveDayOffRequest(req, principalFor(EUserRole.USER)));
        verify(dayOffRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Unauthenticated (null principal) — denied
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_unauthenticated_throwsAccessDenied() {
        // The principal guard runs before any repository work, so nothing else is stubbed.
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);
        assertThrows(AccessDeniedException.class,
                () -> approvalService.approveDayOffRequest(req, principalFor(EUserRole.ADMIN)));
        verify(dayOffRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Self-approval: PROJECT_MANAGER approving own request — denied
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_projectManagerSelfApproval_throwsAccessDenied() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        manager.setId(MANAGER_ID); // requester and decider must be the same identity
        DayOffEntity dayOff = pendingDayOff(REQUEST_ID, manager);

        mockSecurityContext(principalFor(EUserRole.PROJECT_MANAGER));
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(messageService.getMessage("dayoff.approval.self.denied")).thenReturn("Self-approval denied");

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);
        assertThrows(AccessDeniedException.class,
                () -> approvalService.approveDayOffRequest(req, principalFor(EUserRole.PROJECT_MANAGER)));
        verify(dayOffRepository, never()).save(any());
        verify(sseService, never()).sendEvent(anyString(), anyString(), any());
        verify(emailService, never()).sendApprovalNotificationEmail(anyString(), anyString(), anyString(), anyString());
    }

    // -------------------------------------------------------------------------
    // Multiple roles: USER + PROJECT_MANAGER — allowed (PM grants access)
    // -------------------------------------------------------------------------

    @Test
    void approveDayOffRequest_userPlusProjectManagerRole_allowed() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        UserEntity requester = Fixtures.buildUser(5L);
        DayOffEntity dayOff = pendingDayOff(REQUEST_ID, requester);

        mockSecurityContext(principalFor(EUserRole.USER, EUserRole.PROJECT_MANAGER));
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(dayOffRepository.findById(REQUEST_ID)).thenReturn(Optional.of(dayOff));
        when(dayOffRepository.save(dayOff)).thenReturn(dayOff);
        when(dayOffMapping.toDto(dayOff)).thenReturn(DayOffDto.builder().requestId(REQUEST_ID).status(EDayOffStatus.APPROVED).build());

        ApprovalDayOffRequest req = new ApprovalDayOffRequest(REQUEST_ID, EDayOffStatus.APPROVED);
        assertDoesNotThrow(() -> approvalService.approveDayOffRequest(req, principalFor(EUserRole.USER, EUserRole.PROJECT_MANAGER)));
        verify(dayOffRepository).save(any());
    }
}
