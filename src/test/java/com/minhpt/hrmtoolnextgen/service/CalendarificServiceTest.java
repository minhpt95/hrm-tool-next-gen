package com.minhpt.hrmtoolnextgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.holiday.CalendarificResponse;
import com.minhpt.hrmtoolnextgen.dto.holiday.HolidayDto;
import com.minhpt.hrmtoolnextgen.exception.InternalServerException;

/**
 * Unit tests for CalendarificService.fetchHolidays (the external-call path).
 *
 * @Cacheable lives on getHolidays; fetchHolidays is the raw RestTemplate call.
 * These tests verify the RestTemplate→HolidayDto mapping without a live HTTP call.
 *
 * R19.5 — fetchHolidays maps CalendarificResponse to List<HolidayDto>:
 *   - name, description mapped directly
 *   - date parsed from iso string (first 10 chars → LocalDate)
 *   - type = first element of types list (or "Unknown" if null/empty)
 *   - isPublic = types contains "National holiday"
 *   - country = country.id (falls back to "VN")
 *   - locations = split on ","
 *
 * R19.6 cache-hit note: the @Cacheable proxy wrapping getHolidays requires a
 * Spring context with a non-Redis CacheManager to assert single RestTemplate
 * invocation across two calls. With Mockito @InjectMocks the proxy is absent,
 * so a second call to fetchHolidays via getHolidays will invoke RestTemplate
 * again — this is expected when running without Spring. The cache-hit assertion
 * (RestTemplate called exactly once for two getHolidays calls) is deferred to an
 * integration test that activates a simple/in-memory CacheManager.
 */
@ExtendWith(MockitoExtension.class)
class CalendarificServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private CalendarificService calendarificService;

    @BeforeEach
    void injectDependencies() {
        // ObjectMapper is not a Spring bean in tests — supply a real instance
        ReflectionTestUtils.setField(calendarificService, "objectMapper",
                new ObjectMapper().registerModule(new JavaTimeModule()));
        // Provide a non-blank API key so fetchHolidays does not short-circuit
        ReflectionTestUtils.setField(calendarificService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(calendarificService, "apiUrl",
                "https://calendarific.com/api/v2/holidays");
    }

    // -------------------------------------------------------------------------
    // R19.5 — happy path: response mapped to HolidayDto list
    // -------------------------------------------------------------------------

    @Test
    void fetchHolidays_validResponse_mapsToHolidayDtoList() {
        // Arrange
        CalendarificResponse response = buildResponse(200,
                List.of(calendarificHoliday(
                        "New Year",
                        "New Year celebration",
                        "2026-01-01T00:00:00+07:00",
                        2026, 1, 1,
                        List.of("National holiday"),
                        "VN",
                        null
                ))
        );
        when(restTemplate.getForObject(anyString(), eq(CalendarificResponse.class)))
                .thenReturn(response);

        // Act
        List<HolidayDto> result = calendarificService.fetchHolidays(2026);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        HolidayDto dto = result.getFirst();
        assertEquals("New Year", dto.getName());
        assertEquals("New Year celebration", dto.getDescription());
        assertEquals(LocalDate.of(2026, 1, 1), dto.getDate());
        assertEquals("National holiday", dto.getType());
        assertTrue(dto.getIsPublic());
        assertEquals("VN", dto.getCountry());

        verify(restTemplate).getForObject(anyString(), eq(CalendarificResponse.class));
    }

    @Test
    void fetchHolidays_multipleHolidays_allMapped() {
        CalendarificResponse response = buildResponse(200, List.of(
                calendarificHoliday("Tet", "Lunar New Year", "2026-01-29T00:00:00+07:00",
                        2026, 1, 29, List.of("National holiday"), "VN", null),
                calendarificHoliday("Reunification", "National holiday", "2026-04-30T00:00:00+07:00",
                        2026, 4, 30, List.of("National holiday"), "VN", null)
        ));
        when(restTemplate.getForObject(anyString(), eq(CalendarificResponse.class)))
                .thenReturn(response);

        List<HolidayDto> result = calendarificService.fetchHolidays(2026);

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2026, 1, 29), result.get(0).getDate());
        assertEquals(LocalDate.of(2026, 4, 30), result.get(1).getDate());
    }

    // -------------------------------------------------------------------------
    // R19.5 — type mapping edge cases
    // -------------------------------------------------------------------------

    @Test
    void fetchHolidays_nullTypes_typeIsUnknownAndIsPublicFalse() {
        CalendarificResponse response = buildResponse(200, List.of(
                calendarificHoliday("Observance", "desc", "2026-03-08T00:00:00+07:00",
                        2026, 3, 8, null, "VN", null)
        ));
        when(restTemplate.getForObject(anyString(), eq(CalendarificResponse.class)))
                .thenReturn(response);

        List<HolidayDto> result = calendarificService.fetchHolidays(2026);

        assertEquals("Unknown", result.getFirst().getType());
        assertNotNull(result.getFirst().getIsPublic());
        assertFalse(result.getFirst().getIsPublic());
    }

    @Test
    void fetchHolidays_nonNationalHolidayType_isPublicFalse() {
        CalendarificResponse response = buildResponse(200, List.of(
                calendarificHoliday("Bank Holiday", "desc", "2026-06-15T00:00:00+07:00",
                        2026, 6, 15, List.of("Observance"), "VN", null)
        ));
        when(restTemplate.getForObject(anyString(), eq(CalendarificResponse.class)))
                .thenReturn(response);

        List<HolidayDto> result = calendarificService.fetchHolidays(2026);

        assertEquals("Observance", result.getFirst().getType());
        assertFalse(result.getFirst().getIsPublic());
    }

    // -------------------------------------------------------------------------
    // R19.5 — location mapping
    // -------------------------------------------------------------------------

    @Test
    void fetchHolidays_locationsCsvString_splitIntoList() {
        CalendarificResponse response = buildResponse(200, List.of(
                calendarificHoliday("Regional", "desc", "2026-05-01T00:00:00+07:00",
                        2026, 5, 1, List.of("Observance"), "VN", "Hanoi,Ho Chi Minh City")
        ));
        when(restTemplate.getForObject(anyString(), eq(CalendarificResponse.class)))
                .thenReturn(response);

        List<HolidayDto> result = calendarificService.fetchHolidays(2026);

        assertNotNull(result.getFirst().getLocations());
        assertEquals(2, result.getFirst().getLocations().size());
    }

    // -------------------------------------------------------------------------
    // R19.5 — null / empty response handled gracefully
    // -------------------------------------------------------------------------

    @Test
    void fetchHolidays_nullResponse_returnsEmptyList() {
        when(restTemplate.getForObject(anyString(), eq(CalendarificResponse.class)))
                .thenReturn(null);

        List<HolidayDto> result = calendarificService.fetchHolidays(2026);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchHolidays_nullHolidaysInsideResponse_returnsEmptyList() {
        CalendarificResponse response = new CalendarificResponse();
        CalendarificResponse.Response inner = new CalendarificResponse.Response();
        inner.setHolidays(null);
        response.setResponse(inner);
        response.setMeta(new CalendarificResponse.Meta(200));

        when(restTemplate.getForObject(anyString(), eq(CalendarificResponse.class)))
                .thenReturn(response);

        List<HolidayDto> result = calendarificService.fetchHolidays(2026);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchHolidays_apiKeyBlank_returnsEmptyListWithoutCallingRestTemplate() {
        ReflectionTestUtils.setField(calendarificService, "apiKey", "");

        List<HolidayDto> result = calendarificService.fetchHolidays(2026);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        // RestTemplate must NOT be called — no live HTTP
        verify(restTemplate, org.mockito.Mockito.never())
                .getForObject(anyString(), eq(CalendarificResponse.class));
    }

    // -------------------------------------------------------------------------
    // R19.5 — non-200 meta code throws InternalServerException
    // -------------------------------------------------------------------------

    @Test
    void fetchHolidays_nonOkMetaCode_throwsInternalServerException() {
        CalendarificResponse response = buildResponse(401, List.of());
        when(restTemplate.getForObject(anyString(), eq(CalendarificResponse.class)))
                .thenReturn(response);
        when(messageService.getMessage("holiday.fetch.api.error")).thenReturn("API error");
        // The source's broad catch(Exception e) re-wraps the typed meta-code exception,
        // so the message that actually propagates is "holiday.fetch.error" (latent src smell —
        // the meta-code branch throws InternalServerException which is then caught and wrapped again).
        when(messageService.getMessage("holiday.fetch.error")).thenReturn("Fetch error");

        InternalServerException ex = assertThrows(InternalServerException.class,
                () -> calendarificService.fetchHolidays(2026));

        // Verify the meta-code branch executed: it called getMessage for the api-error key
        // before the catch block re-wrapped it with the generic fetch-error key.
        verify(messageService).getMessage("holiday.fetch.api.error");
        // Verify the re-wrap path also executed (the message that actually propagates).
        verify(messageService).getMessage("holiday.fetch.error");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // R19.6 — cache-hit contract (deferred: requires Spring context)
    // -------------------------------------------------------------------------

    @Disabled("R19.6: @Cacheable cache-hit (2nd getHolidays served from cache, RestTemplate invoked once) requires a Spring context with an in-memory CacheManager — not observable under @InjectMocks; no live Redis in the harness")
    @Test
    void cacheHit_secondGetHolidaysCall_doesNotInvokeRestTemplateAgain() {
        // Desired contract:
        //   CalendarificResponse response = buildResponse(200, List.of(...));
        //   when(restTemplate.getForObject(anyString(), eq(CalendarificResponse.class))).thenReturn(response);
        //   calendarificService.getHolidays(2026); // first call — populates cache
        //   calendarificService.getHolidays(2026); // second call — should hit cache
        //   verify(restTemplate, times(1)).getForObject(anyString(), eq(CalendarificResponse.class));
        // This assertion requires the @Cacheable proxy to be active (Spring context + CacheManager).
        // Under @InjectMocks the proxy is absent; both calls reach RestTemplate directly.
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static CalendarificResponse buildResponse(int code,
            List<CalendarificResponse.CalendarificHoliday> holidays) {
        CalendarificResponse response = new CalendarificResponse();
        response.setMeta(new CalendarificResponse.Meta(code));
        CalendarificResponse.Response inner = new CalendarificResponse.Response();
        inner.setHolidays(holidays);
        response.setResponse(inner);
        return response;
    }

    private static CalendarificResponse.CalendarificHoliday calendarificHoliday(
            String name, String description, String isoDate,
            int year, int month, int day,
            List<String> types, String countryId, String locations) {

        CalendarificResponse.CalendarificHoliday h = new CalendarificResponse.CalendarificHoliday();
        h.setName(name);
        h.setDescription(description);
        h.setTypes(types);
        h.setLocations(locations);

        CalendarificResponse.Country country = new CalendarificResponse.Country();
        country.setId(countryId);
        h.setCountry(country);

        CalendarificResponse.DateTime dt = new CalendarificResponse.DateTime(year, month, day);
        CalendarificResponse.HolidayDate holidayDate = new CalendarificResponse.HolidayDate(isoDate, dt);
        h.setDate(holidayDate);

        return h;
    }
}
