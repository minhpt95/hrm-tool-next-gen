package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.vatek.hrmtoolnextgen.dto.request.ApprovalDayOffRequest;
import com.vatek.hrmtoolnextgen.dto.request.CreateDayOffRequest;
import com.vatek.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.vatek.hrmtoolnextgen.exception.BadRequestException;
import com.vatek.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.UserRepository;
import com.vatek.hrmtoolnextgen.util.DateUtils;
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

    @Transactional
    public DayOffDto createDayOffRequest(Long userId, CreateDayOffRequest request) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found with id: " + userId));

        // Validate that endTime is after startTime
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().equals(request.getStartTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }

        // Validate that startTime and endTime are on the same day
        LocalDate startDate = request.getStartTime().toLocalDate();
        LocalDate endDate = request.getEndTime().toLocalDate();
        if (!startDate.equals(endDate)) {
            throw new BadRequestException("startTime and endTime must be on the same day");
        }

        // Check for overlapping day off requests on the same date
        var existingDayOffs = dayOffRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            predicates.add(cb.equal(root.get("delete"), false));
            // Check if the date matches (same day)
            predicates.add(cb.equal(
                    cb.function("DATE", LocalDate.class, root.get("startTime")),
                    startDate
            ));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });

        for (DayOffEntity existing : existingDayOffs) {
            // Check if time ranges overlap
            if (!(endDateTime.isBefore(existing.getStartTime()) || startDateTime.isAfter(existing.getEndTime()))) {
                throw new BadRequestException("Day off request overlaps with an existing request");
            }
        }

        // Create new day off entity
        DayOffEntity dayOffEntity = new DayOffEntity();
        dayOffEntity.setUser(userEntity);
        dayOffEntity.setRequestTitle(request.getRequestTitle());
        dayOffEntity.setRequestReason(request.getRequestReason());
        dayOffEntity.setStartTime(request.getStartTime());
        dayOffEntity.setEndTime(request.getEndTime());
        dayOffEntity.setStatus(EDayOffStatus.PENDING);

        DayOffEntity savedEntity = dayOffRepository.save(dayOffEntity);
        log.info("Created day off request for user: {} from {} to {}", 
                userId, startDateTime, endDateTime);

        return toDto(savedEntity);
    }

    @Transactional
    public DayOffDto approveDayOffRequest(ApprovalDayOffRequest request) {
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
            throw new BadRequestException("Day off request not found");
        }

        DayOffEntity dayOffEntity = dayOffs.get(0);

        // Check if already processed
        if (dayOffEntity.getStatus() != EDayOffStatus.PENDING) {
            throw new BadRequestException("Day off request has already been processed");
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
                .startTime(entity.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .endTime(entity.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .dateOff(entity.getStartTime().toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
                .status(entity.getStatus())
                .build();
    }
}

