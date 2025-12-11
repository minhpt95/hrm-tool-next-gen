package com.vatek.hrmtoolnextgen.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * JPA Converter to convert between LocalDateTime (database) and ZonedDateTime (entity)
 * MySQL DATETIME doesn't store timezone information, so we convert to/from system default timezone
 */
@Converter(autoApply = true)
public class JpaZonedDateTimeConverter implements AttributeConverter<ZonedDateTime, LocalDateTime> {

    @Override
    public LocalDateTime convertToDatabaseColumn(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault());
    }
}

