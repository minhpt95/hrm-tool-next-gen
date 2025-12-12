package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.holiday.HolidayDto;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.vatek.hrmtoolnextgen.service.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/${hrm.api.prefix}/holidays")
@Tag(name = "Holidays", description = "Vietnam holidays and lunar calendar holidays API")
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping("/year/{year}")
    @Operation(
            summary = "Get holidays for a specific year",
            description = "Retrieves all Vietnam public holidays and lunar holidays for the specified year"
    )
    public ResponseEntity<CommonSuccessResponse<List<HolidayDto>>> getHolidaysByYear(
            @Parameter(description = "Year to get holidays for", example = "2024")
            @PathVariable int year,
            HttpServletRequest request) {
        List<HolidayDto> holidays = holidayService.getHolidays(year);
        return ResponseEntity.ok(buildSuccessResponse(holidays, request));
    }

    @GetMapping("/current")
    @Operation(
            summary = "Get holidays for current year",
            description = "Retrieves all Vietnam public holidays and lunar holidays for the current year"
    )
    public ResponseEntity<CommonSuccessResponse<List<HolidayDto>>> getCurrentYearHolidays(
            HttpServletRequest request) {
        List<HolidayDto> holidays = holidayService.getCurrentYearHolidays();
        return ResponseEntity.ok(buildSuccessResponse(holidays, request));
    }

    @GetMapping("/range")
    @Operation(
            summary = "Get holidays for a date range",
            description = "Retrieves all Vietnam holidays within the specified date range"
    )
    public ResponseEntity<CommonSuccessResponse<List<HolidayDto>>> getHolidaysByRange(
            @Parameter(description = "Start date (format: yyyy-MM-dd)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (format: yyyy-MM-dd)", example = "2024-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        List<HolidayDto> holidays = holidayService.getHolidays(startDate, endDate);
        return ResponseEntity.ok(buildSuccessResponse(holidays, request));
    }

    @GetMapping("/check")
    @Operation(
            summary = "Check if a date is a holiday",
            description = "Checks if the specified date is a public holiday in Vietnam"
    )
    public ResponseEntity<CommonSuccessResponse<Boolean>> isHoliday(
            @Parameter(description = "Date to check (format: yyyy-MM-dd)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        boolean isHoliday = holidayService.isHoliday(date);
        return ResponseEntity.ok(buildSuccessResponse(isHoliday, request));
    }

    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .data(data)
                .build();
    }
}

