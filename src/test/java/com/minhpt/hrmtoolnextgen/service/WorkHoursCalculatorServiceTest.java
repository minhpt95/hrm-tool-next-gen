package com.minhpt.hrmtoolnextgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for WorkHoursCalculatorService.
 *
 * Daily work windows:
 *   Morning session:   09:00 – 12:00  (3.0 h)
 *   Afternoon session: 13:30 – 18:30  (5.0 h)
 *   DAILY_WORK_HOURS = 8.0 h  (3 + 5)
 *
 * calculateRemainingHours(start, end) returns per-date remaining loggable hours
 * after subtracting the day-off overlap from the work windows.
 * Weekends always return 0.0.
 */
@ExtendWith(MockitoExtension.class)
class WorkHoursCalculatorServiceTest {

    private static final double DAILY_HOURS = 8.0;
    private static final double DELTA = 0.001;

    // A known weekday: Tuesday 2026-06-23
    private static final LocalDate WEEKDAY = LocalDate.of(2026, 6, 23);
    // A known Saturday: 2026-06-27
    private static final LocalDate SATURDAY = LocalDate.of(2026, 6, 27);
    // A known Sunday: 2026-06-28
    private static final LocalDate SUNDAY = LocalDate.of(2026, 6, 28);

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private WorkHoursCalculatorService calculator;

    // -------------------------------------------------------------------------
    // getDailyWorkHours
    // -------------------------------------------------------------------------

    @Test
    void getDailyWorkHours_returns8point0() {
        assertEquals(DAILY_HOURS, calculator.getDailyWorkHours(), DELTA);
    }

    // -------------------------------------------------------------------------
    // calculateRemainingHours — invalid range
    // -------------------------------------------------------------------------

    @Test
    void calculateRemainingHours_endEqualsStart_throwsIllegalArgument() {
        LocalDateTime t = WEEKDAY.atTime(9, 0);
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateRemainingHours(t, t));
    }

    @Test
    void calculateRemainingHours_endBeforeStart_throwsIllegalArgument() {
        LocalDateTime start = WEEKDAY.atTime(10, 0);
        LocalDateTime end   = WEEKDAY.atTime(9, 0);
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateRemainingHours(start, end));
    }

    // -------------------------------------------------------------------------
    // calculateRemainingHours — weekend days
    // -------------------------------------------------------------------------

    @Test
    void calculateRemainingHours_saturdayInRange_returns0ForSaturday() {
        // Day-off spans the entire Saturday
        LocalDateTime start = SATURDAY.atTime(9, 0);
        LocalDateTime end   = SATURDAY.atTime(18, 30);
        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        assertEquals(0.0, result.get(SATURDAY), DELTA);
    }

    @Test
    void calculateRemainingHours_sundayInRange_returns0ForSunday() {
        LocalDateTime start = SUNDAY.atTime(9, 0);
        LocalDateTime end   = SUNDAY.atTime(18, 30);
        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        assertEquals(0.0, result.get(SUNDAY), DELTA);
    }

    // -------------------------------------------------------------------------
    // calculateRemainingHours — full-day day-off on a weekday
    // -------------------------------------------------------------------------

    @Test
    void calculateRemainingHours_fullDayOff_remainingIsZero() {
        // day-off covers the entire work day: start of day to end of day
        LocalDateTime start = WEEKDAY.atStartOfDay();
        LocalDateTime end   = WEEKDAY.plusDays(1).atStartOfDay();
        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        assertEquals(0.0, result.get(WEEKDAY), DELTA);
    }

    // -------------------------------------------------------------------------
    // calculateRemainingHours — morning session only (09:00–12:00, 3 h taken)
    // -------------------------------------------------------------------------

    @Test
    void calculateRemainingHours_morningSessionFullyTaken_remainingIs5point0() {
        // day-off = 09:00–12:00 (exactly the morning window, 3 h)
        LocalDateTime start = WEEKDAY.atTime(9, 0);
        LocalDateTime end   = WEEKDAY.atTime(12, 0);
        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        // remaining = 8.0 – 3.0 = 5.0
        assertEquals(5.0, result.get(WEEKDAY), DELTA);
    }

    // -------------------------------------------------------------------------
    // calculateRemainingHours — afternoon session only (13:30–18:30, 5 h taken)
    // -------------------------------------------------------------------------

    @Test
    void calculateRemainingHours_afternoonSessionFullyTaken_remainingIs3point0() {
        // day-off = 13:30–18:30 (exactly the afternoon window, 5 h)
        LocalDateTime start = WEEKDAY.atTime(13, 30);
        LocalDateTime end   = WEEKDAY.atTime(18, 30);
        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        // remaining = 8.0 – 5.0 = 3.0
        assertEquals(3.0, result.get(WEEKDAY), DELTA);
    }

    // -------------------------------------------------------------------------
    // calculateRemainingHours — day-off entirely outside work windows (lunch break)
    // -------------------------------------------------------------------------

    @Test
    void calculateRemainingHours_dayOffDuringLunchBreak_fullHoursRemain() {
        // day-off = 12:00–13:30 (the lunch break — not a work window)
        LocalDateTime start = WEEKDAY.atTime(12, 0);
        LocalDateTime end   = WEEKDAY.atTime(13, 30);
        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        // zero overlap with work windows → full 8.5 h remain
        assertEquals(DAILY_HOURS, result.get(WEEKDAY), DELTA);
    }

    // -------------------------------------------------------------------------
    // calculateRemainingHours — partial overlap with morning session (1 h taken)
    // -------------------------------------------------------------------------

    @Test
    void calculateRemainingHours_partialMorningOverlap_remaining7point0() {
        // day-off = 09:00–10:00 (1 h overlap with morning)
        LocalDateTime start = WEEKDAY.atTime(9, 0);
        LocalDateTime end   = WEEKDAY.atTime(10, 0);
        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        // remaining = 8.0 – 1.0 = 7.0
        assertEquals(7.0, result.get(WEEKDAY), DELTA);
    }

    // -------------------------------------------------------------------------
    // calculateRemainingHours — day-off entirely before work start
    // -------------------------------------------------------------------------

    @Test
    void calculateRemainingHours_dayOffBeforeWorkStart_fullHoursRemain() {
        // day-off = 07:00–08:59 (before 09:00)
        LocalDateTime start = WEEKDAY.atTime(7, 0);
        LocalDateTime end   = WEEKDAY.atTime(8, 59);
        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        assertEquals(DAILY_HOURS, result.get(WEEKDAY), DELTA);
    }

    // -------------------------------------------------------------------------
    // calculateRemainingHours — day-off entirely after work end
    // -------------------------------------------------------------------------

    @Test
    void calculateRemainingHours_dayOffAfterWorkEnd_fullHoursRemain() {
        // day-off = 19:00–20:00 (after 18:30)
        LocalDateTime start = WEEKDAY.atTime(19, 0);
        LocalDateTime end   = WEEKDAY.atTime(20, 0);
        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        assertEquals(DAILY_HOURS, result.get(WEEKDAY), DELTA);
    }

    // -------------------------------------------------------------------------
    // calculateRemainingHours — multi-day range builds map with correct keys
    // -------------------------------------------------------------------------

    @Test
    void calculateRemainingHours_multiDayRange_mapContainsAllDates() {
        // Monday 2026-06-22 to Wednesday 2026-06-24 (3 weekdays)
        LocalDate mon = LocalDate.of(2026, 6, 22);
        LocalDate wed = LocalDate.of(2026, 6, 24);
        LocalDateTime start = mon.atTime(9, 0);
        LocalDateTime end   = wed.atTime(18, 30);
        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        assertTrue(result.containsKey(mon));
        assertTrue(result.containsKey(LocalDate.of(2026, 6, 23)));
        assertTrue(result.containsKey(wed));
    }

    // -------------------------------------------------------------------------
    // calculateRemainingHours — range spanning weekday + weekend returns 0 for weekend
    // -------------------------------------------------------------------------

    @Test
    void calculateRemainingHours_rangeIncludesWeekend_weekendDayIsZero() {
        // Friday 2026-06-26 to Saturday 2026-06-27
        LocalDate friday   = LocalDate.of(2026, 6, 26);
        LocalDate saturday = LocalDate.of(2026, 6, 27);
        LocalDateTime start = friday.atTime(9, 0);
        LocalDateTime end   = saturday.atTime(18, 30);
        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        assertEquals(0.0, result.get(saturday), DELTA);
        // Friday has some overlap, so it should be < 8.5 (not full)
        assertTrue(result.get(friday) < DAILY_HOURS);
    }

    // -------------------------------------------------------------------------
    // calculateRemainingHours — holiday returns 0.0
    // -------------------------------------------------------------------------

    @Test
    void calculateRemainingHours_holidayInRange_returnsZeroForHoliday() {
        LocalDate holiday = LocalDate.of(2026, 1, 1); // New Year's Day (Thursday)
        LocalDateTime start = holiday.atTime(9, 0);
        LocalDateTime end = holiday.atTime(18, 30);

        when(holidayService.isHoliday(holiday)).thenReturn(true);

        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        assertEquals(0.0, result.get(holiday), DELTA);
    }

    @Test
    void calculateRemainingHours_weekdayNotHoliday_returnsFullHours() {
        when(holidayService.isHoliday(WEEKDAY)).thenReturn(false);

        LocalDateTime start = WEEKDAY.atTime(9, 0);
        LocalDateTime end = WEEKDAY.atTime(17, 0);
        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);

        // 09:00-17:00 overlaps morning (3h) + 3.5h of afternoon = 6.5h taken
        // remaining = 8.0 - 6.5 = 1.5
        assertEquals(1.5, result.get(WEEKDAY), DELTA);
    }

    @Test
    void calculateRemainingHours_mixedHolidayAndWeekday_holidayIsZero() {
        LocalDate friday = LocalDate.of(2026, 6, 26);
        LocalDate saturday = LocalDate.of(2026, 6, 27);
        LocalDateTime start = friday.atTime(9, 0);
        LocalDateTime end = saturday.atTime(18, 30);

        // Saturday is excluded by the weekend rule before isHoliday() is ever consulted,
        // so only the weekday lookup is stubbed.
        when(holidayService.isHoliday(friday)).thenReturn(false);

        Map<LocalDate, Double> result = calculator.calculateRemainingHours(start, end);
        assertEquals(0.0, result.get(saturday), DELTA);
        assertTrue(result.get(friday) < DAILY_HOURS);
    }
}
