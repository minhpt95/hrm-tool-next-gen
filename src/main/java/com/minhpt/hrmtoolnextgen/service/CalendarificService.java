package com.minhpt.hrmtoolnextgen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.holiday.CalendarificResponse;
import com.minhpt.hrmtoolnextgen.dto.holiday.HolidayDto;
import com.minhpt.hrmtoolnextgen.exception.InternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class CalendarificService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;

    @Value("${calendarific.api.key:}")
    private String apiKey;

    @Value("${calendarific.api.url:https://calendarific.com/api/v2/holidays}")
    private String apiUrl;

    private static final String COUNTRY_CODE = "VN"; // Vietnam
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Cacheable(value = "holidays", key = "#year")
    public List<HolidayDto> getHolidays(int year) {
        return normalize(fetchHolidays(year));
    }

    @SuppressWarnings("null")
    public List<HolidayDto> fetchHolidays(int year) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Calendarific API key is not configured. Returning empty list.");
            return new ArrayList<>();
        }

        try {
            String url = UriComponentsBuilder.fromUriString(Objects.requireNonNullElse(apiUrl, ""))
                    .queryParam("api_key", apiKey)
                    .queryParam("country", COUNTRY_CODE)
                    .queryParam("year", year)
                    .toUriString();

            log.debug("Fetching holidays from Calendarific API for year: {}", year);
            CalendarificResponse response = restTemplate.getForObject(url, CalendarificResponse.class);

            if (response == null || response.getResponse() == null || response.getResponse().getHolidays() == null) {
                log.warn("No holidays data received from Calendarific API");
                return new ArrayList<>();
            }

            if (response.getMeta() != null && response.getMeta().getCode() != null && response.getMeta().getCode() != 200) {
                log.error("Calendarific API returned error code: {}", response.getMeta().getCode());
                InternalServerException ex = new InternalServerException();
                ex.setMessage(messageService.getMessage("holiday.fetch.api.error"));
                ex.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                throw ex;
            }

            return response.getResponse().getHolidays().stream()
                    .map(this::mapToHolidayDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching holidays from Calendarific API", e);
            InternalServerException ex = new InternalServerException();
            ex.setMessage(messageService.getMessage("holiday.fetch.error"));
            ex.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            throw ex;
        }
    }

    /**
     * Get the proxy of this service to enable cache interception
     *
     * @return The proxied service instance
     */
    private HolidayDto mapToHolidayDto(CalendarificResponse.CalendarificHoliday holiday) {
        HolidayDto dto = new HolidayDto();
        dto.setName(holiday.getName());
        dto.setDescription(holiday.getDescription());
        dto.setType(holiday.getTypes() != null && !holiday.getTypes().isEmpty()
                ? holiday.getTypes().getFirst() : "Unknown");
        dto.setIsPublic(holiday.getTypes() != null && holiday.getTypes().contains("National holiday"));
        dto.setCountry(holiday.getCountry() != null ? holiday.getCountry().getId() : COUNTRY_CODE);

        // Parse date
        if (holiday.getDate() != null && holiday.getDate().getIso() != null) {
            try {
                dto.setDate(LocalDate.parse(holiday.getDate().getIso().substring(0, 10), DATE_FORMATTER));
            } catch (Exception e) {
                log.warn("Error parsing holiday date: {}", holiday.getDate().getIso(), e);
                // Fallback to datetime if available
                if (holiday.getDate().getDatetime() != null) {
                    var dt = holiday.getDate().getDatetime();
                    dto.setDate(LocalDate.of(dt.getYear(), dt.getMonth(), dt.getDay()));
                }
            }
        }

        // Parse locations
        if (holiday.getLocations() != null && !holiday.getLocations().isEmpty()) {
            dto.setLocations(List.of(holiday.getLocations().split(",")));
        }

        return dto;
    }

    private List<HolidayDto> normalize(List<?> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<HolidayDto> normalized = new ArrayList<>(raw.size());
        for (Object item : raw) {
            if (item instanceof HolidayDto dto) {
                normalized.add(dto);
            } else {
                normalized.add(objectMapper.convertValue(item, HolidayDto.class));
            }
        }
        return normalized;
    }
}
