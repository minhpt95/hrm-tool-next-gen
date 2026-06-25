package com.minhpt.hrmtoolnextgen.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.minhpt.hrmtoolnextgen.constant.CommonConstant;
import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.dto.user.UserDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.mapping.UserMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Unit tests for UserBirthdayService — plain Mockito, no Spring context.
 *
 * R22.4 getUsersWithBirthdayToday   — delegates to getUsersWithBirthday(req, today),
 *                                     calls userRepository.findAll(spec, pageable),
 *                                     maps via userMapping, and returns a PaginationResponse.
 * R22.4 getUsersWithUpcomingBirthdays — builds a list of the next
 *                                       CommonConstant.UPCOMING_BIRTHDAY_DAYS month-day strings
 *                                       and calls the same repo/mapping pipeline.
 *
 * Spec internals (TO_CHAR month-day predicates) are opaque to the unit test; we verify
 * the repo and mapping are invoked and that the returned envelope carries the correct
 * element counts and page metadata.
 */
@ExtendWith(MockitoExtension.class)
class UserBirthdayServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapping userMapping;

    @InjectMocks
    private UserBirthdayService userBirthdayService;

    // -------------------------------------------------------------------------
    // R22.4 getUsersWithBirthdayToday — happy path: 2 users returned
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getUsersWithBirthdayToday_twoMatches_returnsCorrectEnvelope() {
        UserEntity e1 = Fixtures.buildUser(1L);
        UserEntity e2 = Fixtures.buildUser(2L);
        Page<UserEntity> entityPage = new PageImpl<>(List.of(e1, e2));

        UserDto dto1 = new UserDto(); dto1.setId(1L); dto1.setEmail("user-1@example.com");
        UserDto dto2 = new UserDto(); dto2.setId(2L); dto2.setEmail("user-2@example.com");
        Page<UserDto> dtoPage = new PageImpl<>(List.of(dto1, dto2));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(entityPage);
        when(userMapping.toDtoPageable(entityPage)).thenReturn(dtoPage);

        PaginationRequest req = PaginationRequest.builder().page(0).size(10).build();
        PaginationResponse<UserDto> result = userBirthdayService.getUsersWithBirthdayToday(req);

        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        assertEquals(2L, result.getTotalElements());

        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(userMapping).toDtoPageable(entityPage);
    }

    // -------------------------------------------------------------------------
    // R22.4 getUsersWithBirthdayToday — no matches returns empty envelope
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getUsersWithBirthdayToday_noMatches_returnsEmptyItems() {
        Page<UserEntity> emptyEntityPage = new PageImpl<>(List.of());
        Page<UserDto>    emptyDtoPage    = new PageImpl<>(List.of());

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyEntityPage);
        when(userMapping.toDtoPageable(emptyEntityPage)).thenReturn(emptyDtoPage);

        PaginationRequest req = PaginationRequest.builder().page(0).size(10).build();
        PaginationResponse<UserDto> result = userBirthdayService.getUsersWithBirthdayToday(req);

        assertNotNull(result);
        assertEquals(0, result.getItems().size());
        assertEquals(0L, result.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // R22.4 getUsersWithUpcomingBirthdays — repo is called once with a spec built
    // from UPCOMING_BIRTHDAY_DAYS distinct future dates; returns the envelope.
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getUsersWithUpcomingBirthdays_returnsEnvelopeAndCallsRepo() {
        UserEntity e1 = Fixtures.buildUser(3L);
        Page<UserEntity> entityPage = new PageImpl<>(List.of(e1));

        UserDto dto1 = new UserDto(); dto1.setId(3L); dto1.setEmail("user-3@example.com");
        Page<UserDto> dtoPage = new PageImpl<>(List.of(dto1));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(entityPage);
        when(userMapping.toDtoPageable(entityPage)).thenReturn(dtoPage);

        PaginationRequest req = PaginationRequest.builder().page(0).size(10).build();
        PaginationResponse<UserDto> result = userBirthdayService.getUsersWithUpcomingBirthdays(req);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(userMapping).toDtoPageable(entityPage);
    }

    // -------------------------------------------------------------------------
    // R22.4 getUsersWithBirthday(req, date) — explicit date is used (not today)
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getUsersWithBirthday_explicitDate_callsRepoAndReturnsEnvelope() {
        Page<UserEntity> entityPage = new PageImpl<>(List.of(Fixtures.buildUser(4L)));
        UserDto dto4 = new UserDto(); dto4.setId(4L); dto4.setEmail("user-4@example.com");
        Page<UserDto>    dtoPage    = new PageImpl<>(List.of(dto4));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(entityPage);
        when(userMapping.toDtoPageable(entityPage)).thenReturn(dtoPage);

        PaginationRequest req = PaginationRequest.builder().page(0).size(5).build();
        LocalDate target = LocalDate.of(2026, 7, 4);
        PaginationResponse<UserDto> result = userBirthdayService.getUsersWithBirthday(req, target);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(1L, result.getTotalElements());
        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // -------------------------------------------------------------------------
    // R22.4 getUsersWithBirthday — null date falls back to today without NPE
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getUsersWithBirthday_nullDate_usesTodayWithoutException() {
        Page<UserEntity> entityPage = new PageImpl<>(List.of());
        Page<UserDto>    dtoPage    = new PageImpl<>(List.of());

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(entityPage);
        when(userMapping.toDtoPageable(entityPage)).thenReturn(dtoPage);

        PaginationRequest req = PaginationRequest.builder().page(0).size(10).build();
        PaginationResponse<UserDto> result = userBirthdayService.getUsersWithBirthday(req, null);

        assertNotNull(result);
        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // -------------------------------------------------------------------------
    // R22.4 pagination metadata — sortBy defaults to "id", direction to "ASC"
    // when request carries no sort fields
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getUsersWithBirthdayToday_defaultSort_responseCarriesIdAsc() {
        Page<UserEntity> entityPage = new PageImpl<>(List.of());
        Page<UserDto>    dtoPage    = new PageImpl<>(List.of());

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(entityPage);
        when(userMapping.toDtoPageable(entityPage)).thenReturn(dtoPage);

        PaginationRequest req = PaginationRequest.builder().page(0).size(10).build();
        PaginationResponse<UserDto> result = userBirthdayService.getUsersWithBirthdayToday(req);

        assertEquals("id",  result.getSortBy());
        assertEquals("ASC", result.getDirection());
    }

    // -------------------------------------------------------------------------
    // R22.4 getUsersWithUpcomingBirthdays — UPCOMING_BIRTHDAY_DAYS = 4:
    // repo receives exactly one spec call covering the next 4 days (verified by
    // ensuring the Pageable is built and the single-call contract holds)
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getUsersWithUpcomingBirthdays_invokedOnce_withUpcomingDayCount() {
        Page<UserEntity> entityPage = new PageImpl<>(List.of());
        Page<UserDto>    dtoPage    = new PageImpl<>(List.of());

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(entityPage);
        when(userMapping.toDtoPageable(entityPage)).thenReturn(dtoPage);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        PaginationRequest req = PaginationRequest.builder().page(0).size(10).build();
        userBirthdayService.getUsersWithUpcomingBirthdays(req);

        // Single repo call with a pageable whose page size matches the request
        verify(userRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals(10, pageableCaptor.getValue().getPageSize());

        // UPCOMING_BIRTHDAY_DAYS is 4 — sanity-check the constant hasn't drifted
        assertEquals(4, CommonConstant.UPCOMING_BIRTHDAY_DAYS);
    }
}
