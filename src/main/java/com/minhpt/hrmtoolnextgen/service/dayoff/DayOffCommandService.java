package com.minhpt.hrmtoolnextgen.service.dayoff;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateDayOffRequest;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.minhpt.hrmtoolnextgen.service.dayoff.DayOffApprovalService.getDayOffDto;

@Service
@RequiredArgsConstructor
@Log4j2
public class DayOffCommandService {

    private final DayOffRepository dayOffRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    @Transactional
    public DayOffDto createDayOffRequest(UserPrincipalDto userPrincipalDto, CreateDayOffRequest request) {
        Long requestId = userPrincipalDto.getId();
        log.info("Creating day off request for user: {} from {} to {}", requestId, request.getStartTime(), request.getEndTime());

        UserEntity userEntity = userRepository.findById(Objects.requireNonNull(requestId))
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("user.not.found", requestId)));

        LocalDateTime startDateTime = request.getStartTime();
        LocalDateTime endDateTime = request.getEndTime();
        validateTimeRange(startDateTime, endDateTime);
        ensureNoOverlap(requestId, startDateTime, endDateTime);

        DayOffEntity dayOffEntity = new DayOffEntity();
        dayOffEntity.setRequestedBy(userEntity);
        dayOffEntity.setRequestedAt(LocalDateTime.now());
        dayOffEntity.setTitle(request.getRequestTitle());
        dayOffEntity.setReason(request.getRequestReason());
        dayOffEntity.setStartTime(startDateTime);
        dayOffEntity.setEndTime(endDateTime);
        dayOffEntity.setStatus(EDayOffStatus.PENDING);

        DayOffEntity savedEntity = dayOffRepository.save(dayOffEntity);
        log.info("Created day off request for user: {} from {} to {}", requestId, startDateTime, endDateTime);
        return toDto(savedEntity);
    }

    private void validateTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (!endDateTime.isAfter(startDateTime)) {
            throw new BadRequestException(messageService.getMessage("dayoff.end.after.start"));
        }

        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            switch (date.getDayOfWeek()) {
                case SATURDAY, SUNDAY ->
                        throw new BadRequestException(messageService.getMessage("dayoff.overlap.weekend"));
                default -> {}
            }
        }
    }

    private void ensureNoOverlap(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        boolean overlaps = dayOffRepository.existsByRequestedByIdAndDeleteFalseAndStartTimeLessThanAndEndTimeGreaterThan(
                userId,
                endDateTime,
                startDateTime
        );

        if (overlaps) {
            throw new BadRequestException(messageService.getMessage("dayoff.overlap.existing"));
        }
    }

    private DayOffDto toDto(DayOffEntity entity) {
        return getDayOffDto(entity);
    }
}
