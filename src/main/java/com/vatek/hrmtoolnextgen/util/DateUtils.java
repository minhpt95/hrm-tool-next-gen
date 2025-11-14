package com.vatek.hrmtoolnextgen.util;

import com.vatek.hrmtoolnextgen.constant.DateConstant;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Log4j2
public class DateUtils {
    public static Date convertInstantToDate (Instant instant)
    {
        return Date.from(instant);
    }

    public static String convertInstantToStringDate(ZonedDateTime stringDate){
        return convertInstantToStringDate(stringDate,null);
    }

    public static String convertInstantToStringDate(ZonedDateTime instant, String datePattern){
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(datePattern != null ? datePattern : DateConstant.DD_MM_YYYY).withZone(ZoneId.systemDefault());
        return dateTimeFormatter.format(instant);
    }

    public static Long getInstantLong(){
       return ZonedDateTime.now().toInstant().toEpochMilli();
    }

    public static ZonedDateTime getZonedDateTimeNow(){
        return ZonedDateTime.now(ZoneId.systemDefault());
    }
}
