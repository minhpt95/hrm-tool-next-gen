package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.constant.DateConstant;
import com.vatek.hrmtoolnextgen.constant.ErrorConstant;
import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.dto.request.ApprovalTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.request.CreateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.vatek.hrmtoolnextgen.entity.common.IdentityEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffType;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetType;
import com.vatek.hrmtoolnextgen.exception.BadRequestException;
import com.vatek.hrmtoolnextgen.exception.CommonException;
import com.vatek.hrmtoolnextgen.mapping.TimesheetMapping;
import com.vatek.hrmtoolnextgen.projection.TimesheetWorkingHourProjection;
import com.vatek.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.TimesheetRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.UserRepository;
import com.vatek.hrmtoolnextgen.util.DateUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;

@Service
@AllArgsConstructor
@Log4j2
public class TimesheetService {
    private TimesheetRepository timesheetRepository;
    private UserRepository userRepository;
    private ProjectRepository projectRepository;
    private DayOffRepository dayOffRepository;
    private TimesheetMapping timesheetMapping;

    @Transactional
    public TimesheetDto createTimesheet(CreateTimesheetRequest form) {
        var currentUser = (UserPrincipalDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        UserEntity userEntity = userRepository.findById(currentUser.getId()).orElse(null);

        ProjectEntity projectEntity = projectRepository.findById(form.getProjectId()).orElse(null);

        if (projectEntity == null) {
            throw new CommonException(
                    String.format(ErrorConstant.Message.NOT_FOUND, "Project id : " + form.getProjectId()),
                    HttpStatus.BAD_REQUEST
            );
        }

        Collection<Long> projectIds = userEntity.getWorkingProject().stream().map(IdentityEntity::getId).toList();

        if (!projectIds.contains(form.getProjectId())) {
            throw new BadRequestException(
                    "You are not in project"
            );
        }

        ZonedDateTime workingDayInstant = DateUtils.convertStringDateToZoneDateTime(form.getWorkingDay());

        var workingDayInstantDayOfWeek = workingDayInstant.getDayOfWeek();

        if (
                form.getTimesheetType() == ETimesheetType.NORMAL && (workingDayInstantDayOfWeek == DayOfWeek.SATURDAY || workingDayInstantDayOfWeek == DayOfWeek.SUNDAY)
        ) {
            throw new BadRequestException(
                    "Cannot log timesheet on weekend"
            );
        }

        Specification<DayOffEntity> dayOffEntitySpecification = (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(criteriaBuilder.equal(root.get("dayoffEntityId").get("dateOff"), workingDayInstant));
            predicates.add(criteriaBuilder.equal(root.get("dayoffEntityId").get("userId"), currentUser.getId()));
            Predicate[] p = new Predicate[predicates.size()];
            return criteriaBuilder.and(predicates.toArray(p));
        };

        var getRequestDayOff = dayOffRepository.findAll(dayOffEntitySpecification);

        if (form.getTimesheetType() == ETimesheetType.NORMAL) {

            var getTimesheetProjection = timesheetRepository
                    .findByUserEntityIdAndWorkingDayAndStatusNot(
                            currentUser.getId(),
                            workingDayInstant,
                            ETimesheetStatus.REJECTED
                    );

            var totalWorkingHours = getTimesheetProjection
                    .stream()
                    .mapToInt(TimesheetWorkingHourProjection::getWorkingHours)
                    .sum();

            switch (getRequestDayOff.size()) {
                case 2 -> throw new CommonException(
                        ErrorConstant.Message.CANNOT_LOG_ON_FULL_DAY_OFF,
                        HttpStatus.BAD_REQUEST
                );
                case 1 -> {
                    switch (getRequestDayOff.getFirst().getDayoffEntityId().getType()) {
                        case FULL -> throw new CommonException(
                                String.format(ErrorConstant.Message.CANNOT_LOG_TIMESHEET, 5),
                                HttpStatus.BAD_REQUEST
                        );
                        case EDayOffType.MORNING -> {
                            if (totalWorkingHours + form.getWorkingHours() > 5) {
                                throw new CommonException(
                                        String.format(ErrorConstant.Message.CANNOT_LOG_TIMESHEET, 5),
                                        HttpStatus.BAD_REQUEST
                                );
                            }
                        }
                        case EDayOffType.AFTERNOON -> {
                            if (totalWorkingHours + form.getWorkingHours() > 3) {
                                throw new CommonException(
                                        String.format(ErrorConstant.Message.CANNOT_LOG_TIMESHEET, 3),
                                        HttpStatus.BAD_REQUEST
                                );
                            }
                        }
                    }
                }
                case 0 -> {
                    if (totalWorkingHours + form.getWorkingHours() > 8) {
                        throw new CommonException(
                                String.format(ErrorConstant.Message.CANNOT_LOG_TIMESHEET, 8),
                                HttpStatus.BAD_REQUEST
                        );
                    }
                }
            }
        }


        TimesheetEntity timesheetEntity = new TimesheetEntity();
        timesheetEntity.setTitle(form.getTitle());
        timesheetEntity.setDescription(form.getDescription());
        timesheetEntity.setWorkingHours(form.getWorkingHours());
        timesheetEntity.setWorkingDay(workingDayInstant);
        timesheetEntity.setProjectEntity(projectEntity);
        timesheetEntity.setStatus(ETimesheetStatus.IN_PROGRESS);
        timesheetEntity.setUserEntity(userEntity);
        timesheetEntity.setType(form.getTimesheetType());
        timesheetEntity = timesheetRepository.save(timesheetEntity);

        return timesheetMapping.toDto(timesheetEntity);
    }

    public TimesheetDto updateTimesheet(UpdateTimesheetRequest form) {
        TimesheetEntity timesheetEntity = timesheetRepository.findById(form.getId()).orElseThrow(
                () -> new CommonException(
                        String.format(ErrorConstant.Message.NOT_FOUND, "Timesheet with id : " + form.getId()),
                        HttpStatus.BAD_REQUEST
                )
        );

        if (form.getTitle() != null) {
            timesheetEntity.setTitle(form.getTitle());
        }

        if (form.getDescription() != null) {
            timesheetEntity.setDescription(form.getDescription());
        }

        if (form.getTimesheetType() != null) {
            timesheetEntity.setType(form.getTimesheetType());
        }

        if (form.getProjectId() != null) {
            ProjectEntity projectEntity = projectRepository.findById(form.getProjectId()).orElseThrow(
                    () -> new CommonException(
                            String.format(ErrorConstant.Message.NOT_FOUND, "Project with id : " + form.getId()),
                            HttpStatus.BAD_REQUEST
                    )
            );
            timesheetEntity.setProjectEntity(projectEntity);
        }

        if (form.getWorkingHours() != null) {
            timesheetEntity.setWorkingHours(form.getWorkingHours());
        }

        if (form.getWorkingDay() != null)
            timesheetEntity.setWorkingDay(DateUtils.convertStringDateToZoneDateTime(form.getWorkingDay(), DateConstant.DD_MM_YYYY));

        timesheetEntity = timesheetRepository.save(timesheetEntity);

        return timesheetMapping.toDto(timesheetEntity);
    }

    public TimesheetDto decisionTimesheet(ApprovalTimesheetRequest form) {
        TimesheetEntity timesheetEntity = timesheetRepository.findById(form.getId()).orElseThrow(
                () -> new BadRequestException(
                        String.format(ErrorConstant.Message.NOT_FOUND, "Timesheet with id : " + form.getId())
                )
        );

        switch (timesheetEntity.getStatus()) {
            case ETimesheetStatus.IN_PROGRESS -> {
                if (form.getTimesheetStatus() == null) {
                    throw new BadRequestException(
                            String.format(ErrorConstant.Message.NOT_FOUND, "Status ")
                    );
                }
                timesheetEntity.setStatus(form.getTimesheetStatus());
            }
            case APPROVED -> timesheetEntity.setStatus(ETimesheetStatus.REJECTED);
            case REJECTED -> timesheetEntity.setStatus(ETimesheetStatus.APPROVED);
        }

        timesheetEntity = timesheetRepository.save(timesheetEntity);

        return timesheetMapping.toDto(timesheetEntity);
    }
}
