package com.minhpt.hrmtoolnextgen.service.dayoff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateDayOffRequest;
import com.minhpt.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.mapping.DayOffMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Unit tests for DayOffCommandService.createDayOffRequest.
 *
 * CREATION FLOW (verified from source):
 *   1. Resolve user via userRepository.findById → BadRequestException("user.not.found") if absent.
 *   2. validateTimeRange: end must be after start; no SATURDAY/SUNDAY in the date range.
 *   3. ensureNoOverlap: existsBy... query → BadRequestException("dayoff.overlap.existing") if true.
 *   4. Build DayOffEntity with status=PENDING, save, map to dto.
 *
 * KNOWN GAP R11.3 — EDayOffType classification:
 *   EDayOffType enum (FULL, PARTIAL) exists in the codebase but DayOffEntity has NO type field,
 *   and CreateDayOffRequest has NO type field. The classification requirement is unimplemented
 *   in the createDayOffRequest path. The @Disabled test at the bottom documents this gap.
 */
@ExtendWith(MockitoExtension.class)
class DayOffCommandServiceTest {

    @Mock private DayOffRepository dayOffRepository;
    @Mock private UserRepository   userRepository;
    @Mock private MessageService   messageService;
    @Mock private DayOffMapping    dayOffMapping;

    @InjectMocks
    private DayOffCommandService commandService;

    private static final long USER_ID = 1L;

    // Monday 2026-06-22 09:00 → Wednesday 2026-06-24 18:00 (pure weekdays, no weekend)
    private static final LocalDateTime WEEKDAY_START = LocalDateTime.of(2026, 6, 22, 9, 0);
    private static final LocalDateTime WEEKDAY_END   = LocalDateTime.of(2026, 6, 24, 18, 0);

    private UserPrincipalDto principal() {
        return UserPrincipalDto.internalBuilder()
                .id(USER_ID)
                .email("user@example.com")
                .build();
    }

    private CreateDayOffRequest request(LocalDateTime start, LocalDateTime end) {
        CreateDayOffRequest req = new CreateDayOffRequest();
        req.setStartTime(start);
        req.setEndTime(end);
        req.setRequestTitle("Annual Leave");
        req.setRequestReason("Family vacation");
        return req;
    }

    // -------------------------------------------------------------------------
    // R11.1 / R11.4 — happy path: weekday range, no overlap
    //   status=PENDING, requestedBy=resolved user, title/reason/start/end mapped;
    //   save invoked exactly once; dto returned.
    // -------------------------------------------------------------------------

    @Test
    void createDayOffRequest_weekdayRangeNoOverlap_savesEntityWithPendingStatus() {
        UserEntity user = Fixtures.buildUser(USER_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(dayOffRepository.existsByRequestedByIdAndDeleteFalseAndStartTimeLessThanAndEndTimeGreaterThan(
                USER_ID, WEEKDAY_END, WEEKDAY_START)).thenReturn(false);

        DayOffEntity savedEntity = Fixtures.buildDayOff(1L, user);
        savedEntity.setStatus(EDayOffStatus.PENDING);
        when(dayOffRepository.save(any(DayOffEntity.class))).thenReturn(savedEntity);

        DayOffDto expectedDto = DayOffDto.builder().requestId(1L).status(EDayOffStatus.PENDING).build();
        when(dayOffMapping.toDto(savedEntity)).thenReturn(expectedDto);

        DayOffDto result = commandService.createDayOffRequest(principal(), request(WEEKDAY_START, WEEKDAY_END));

        ArgumentCaptor<DayOffEntity> captor = ArgumentCaptor.forClass(DayOffEntity.class);
        verify(dayOffRepository).save(captor.capture());

        DayOffEntity captured = captor.getValue();
        assertEquals(EDayOffStatus.PENDING, captured.getStatus());
        assertEquals(user, captured.getRequestedBy());
        assertEquals("Annual Leave", captured.getTitle());
        assertEquals("Family vacation", captured.getReason());
        assertEquals(WEEKDAY_START, captured.getStartTime());
        assertEquals(WEEKDAY_END, captured.getEndTime());
        assertNotNull(captured.getRequestedAt());

        assertNotNull(result);
        assertEquals(EDayOffStatus.PENDING, result.getStatus());
    }

    // -------------------------------------------------------------------------
    // R11.2 — overlap with existing approved/pending request
    //   existsBy... returns true → BadRequestException("dayoff.overlap.existing"), no save.
    // -------------------------------------------------------------------------

    @Test
    void createDayOffRequest_overlapsExistingRequest_throwsBadRequestException() {
        UserEntity user = Fixtures.buildUser(USER_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(dayOffRepository.existsByRequestedByIdAndDeleteFalseAndStartTimeLessThanAndEndTimeGreaterThan(
                USER_ID, WEEKDAY_END, WEEKDAY_START)).thenReturn(true);
        when(messageService.getMessage("dayoff.overlap.existing")).thenReturn("Overlaps existing request");

        assertThrows(BadRequestException.class,
                () -> commandService.createDayOffRequest(principal(), request(WEEKDAY_START, WEEKDAY_END)));
        verify(dayOffRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // end not after start → BadRequestException("dayoff.end.after.start"), no save.
    // -------------------------------------------------------------------------

    @Test
    void createDayOffRequest_endNotAfterStart_throwsBadRequestException() {
        UserEntity user = Fixtures.buildUser(USER_ID);
        // end == start (not strictly after)
        LocalDateTime start = LocalDateTime.of(2026, 6, 23, 9, 0);
        LocalDateTime end   = LocalDateTime.of(2026, 6, 23, 9, 0);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(messageService.getMessage("dayoff.end.after.start")).thenReturn("End must be after start");

        assertThrows(BadRequestException.class,
                () -> commandService.createDayOffRequest(principal(), request(start, end)));
        verify(dayOffRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // weekend day in range → BadRequestException("dayoff.overlap.weekend"), no save.
    // 2026-06-27 is a Saturday (verified).
    // Range Monday 2026-06-22 → Saturday 2026-06-27 crosses a weekend boundary.
    // -------------------------------------------------------------------------

    @Test
    void createDayOffRequest_rangeIncludesSaturday_throwsBadRequestException() {
        UserEntity user = Fixtures.buildUser(USER_ID);
        // Friday → Saturday crosses into weekend
        LocalDateTime start = LocalDateTime.of(2026, 6, 26, 9, 0);  // Friday
        LocalDateTime end   = LocalDateTime.of(2026, 6, 27, 18, 0); // Saturday

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(messageService.getMessage("dayoff.overlap.weekend")).thenReturn("Range includes a weekend day");

        assertThrows(BadRequestException.class,
                () -> commandService.createDayOffRequest(principal(), request(start, end)));
        verify(dayOffRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // user not found → BadRequestException("user.not.found"), no save.
    // -------------------------------------------------------------------------

    @Test
    void createDayOffRequest_userNotFound_throwsBadRequestException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(messageService.getMessage("user.not.found", USER_ID)).thenReturn("User not found");

        assertThrows(BadRequestException.class,
                () -> commandService.createDayOffRequest(principal(), request(WEEKDAY_START, WEEKDAY_END)));
        verify(dayOffRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // KNOWN GAP R11.3 — EDayOffType classification not wired into DayOffEntity /
    // createDayOffRequest.
    //
    // EDayOffType (FULL, PARTIAL) exists as an enum but:
    //   - DayOffEntity has no 'type' field.
    //   - CreateDayOffRequest has no 'type' field.
    //   - DayOffCommandService.createDayOffRequest never sets a type on the entity.
    // Re-enable and implement assertions once the field is wired in.
    // -------------------------------------------------------------------------

    @org.junit.jupiter.api.Disabled(
            "KNOWN GAP: R11.3 EDayOffType classification not wired into DayOffEntity / " +
            "createDayOffRequest — EDayOffType enum (FULL, PARTIAL) exists but no type field is " +
            "present on DayOffEntity or CreateDayOffRequest. Re-enable once the field is added.")
    @Test
    void createDayOffRequest_typeClassification_r11_3_notImplemented() {
        throw new AssertionError("R11.3 EDayOffType classification not yet implemented on createDayOffRequest path");
    }
}
