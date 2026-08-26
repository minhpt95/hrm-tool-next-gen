package com.minhpt.hrmtoolnextgen.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link TimeUtils} — LocalTime ↔ decimal-hours conversion, addition,
 * and null-tolerant comparison, including the documented 24h clamp.
 */
class TimeUtilsTest {

    private static final double DELTA = 1e-9;

    // -------------------------------------------------------------------------
    // convertTimeToHours
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "8, 30, 8.5",
            "0, 0,  0.0",
            "1, 15, 1.25",
            "23, 59, 23.983333333333334",
            "7, 45, 7.75"
    })
    void convertTimeToHours_convertsMinutesToFraction(int hour, int minute, double expected) {
        assertEquals(expected, TimeUtils.convertTimeToHours(LocalTime.of(hour, minute)), DELTA);
    }

    @Test
    void convertTimeToHours_null_returnsZero() {
        assertEquals(0.0, TimeUtils.convertTimeToHours(null), DELTA);
    }

    // -------------------------------------------------------------------------
    // convertHoursToTime
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "8.5,  8, 30",
            "0.0,  0, 0",
            "1.25, 1, 15",
            "7.75, 7, 45"
    })
    void convertHoursToTime_convertsFractionToMinutes(double hours, int expectedHour, int expectedMinute) {
        assertEquals(LocalTime.of(expectedHour, expectedMinute), TimeUtils.convertHoursToTime(hours));
    }

    @Test
    void convertHoursToTime_roundsToNearestMinute() {
        // 1.008333h == 1h 0.5min -> rounds up to 1 minute
        assertEquals(LocalTime.of(1, 1), TimeUtils.convertHoursToTime(1.0083334));
    }

    @Test
    void convertHoursToTime_atOrAbove24Hours_clampsToEndOfDay() {
        assertEquals(LocalTime.of(23, 59), TimeUtils.convertHoursToTime(24.0));
        assertEquals(LocalTime.of(23, 59), TimeUtils.convertHoursToTime(30.5));
    }

    @Test
    void convertHoursToTime_negative_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TimeUtils.convertHoursToTime(-0.5));
        assertTrue(ex.getMessage().contains("negative"));
    }

    // -------------------------------------------------------------------------
    // addTimes — nulls are treated as LocalTime.MIN
    // -------------------------------------------------------------------------

    @Test
    void addTimes_sumsBothOperands() {
        assertEquals(LocalTime.of(12, 0), TimeUtils.addTimes(LocalTime.of(8, 30), LocalTime.of(3, 30)));
    }

    @Test
    void addTimes_nullOperands_treatedAsZero() {
        assertEquals(LocalTime.of(8, 30), TimeUtils.addTimes(LocalTime.of(8, 30), null));
        assertEquals(LocalTime.of(2, 15), TimeUtils.addTimes(null, LocalTime.of(2, 15)));
        assertEquals(LocalTime.MIN, TimeUtils.addTimes(null, null));
    }

    @Test
    void addTimes_overflowingSum_clampsToEndOfDay() {
        assertEquals(LocalTime.of(23, 59), TimeUtils.addTimes(LocalTime.of(20, 0), LocalTime.of(10, 0)));
    }

    // -------------------------------------------------------------------------
    // compareTimes — null-tolerant ordering
    // -------------------------------------------------------------------------

    @Test
    void compareTimes_ordersNonNullValues() {
        assertTrue(TimeUtils.compareTimes(LocalTime.of(9, 0), LocalTime.of(8, 0)) > 0);
        assertTrue(TimeUtils.compareTimes(LocalTime.of(7, 0), LocalTime.of(8, 0)) < 0);
        assertEquals(0, TimeUtils.compareTimes(LocalTime.of(8, 0), LocalTime.of(8, 0)));
    }

    @Test
    void compareTimes_nullsSortFirst() {
        assertEquals(0, TimeUtils.compareTimes(null, null));
        assertEquals(-1, TimeUtils.compareTimes(null, LocalTime.of(8, 0)));
        assertEquals(1, TimeUtils.compareTimes(LocalTime.of(8, 0), null));
    }

    // -------------------------------------------------------------------------
    // Round trip
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({"8, 30", "0, 0", "13, 45", "23, 59"})
    void convertTimeToHours_thenBack_isLossless(int hour, int minute) {
        LocalTime original = LocalTime.of(hour, minute);
        assertEquals(original, TimeUtils.convertHoursToTime(TimeUtils.convertTimeToHours(original)));
    }
}
