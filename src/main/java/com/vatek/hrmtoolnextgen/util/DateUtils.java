package com.vatek.hrmtoolnextgen.util;

import com.vatek.hrmtoolnextgen.constant.DateConstant;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Log4j2
public class DateUtils {
    public static Date convertInstantToDate(Instant instant) {
        return Date.from(instant);
    }

    public static String convertLocalDateToStringDate(LocalDate localDate) {
        return convertLocalDateToStringDate(localDate, null);
    }

    public static String convertLocalDateToStringDate(LocalDate instant, String datePattern) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(datePattern != null ? datePattern : DateConstant.DD_MM_YYYY).withZone(ZoneId.systemDefault());
        return dateTimeFormatter.format(instant);
    }

    public static Long getInstantLong() {
        return Instant.now().toEpochMilli();
    }

    public static LocalDateTime getLocalDateTimeNow() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }

    public static LocalDate convertStringDateToLocalDate(String stringDate) {
        return convertStringDateToLocalDate(stringDate, null);
    }

    public static LocalDate convertStringDateToLocalDate(String stringDate, String datePattern) {
        String pattern = datePattern != null ? datePattern : DateConstant.DD_MM_YYYY;
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDate.parse(stringDate, dateTimeFormatter);
    }

    public static LocalDateTime convertStringDateTimeToLocalDateTime(String stringDateTime) {
        return convertStringDateTimeToLocalDateTime(stringDateTime, null);
    }

    public static LocalDateTime convertStringDateTimeToLocalDateTime(String stringDateTime, String dateTimePattern) {
        String pattern = dateTimePattern != null ? dateTimePattern : "dd/MM/yyyy HH:mm";
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(stringDateTime, dateTimeFormatter);
    }
}
