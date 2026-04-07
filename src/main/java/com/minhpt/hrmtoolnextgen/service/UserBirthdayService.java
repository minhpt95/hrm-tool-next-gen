package com.minhpt.hrmtoolnextgen.service;

import com.minhpt.hrmtoolnextgen.constant.CommonConstant;
import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.dto.user.UserDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.mapping.UserMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.util.CommonUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service for birthday-related user queries, extracted from UserService for better cohesion.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class UserBirthdayService {

    private final UserRepository userRepository;
    private final UserMapping userMapping;

    @Transactional(readOnly = true)
    public PaginationResponse<UserDto> getUsersWithBirthday(PaginationRequest paginationRequest, LocalDate date) {
        log.debug("Getting users with birthday on date: {}", date);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String targetDateStr = formatMonthDay(targetDate);

        Specification<UserEntity> spec = buildBirthdaySpec(List.of(targetDateStr));
        return executeUserPageQuery(spec, paginationRequest, "id", "ASC");
    }

    @Transactional(readOnly = true)
    public PaginationResponse<UserDto> getUsersWithBirthdayToday(PaginationRequest paginationRequest) {
        return getUsersWithBirthday(paginationRequest, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public PaginationResponse<UserDto> getUsersWithUpcomingBirthdays(PaginationRequest paginationRequest) {
        log.debug("Getting users with upcoming birthdays (next {} days)", CommonConstant.UPCOMING_BIRTHDAY_DAYS);
        LocalDate today = LocalDate.now();
        List<String> upcomingDates = new ArrayList<>();
        for (int i = 1; i <= CommonConstant.UPCOMING_BIRTHDAY_DAYS; i++) {
            upcomingDates.add(formatMonthDay(today.plusDays(i)));
        }

        Specification<UserEntity> spec = buildBirthdaySpec(upcomingDates);
        return executeUserPageQuery(spec, paginationRequest, "id", "ASC");
    }

    private Specification<UserEntity> buildBirthdaySpec(List<String> monthDayValues) {
        return (root, query, cb) -> {
            var userInfoPath = root.get("userInfo");
            Predicate birthDateNotNull = cb.isNotNull(userInfoPath.get("birthDate"));

            List<Predicate> datePredicates = new ArrayList<>();
            for (String dateStr : monthDayValues) {
                datePredicates.add(cb.equal(
                        cb.function("TO_CHAR", String.class, userInfoPath.get("birthDate"), cb.literal("MM-DD")),
                        dateStr
                ));
            }

            Predicate anyDate = datePredicates.size() == 1
                    ? datePredicates.get(0)
                    : cb.or(datePredicates.toArray(Predicate[]::new));

            return cb.and(birthDateNotNull, anyDate);
        };
    }

    private PaginationResponse<UserDto> executeUserPageQuery(
            Specification<UserEntity> spec,
            PaginationRequest paginationRequest,
            String defaultSort,
            String defaultDirection) {
        Pageable pageable = CommonUtils.buildPageableWithDefaultSort(paginationRequest, defaultSort, defaultDirection);
        Page<UserEntity> entityPage = userRepository.findAll(spec, Objects.requireNonNull(pageable));
        Page<UserDto> dtoPage = userMapping.toDtoPageable(entityPage);
        return CommonUtils.buildPaginationResponse(dtoPage,
                CommonUtils.buildPaginationRequestForResponse(
                        paginationRequest,
                        paginationRequest.getSortBy() != null && !paginationRequest.getSortBy().isBlank()
                                ? paginationRequest.getSortBy() : defaultSort,
                        paginationRequest.getDirection() != null && !paginationRequest.getDirection().isBlank()
                                ? paginationRequest.getDirection() : defaultDirection
                ));
    }

    private static String formatMonthDay(LocalDate date) {
        return String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth());
    }
}
