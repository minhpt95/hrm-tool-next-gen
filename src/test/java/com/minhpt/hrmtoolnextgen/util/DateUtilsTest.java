package com.minhpt.hrmtoolnextgen.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DateUtils} — conversion helpers and their default patterns
 * (dd/MM/yyyy for dates, dd/MM/yyyy HH:mm for date-times).
 */
class DateUtilsTest {

    // -------------------------------------------------------------------------
    // Instant <-> Date
    // -------------------------------------------------------------------------

    @Test
    void convertInstantToDate_preservesEpochMillis() {
        Instant instant = Instant.parse("2026-05-04T10:15:30Z");

        Date date = DateUtils.convertInstantToDate(instant);

        assertNotNull(date);
        assertEquals(instant.toEpochMilli(), date.getTime());
    }

    // -------------------------------------------------------------------------
    // LocalDate -> String
    // -------------------------------------------------------------------------

    @Test
    void convertLocalDateToStringDate_usesDefaultDdMmYyyyPattern() {
        assertEquals("04/05/2026",
                DateUtils.convertLocalDateToStringDate(LocalDate.of(2026, 5, 4)));
    }

    @Test
    void convertLocalDateToStringDate_honoursExplicitPattern() {
        assertEquals("2026-05-04",
                DateUtils.convertLocalDateToStringDate(LocalDate.of(2026, 5, 4), "yyyy-MM-dd"));
    }

    @Test
    void convertLocalDateToStringDate_nullPattern_fallsBackToDefault() {
        assertEquals("04/05/2026",
                DateUtils.convertLocalDateToStringDate(LocalDate.of(2026, 5, 4), null));
    }

    // -------------------------------------------------------------------------
    // String -> LocalDate
    // -------------------------------------------------------------------------

    @Test
    void convertStringDateToLocalDate_usesDefaultPattern() {
        assertEquals(LocalDate.of(2026, 5, 4), DateUtils.convertStringDateToLocalDate("04/05/2026"));
    }

    @Test
    void convertStringDateToLocalDate_honoursExplicitPattern() {
        assertEquals(LocalDate.of(2026, 5, 4),
                DateUtils.convertStringDateToLocalDate("2026-05-04", "yyyy-MM-dd"));
    }

    @Test
    void convertStringDateToLocalDate_patternMismatch_throws() {
        assertThrows(DateTimeParseException.class,
                () -> DateUtils.convertStringDateToLocalDate("2026-05-04"));
    }

    @Test
    void localDate_roundTripsThroughString() {
        LocalDate original = LocalDate.of(2026, 12, 31);
        assertEquals(original, DateUtils.convertStringDateToLocalDate(
                DateUtils.convertLocalDateToStringDate(original)));
    }

    // -------------------------------------------------------------------------
    // String -> LocalDateTime
    // -------------------------------------------------------------------------

    @Test
    void convertStringDateTimeToLocalDateTime_usesDefaultPattern() {
        assertEquals(LocalDateTime.of(2026, 5, 4, 14, 30),
                DateUtils.convertStringDateTimeToLocalDateTime("04/05/2026 14:30"));
    }

    @Test
    void convertStringDateTimeToLocalDateTime_honoursExplicitPattern() {
        assertEquals(LocalDateTime.of(2026, 5, 4, 14, 30, 15),
                DateUtils.convertStringDateTimeToLocalDateTime(
                        "2026-05-04T14:30:15", "yyyy-MM-dd'T'HH:mm:ss"));
    }

    @Test
    void convertStringDateTimeToLocalDateTime_patternMismatch_throws() {
        assertThrows(DateTimeParseException.class,
                () -> DateUtils.convertStringDateTimeToLocalDateTime("not-a-date"));
    }

    // -------------------------------------------------------------------------
    // Clock-reading helpers
    // -------------------------------------------------------------------------

    @Test
    void getInstantLong_returnsCurrentEpochMillis() {
        long before = Instant.now().toEpochMilli();
        Long value = DateUtils.getInstantLong();
        long after = Instant.now().toEpochMilli();

        assertNotNull(value);
        assertTrue(value >= before && value <= after,
                () -> "expected " + value + " within [" + before + ", " + after + "]");
    }

    @Test
    void getLocalDateTimeNow_isCloseToSystemClock() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime value = DateUtils.getLocalDateTimeNow();
        LocalDateTime after = LocalDateTime.now();

        assertTrue(!value.isBefore(before) && !value.isAfter(after));
    }
}
