package com.minhpt.hrmtoolnextgen.service;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.minhpt.hrmtoolnextgen.dto.request.ApprovalDayOffRequest;
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

@Service
@RequiredArgsConstructor
@Log4j2
public class DayOffService {

    private final DayOffRepository dayOffRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    @Transactional
    public DayOffDto createDayOffRequest(Long userId, CreateDayOffRequest request) {
        log.info("Creating day off request for user: {} from {} to {}", userId, request.getStartTime(), request.getEndTime());
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("user.not.found", userId)));

        LocalDateTime startDateTime = request.getStartTime();
        LocalDateTime endDateTime = request.getEndTime();

        validateTimeRange(startDateTime, endDateTime);
        ensureNoOverlap(userId, startDateTime, endDateTime);

        // Create a new day off entity
        DayOffEntity dayOffEntity = new DayOffEntity();
        dayOffEntity.setUser(userEntity);
        dayOffEntity.setRequestTitle(request.getRequestTitle());
        dayOffEntity.setRequestReason(request.getRequestReason());
        dayOffEntity.setStartTime(startDateTime);
        dayOffEntity.setEndTime(endDateTime);
        dayOffEntity.setStatus(EDayOffStatus.PENDING);

        DayOffEntity savedEntity = dayOffRepository.save(dayOffEntity);
        log.info("Created day off request for user: {} from {} to {}",
                userId, startDateTime, endDateTime);

        return toDto(savedEntity);
    }

    private void validateTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (!endDateTime.isAfter(startDateTime)) {
            throw new BadRequestException(messageService.getMessage("dayoff.end.after.start"));
        }

        // Disallow day off ranges that cover weekends
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            switch (date.getDayOfWeek()) {
                case SATURDAY, SUNDAY ->
                        throw new BadRequestException(messageService.getMessage("dayoff.overlap.weekend"));
                default -> {
                }
            }
        }
    }

    private void ensureNoOverlap(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        var existingDayOffs = dayOffRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            predicates.add(cb.equal(root.get("delete"), false));
            // Overlap when existing.start < newEnd && existing.end > newStart
            predicates.add(cb.lessThan(root.get("startTime"), endDateTime));
            predicates.add(cb.greaterThan(root.get("endTime"), startDateTime));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });

        for (DayOffEntity existing : existingDayOffs) {
            // Extra guard if repository adds additional records beyond criteria
            boolean overlaps = existing.getStartTime().isBefore(endDateTime)
                    && existing.getEndTime().isAfter(startDateTime);
            if (overlaps) throw new BadRequestException(messageService.getMessage("dayoff.overlap.existing"));
        }
    }

    @Transactional
    public DayOffDto approveDayOffRequest(ApprovalDayOffRequest request) {
        log.info("Processing day off approval for user: {} from {} to {} with status: {}", request.getUserId(), request.getStartTime(), request.getEndTime(), request.getStatus());
        // Find day off by user, startTime, and endTime
        var dayOffs = dayOffRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("user").get("id"), request.getUserId()));
            predicates.add(cb.equal(root.get("startTime"), request.getStartTime()));
            predicates.add(cb.equal(root.get("endTime"), request.getEndTime()));
            predicates.add(cb.equal(root.get("delete"), false));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });

        if (dayOffs.isEmpty()) {
            throw new BadRequestException(messageService.getMessage("dayoff.not.found"));
        }

        DayOffEntity dayOffEntity = dayOffs.getFirst();

        // Check if already processed
        if (dayOffEntity.getStatus() != EDayOffStatus.PENDING) {
            throw new BadRequestException(messageService.getMessage("dayoff.already.processed"));
        }

        // Update status
        dayOffEntity.setStatus(request.getStatus());
        DayOffEntity savedEntity = dayOffRepository.save(dayOffEntity);

        log.info("Updated day off request status to {} for user: {} from {} to {}",
                request.getStatus(), request.getUserId(), request.getStartTime(), request.getEndTime());

        return toDto(savedEntity);
    }

    private DayOffDto toDto(DayOffEntity entity) {
        return DayOffDto.builder()
                .userId(entity.getUser().getId())
                .userName(entity.getUser().getUserInfo() != null
                        ? entity.getUser().getUserInfo().getFirstName() + " " +
                        entity.getUser().getUserInfo().getLastName()
                        : null)
                .userEmail(entity.getUser().getEmail())
                .requestTitle(entity.getRequestTitle())
                .requestReason(entity.getRequestReason())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .build();
    }
}

