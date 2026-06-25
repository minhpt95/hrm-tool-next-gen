package com.minhpt.hrmtoolnextgen.controller;

import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.USER;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.minhpt.hrmtoolnextgen.component.TokenBucketRateLimiter;
import com.minhpt.hrmtoolnextgen.dto.holiday.HolidayDto;
import com.minhpt.hrmtoolnextgen.service.HolidayService;

/**
 * Integration tests for HolidayController.
 *
 * Security gate: HOLIDAY_ENDPOINTS → .authenticated()
 * Any authenticated user passes; unauthenticated → 401.
 *
 * @MockBean HolidayService keeps tests deterministic — no Redis, no Calendarific call.
 * @MockBean TokenBucketRateLimiter allows all requests through (tryConsume → true).
 *
 * Param names (verified from controller source):
 *   /holidays/range  → @RequestParam LocalDate startDate, LocalDate endDate
 *   /holidays/check  → @RequestParam LocalDate date
 *
 * R19.1 GET /holidays/year/{year}   → 200, $.data is array of holiday objects
 * R19.2 GET /holidays/current       → 200, $.data is array (body contract, not deprecation header)
 * R19.3 GET /holidays/range         → 200, $.data filtered list
 * R19.4 GET /holidays/check         → 200, $.data boolean
 * Auth  unauthenticated → 401
 */
@SpringBootTest
@AutoConfigureMockMvc
class HolidayControllerIntegrationTest {

    @SuppressWarnings("unused")
    @TestConfiguration
    static class MailTestConfig {
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }

    @MockBean
    private HolidayService holidayService;

    @MockBean
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    @Autowired
    private MockMvc mockMvc;

    private List<HolidayDto> sampleHolidays;

    @BeforeEach
    void setUp() {
        // Allow all rate-limit checks through
        when(tokenBucketRateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(true);

        HolidayDto newYear = new HolidayDto();
        newYear.setName("New Year");
        newYear.setDate(LocalDate.of(2026, 1, 1));
        newYear.setType("National holiday");
        newYear.setIsPublic(true);
        newYear.setCountry("VN");

        HolidayDto tet = new HolidayDto();
        tet.setName("Tet Holiday");
        tet.setDate(LocalDate.of(2026, 1, 29));
        tet.setType("National holiday");
        tet.setIsPublic(true);
        tet.setCountry("VN");

        sampleHolidays = List.of(newYear, tet);
    }

    // -------------------------------------------------------------------------
    // R19.1 — GET /api/v1/holidays/year/{year} → 200, list in $.data
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = USER)
    void getHolidaysByYear_authenticated_returns200WithHolidayList() throws Exception {
        when(holidayService.getHolidaysByYear(2026)).thenReturn(sampleHolidays);

        mockMvc.perform(get("/api/v1/holidays/year/2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("New Year"))
                .andExpect(jsonPath("$.data[1].name").value("Tet Holiday"));
    }

    @Test
    @WithMockUser(authorities = USER)
    void getHolidaysByYear_legacyPath_returns200WithHolidayList() throws Exception {
        when(holidayService.getHolidaysByYear(2026)).thenReturn(sampleHolidays);

        mockMvc.perform(get("/api/holidays/year/2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = USER)
    void getHolidaysByYear_emptyResult_returns200WithEmptyArray() throws Exception {
        when(holidayService.getHolidaysByYear(2020)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/holidays/year/2020"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // -------------------------------------------------------------------------
    // R19.2 — GET /api/v1/holidays/current → 200, body $.data is array
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = USER)
    void getCurrentYearHolidays_authenticated_returns200WithHolidayList() throws Exception {
        when(holidayService.getCurrentYearHolidays()).thenReturn(sampleHolidays);

        mockMvc.perform(get("/api/v1/holidays/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("New Year"));
    }

    // -------------------------------------------------------------------------
    // R19.3 — GET /api/v1/holidays/range?startDate=...&endDate=...
    //         Param names: startDate, endDate (ISO date format yyyy-MM-dd)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = USER)
    void getHolidaysByRange_validDates_returns200WithFilteredList() throws Exception {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end   = LocalDate.of(2026, 3, 31);
        when(holidayService.getHolidaysByRange(start, end)).thenReturn(sampleHolidays);

        mockMvc.perform(get("/api/v1/holidays/range")
                        .param("startDate", "2026-01-01")
                        .param("endDate",   "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("New Year"));
    }

    @Test
    @WithMockUser(authorities = USER)
    void getHolidaysByRange_legacyPath_returns200() throws Exception {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end   = LocalDate.of(2026, 6, 30);
        when(holidayService.getHolidaysByRange(start, end)).thenReturn(List.of());

        mockMvc.perform(get("/api/holidays/range")
                        .param("startDate", "2026-01-01")
                        .param("endDate",   "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // -------------------------------------------------------------------------
    // R19.4 — GET /api/v1/holidays/check?date=...
    //         Param name: date (ISO date format yyyy-MM-dd)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = USER)
    void isHoliday_dateIsHoliday_returns200WithTrue() throws Exception {
        LocalDate holidayDate = LocalDate.of(2026, 1, 1);
        when(holidayService.isHoliday(holidayDate)).thenReturn(true);

        mockMvc.perform(get("/api/v1/holidays/check")
                        .param("date", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @WithMockUser(authorities = USER)
    void isHoliday_dateIsNotHoliday_returns200WithFalse() throws Exception {
        LocalDate notHoliday = LocalDate.of(2026, 3, 16);
        when(holidayService.isHoliday(notHoliday)).thenReturn(false);

        mockMvc.perform(get("/api/v1/holidays/check")
                        .param("date", "2026-03-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @WithMockUser(authorities = USER)
    void isHoliday_legacyPath_returns200() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 2);
        when(holidayService.isHoliday(date)).thenReturn(true);

        mockMvc.perform(get("/api/holidays/check")
                        .param("date", "2026-09-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    // -------------------------------------------------------------------------
    // Auth — unauthenticated requests → 401 (HOLIDAY_ENDPOINTS → .authenticated())
    // -------------------------------------------------------------------------

    @Test
    void getHolidaysByYear_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/holidays/year/2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentYearHolidays_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/holidays/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getHolidaysByRange_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/holidays/range")
                        .param("startDate", "2026-01-01")
                        .param("endDate",   "2026-12-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void isHoliday_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/holidays/check")
                        .param("date", "2026-01-01"))
                .andExpect(status().isUnauthorized());
    }
}
