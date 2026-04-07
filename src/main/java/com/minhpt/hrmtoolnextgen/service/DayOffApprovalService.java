package com.minhpt.hrmtoolnextgen.service;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.ApprovalDayOffRequest;
import com.minhpt.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Log4j2
public class DayOffApprovalService {

    private final DayOffRepository dayOffRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    @Transactional
    public DayOffDto approveDayOffRequest(ApprovalDayOffRequest request, UserPrincipalDto userPrincipalDto) {
        Long decidedId = userPrincipalDto.getId();
        log.info("Processing day off approval for user: {} from {} to {} with status: {}",
                decidedId, request.getStartTime(), request.getEndTime(), request.getStatus());

        UserEntity userEntity = userRepository.findById(Objects.requireNonNull(decidedId))
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("user.not.found", decidedId)));

        DayOffEntity dayOffEntity = dayOffRepository
            .findByRequestedByIdAndStartTimeAndEndTimeAndDeleteFalse(
                userPrincipalDto.getId(),
                request.getStartTime(),
                request.getEndTime())
            .orElseThrow(() -> new BadRequestException(messageService.getMessage("dayoff.not.found")));

        if (dayOffEntity.getStatus() != EDayOffStatus.PENDING) {
            throw new BadRequestException(messageService.getMessage("dayoff.already.processed"));
        }

        dayOffEntity.setStatus(request.getStatus());
        dayOffEntity.setDecidedAt(LocalDateTime.now());
        dayOffEntity.setDecidedBy(userEntity);
        DayOffEntity savedEntity = dayOffRepository.save(dayOffEntity);

        log.info("Updated day off request status to {} for user: {} from {} to {}",
                request.getStatus(), decidedId, request.getStartTime(), request.getEndTime());
        return toDto(savedEntity);
    }

    private DayOffDto toDto(DayOffEntity entity) {
        return DayOffDto.builder()
                .requestId(entity.getId())
                .requesterName(entity.getRequestedBy().getUserInfo() != null
                        ? entity.getRequestedBy().getUserInfo().getFirstName() + " " + entity.getRequestedBy().getUserInfo().getLastName()
                        : null)
                .requesterEmail(entity.getRequestedBy().getEmail())
                .requestTitle(entity.getTitle())
                .requestReason(entity.getReason())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .build();
    }
}
