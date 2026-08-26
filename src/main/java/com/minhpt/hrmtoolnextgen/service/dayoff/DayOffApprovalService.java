package com.minhpt.hrmtoolnextgen.service.dayoff;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.constant.RoleConstant;
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
import com.minhpt.hrmtoolnextgen.service.EmailService;
import com.minhpt.hrmtoolnextgen.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Log4j2
public class DayOffApprovalService {

    private final DayOffRepository dayOffRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;
    private final DayOffMapping dayOffMapping;
    private final SseService sseService;
    private final EmailService emailService;

    @Transactional
    public DayOffDto approveDayOffRequest(ApprovalDayOffRequest request, UserPrincipalDto userPrincipalDto) {
        // Reject unauthenticated / unrecognised principals before doing any lookup.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserPrincipalDto currentUser)) {
            throw new AccessDeniedException(messageService.getMessage("error.access.denied"));
        }

        Long decidedId = userPrincipalDto.getId();
        log.info("Processing day off approval for user: {} with status: {}", decidedId, request.getStatus());

        UserEntity decidedUserEntity = userRepository
                .findById(Objects.requireNonNull(decidedId))
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("user.not.found", decidedId)));

        DayOffEntity dayOffEntity = dayOffRepository
                .findById(request.getId())
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("dayoff.not.found", request.getId())));

        if (dayOffEntity.getStatus() != EDayOffStatus.PENDING) {
            throw new BadRequestException(messageService.getMessage("dayoff.already.processed"));
        }

        // Authorization check: only ADMIN, HR, PROJECT_MANAGER, IT_ADMIN can approve/reject
        boolean isAuthorized = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> RoleConstant.ADMIN.equals(auth)
                        || RoleConstant.HR.equals(auth)
                        || RoleConstant.PROJECT_MANAGER.equals(auth)
                        || RoleConstant.IT_ADMIN.equals(auth));
        if (!isAuthorized) {
            throw new AccessDeniedException(messageService.getMessage("dayoff.approval.denied"));
        }

        // Null guard: ensure requester exists before accessing
        if (dayOffEntity.getRequestedBy() == null) {
            throw new BadRequestException(messageService.getMessage("dayoff.requester.missing"));
        }

        // Separation of duties: nobody may decide their own request, whatever their role.
        if (Objects.equals(dayOffEntity.getRequestedBy().getId(), decidedId)) {
            throw new AccessDeniedException(messageService.getMessage("dayoff.approval.self.denied"));
        }

        dayOffEntity.setStatus(request.getStatus());
        dayOffEntity.setDecidedAt(LocalDateTime.now());
        dayOffEntity.setDecidedBy(decidedUserEntity);
        DayOffEntity savedEntity = dayOffRepository.save(dayOffEntity);

        log.info("Updated day off request status to {} for user: {}", request.getStatus(), decidedId);

        // Schedule SSE and email notifications after transaction commit
        Long requesterId = dayOffEntity.getRequestedBy().getId();
        String requesterIdStr = String.valueOf(requesterId);
        String requesterEmail = dayOffEntity.getRequestedBy().getEmail();
        String requesterName = dayOffEntity.getRequestedBy().getUserInfo() != null
                ? dayOffEntity.getRequestedBy().getUserInfo().getFirstName() : "User";
        String status = savedEntity.getStatus().name();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                sseService.sendEvent(requesterIdStr, "dayoff.approval",
                        Map.of("dayOffId", savedEntity.getId(), "status", status));
                emailService.sendApprovalNotificationEmail(requesterEmail, requesterName, "Day Off Request", status);
            }
        });

        return dayOffMapping.toDto(savedEntity);
    }
}
