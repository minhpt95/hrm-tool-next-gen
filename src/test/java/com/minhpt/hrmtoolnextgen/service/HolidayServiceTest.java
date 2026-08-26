package com.minhpt.hrmtoolnextgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.minhpt.hrmtoolnextgen.dto.holiday.HolidayDto;

/**
 * Unit tests for HolidayService.
 *
 * HolidayService is a thin delegation layer over CalendarificService.
 * CalendarificService is mocked — no live HTTP, no Redis.
 *
 * R19.1 getHolidaysByYear   — delegates to calendarificService.getHolidays(year)
 * R19.2 getCurrentYearHolidays — calls getHolidaysByYear(LocalDate.now().getYear())
 * R19.3 getHolidaysByRange  — aggregates across years, filters to [start..end] inclusive
 * R19.4 isHoliday           — true iff HolidayDto.getDate() equals the given date
 *
 * R19.6 cache-hit note: @Cacheable lives on CalendarificService.getHolidays, not on
 * HolidayService. A Mockito mock of CalendarificService bypasses the Spring proxy, so
 * cache behaviour cannot be observed here. See CalendarificServiceTest for the
 * mapping path; cache-hit determinism (second call → RestTemplate invoked once) requires
 * a Spring context with a non-Redis cache manager and is documented as needing the
 * integration harness.
 */
@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private CalendarificService calendarificService;

    @InjectMocks
    private HolidayService holidayService;

    // -------------------------------------------------------------------------
    // R19.1 — getHolidaysByYear delegates to calendarificService.getHolidays
    // -------------------------------------------------------------------------

    @Test
    void getHolidaysByYear_delegatesToCalendarificService_returnsHolidayList() {
        // Arrange
        List<HolidayDto> expected = List.of(holiday("New Year", LocalDate.of(2026, 1, 1)));
        when(calendarificService.getHolidays(2026)).thenReturn(expected);

        // Act
        List<HolidayDto> result = holidayService.getHolidaysByYear(2026);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("New Year", result.getFirst().getName());
        verify(calendarificService).getHolidays(2026);
    }

    @Test
    void getHolidaysByYear_emptyList_returnsEmpty() {
        when(calendarificService.getHolidays(2025)).thenReturn(List.of());

        List<HolidayDto> result = holidayService.getHolidaysByYear(2025);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(calendarificService).getHolidays(2025);
    }

    // -------------------------------------------------------------------------
    // R19.2 — getCurrentYearHolidays uses LocalDate.now().getYear()
    // -------------------------------------------------------------------------

    @Test
    void getCurrentYearHolidays_invokesCalendarificWithCurrentYear() {
        // Arrange
        int currentYear = LocalDate.now().getYear();
        List<HolidayDto> expected = List.of(
                holiday("Reunification Day", LocalDate.of(currentYear, 4, 30)));
        when(calendarificService.getHolidays(currentYear)).thenReturn(expected);

        // Act
        List<HolidayDto> result = holidayService.getCurrentYearHolidays();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(calendarificService).getHolidays(currentYear);
    }

    @Test
    void getCurrentYearHolidays_whenNoHolidays_returnsEmptyList() {
        int currentYear = LocalDate.now().getYear();
        when(calendarificService.getHolidays(currentYear)).thenReturn(List.of());

        List<HolidayDto> result = holidayService.getCurrentYearHolidays();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // R19.3 — getHolidaysByRange filters to [startDate..endDate] inclusive
    //         and aggregates across multiple years when range spans a year boundary
    // -------------------------------------------------------------------------

    @Test
    void getHolidaysByRange_singleYear_returnsHolidaysWithinRange() {
        // Arrange — three holidays; only the middle one falls in range
        List<HolidayDto> year2026 = List.of(
                holiday("Before",  LocalDate.of(2026, 3, 14)),
                holiday("InRange", LocalDate.of(2026, 4, 30)),
                holiday("After",   LocalDate.of(2026, 9, 2))
        );
        when(calendarificService.getHolidays(2026)).thenReturn(year2026);

        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end   = LocalDate.of(2026, 6, 30);

        // Act
        List<HolidayDto> result = holidayService.getHolidaysByRange(start, end);

        // Assert
        assertEquals(1, result.size());
        assertEquals("InRange", result.getFirst().getName());
        verify(calendarificService).getHolidays(2026);
    }

    @Test
    void getHolidaysByRange_inclusiveBoundaries_includesStartAndEndDates() {
        // Arrange — holidays exactly on start and end of range
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end   = LocalDate.of(2026, 12, 31);
        List<HolidayDto> year2026 = List.of(
                holiday("New Year",          start),
                holiday("National Day",      LocalDate.of(2026, 9, 2)),
                holiday("New Year Eve",      end)
        );
        when(calendarificService.getHolidays(2026)).thenReturn(year2026);

        // Act
        List<HolidayDto> result = holidayService.getHolidaysByRange(start, end);

        // Assert — both boundary dates are included
        assertEquals(3, result.size());
    }

    @Test
    void getHolidaysByRange_multiYear_aggregatesAcrossYearBoundary() {
        // Arrange — range spans 2026-11-01 to 2027-02-28
        List<HolidayDto> year2026 = List.of(
                holiday("Christmas",  LocalDate.of(2026, 12, 25)),
                holiday("Oct Holiday", LocalDate.of(2026, 10, 1))  // outside range
        );
        List<HolidayDto> year2027 = List.of(
                holiday("Tet 2027",   LocalDate.of(2027, 1, 29)),
                holiday("March OOB",  LocalDate.of(2027, 3, 8))    // outside range
        );
        when(calendarificService.getHolidays(2026)).thenReturn(year2026);
        when(calendarificService.getHolidays(2027)).thenReturn(year2027);

        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end   = LocalDate.of(2027, 2, 28);

        // Act
        List<HolidayDto> result = holidayService.getHolidaysByRange(start, end);

        // Assert — 1 from 2026 (Christmas) + 1 from 2027 (Tet) = 2 total
        assertEquals(2, result.size());
        verify(calendarificService).getHolidays(2026);
        verify(calendarificService).getHolidays(2027);
    }

    @Test
    void getHolidaysByRange_noHolidaysInRange_returnsEmptyList() {
        when(calendarificService.getHolidays(2026)).thenReturn(List.of(
                holiday("Far Away", LocalDate.of(2026, 12, 25))
        ));

        List<HolidayDto> result = holidayService.getHolidaysByRange(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // R19.4 — isHoliday returns true when date is in the year's holiday list
    // -------------------------------------------------------------------------

    @Test
    void isHoliday_dateMatchesHoliday_returnsTrue() {
        // Arrange
        LocalDate holidayDate = LocalDate.of(2026, 9, 2);
        when(calendarificService.getHolidays(2026)).thenReturn(List.of(
                holiday("National Day", holidayDate)
        ));

        // Act & Assert
        assertTrue(holidayService.isHoliday(holidayDate));
        verify(calendarificService).getHolidays(2026);
    }

    @Test
    void isHoliday_dateNotInHolidayList_returnsFalse() {
        // Arrange — holiday list contains a different date
        when(calendarificService.getHolidays(2026)).thenReturn(List.of(
                holiday("National Day", LocalDate.of(2026, 9, 2))
        ));

        // Act & Assert
        assertFalse(holidayService.isHoliday(LocalDate.of(2026, 9, 3)));
    }

    @Test
    void isHoliday_emptyHolidayList_returnsFalse() {
        when(calendarificService.getHolidays(2026)).thenReturn(List.of());

        assertFalse(holidayService.isHoliday(LocalDate.of(2026, 1, 1)));
    }

    @Test
    void isHoliday_usesYearFromGivenDate() {
        // isHoliday must call getHolidays with the year of the checked date — not a hard-coded year
        LocalDate dateIn2025 = LocalDate.of(2025, 4, 30);
        when(calendarificService.getHolidays(2025)).thenReturn(List.of(
                holiday("Reunification Day", dateIn2025)
        ));

        assertTrue(holidayService.isHoliday(dateIn2025));
        verify(calendarificService, times(1)).getHolidays(2025);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static HolidayDto holiday(String name, LocalDate date) {
        HolidayDto dto = new HolidayDto();
        dto.setName(name);
        dto.setDate(date);
        return dto;
    }
}
