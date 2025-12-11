package com.vatek.hrmtoolnextgen.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * JPA Converter to convert between LocalDate (database DATE) and ZonedDateTime (entity)
 * MySQL DATE doesn't store timezone information, so we convert to/from system default timezone
 * at start of day (00:00:00)
 */
@Converter
public class JpaZonedDateTimeDateConverter implements AttributeConverter<ZonedDateTime, LocalDate> {

    @Override
    public LocalDate convertToDatabaseColumn(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay(ZoneId.systemDefault());
    }
}

