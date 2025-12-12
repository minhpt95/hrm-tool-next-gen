package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.vatek.hrmtoolnextgen.dto.request.ApprovalDayOffRequest;
import com.vatek.hrmtoolnextgen.dto.request.CreateDayOffRequest;
import com.vatek.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffType;
import com.vatek.hrmtoolnextgen.exception.BadRequestException;
import com.vatek.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.UserRepository;
import com.vatek.hrmtoolnextgen.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

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

        // Validate numberOfHours based on type
        if (request.getType() == EDayOffType.PARTIAL) {
            if (request.getNumberOfHours() == null) {
                throw new BadRequestException("numberOfHours is required for PARTIAL day off type");
            }
            if (request.getNumberOfHours() <= 0) {
                throw new BadRequestException("numberOfHours must be greater than 0 for PARTIAL day off type");
            }
            if (request.getNumberOfHours() >= 8) {
                throw new BadRequestException("numberOfHours must be less than 8 for PARTIAL day off type");
            }
        }
        // For FULL type, numberOfHours is not required and can be null

        // Convert date string to Instant
        LocalDate localDate = DateUtils.convertStringDateToLocalDate(request.getDateOff());
        Instant dateOffInstant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Check if day off request already exists
        DayOffEntity.DayOffEntityId dayOffId = new DayOffEntity.DayOffEntityId();
        dayOffId.setDateOff(dateOffInstant);
        dayOffId.setType(request.getType());
        dayOffId.setUser(userEntity);

        if (dayOffRepository.findById(dayOffId).isPresent()) {
            throw new BadRequestException("Day off request already exists for this date and type");
        }

        // Create new day off entity
        DayOffEntity dayOffEntity = new DayOffEntity();
        dayOffEntity.setDayoffEntityId(dayOffId);
        dayOffEntity.setRequestTitle(request.getRequestTitle());
        dayOffEntity.setRequestReason(request.getRequestReason());
        dayOffEntity.setNumberOfHours(request.getNumberOfHours());
        dayOffEntity.setStatus(EDayOffStatus.PENDING);

        DayOffEntity savedEntity = dayOffRepository.save(dayOffEntity);
        log.info("Created day off request for user: {} on date: {} with type: {} and hours: {}", 
                userId, dateOffInstant, request.getType(), request.getNumberOfHours());

        return toDto(savedEntity);
    }

    @Transactional
    public DayOffDto approveDayOffRequest(ApprovalDayOffRequest request) {
        // Build the composite key
        DayOffEntity.DayOffEntityId dayOffId = new DayOffEntity.DayOffEntityId();
        dayOffId.setDateOff(request.getDateOff());
        dayOffId.setType(request.getType());
        
        UserEntity userEntity = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found with id: " + request.getUserId()));
        dayOffId.setUser(userEntity);

        DayOffEntity dayOffEntity = dayOffRepository.findById(dayOffId)
                .orElseThrow(() -> new BadRequestException("Day off request not found"));

        // Check if already processed
        if (dayOffEntity.getStatus() != EDayOffStatus.PENDING) {
            throw new BadRequestException("Day off request has already been processed");
        }

        // Update status
        dayOffEntity.setStatus(request.getStatus());
        DayOffEntity savedEntity = dayOffRepository.save(dayOffEntity);
        
        log.info("Updated day off request status to {} for user: {} on date: {}", 
                request.getStatus(), request.getUserId(), request.getDateOff());

        return toDto(savedEntity);
    }

    private DayOffDto toDto(DayOffEntity entity) {
        return DayOffDto.builder()
                .userId(entity.getDayoffEntityId().getUser().getId())
                .userName(entity.getDayoffEntityId().getUser().getUserInfo() != null 
                        ? entity.getDayoffEntityId().getUser().getUserInfo().getFirstName() + " " + 
                          entity.getDayoffEntityId().getUser().getUserInfo().getLastName()
                        : null)
                .userEmail(entity.getDayoffEntityId().getUser().getEmail())
                .requestTitle(entity.getRequestTitle())
                .requestReason(entity.getRequestReason())
                .dateOff(entity.getDayoffEntityId().getDateOff())
                .type(entity.getDayoffEntityId().getType())
                .numberOfHours(entity.getNumberOfHours())
                .status(entity.getStatus())
                .build();
    }
}

