package com.minhpt.hrmtoolnextgen.controller;

import com.minhpt.hrmtoolnextgen.annotation.RateLimit;
import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.constant.ApiConstant;
import com.minhpt.hrmtoolnextgen.dto.holiday.HolidayDto;
import com.minhpt.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.minhpt.hrmtoolnextgen.service.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping({ApiConstant.HOLIDAYS_BASE, ApiConstant.HOLIDAYS_V1_BASE})
@Tag(name = "Holidays", description = "Vietnam holidays and lunar calendar holidays API")
public class HolidayController {

    private final HolidayService holidayService;
    private final MessageService messageService;

    @GetMapping("/year/{year}")
    @Operation(
            summary = "Get holidays for a specific year",
            description = "Retrieves all Vietnam public holidays and lunar holidays for the specified year"
    )
    @RateLimit(capacity = 5,refillRate = 5,keyPrefix = "ratelimit:holiday-year",strategy = RateLimit.RateLimitStrategy.GLOBAL)
    public ResponseEntity<CommonSuccessResponse<List<HolidayDto>>> getHolidaysByYear(
            @Parameter(description = "Year to get holidays for", example = "2024")
            @PathVariable int year,
            HttpServletRequest request) {
        List<HolidayDto> holidays = holidayService.getHolidaysByYear(year);
        return ResponseEntity.ok(buildSuccessResponse(holidays, request));
    }

    @GetMapping("/current")
    @Operation(
            summary = "Get holidays for current year",
            description = "Retrieves all Vietnam public holidays and lunar holidays for the current year"
    )
    @RateLimit(capacity = 5,refillRate = 5,keyPrefix = "ratelimit:holiday-current",strategy = RateLimit.RateLimitStrategy.GLOBAL)
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
    @RateLimit(capacity = 5,refillRate = 5,keyPrefix = "ratelimit:holiday-range",strategy = RateLimit.RateLimitStrategy.GLOBAL)
    public ResponseEntity<CommonSuccessResponse<List<HolidayDto>>> getHolidaysByRange(
            @Parameter(description = "Start date (format: yyyy-MM-dd)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (format: yyyy-MM-dd)", example = "2024-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        List<HolidayDto> holidays = holidayService.getHolidaysByRange(startDate, endDate);
        return ResponseEntity.ok(buildSuccessResponse(holidays, request));
    }

    @GetMapping("/check")
    @Operation(
            summary = "Check if a date is a holiday",
            description = "Checks if the specified date is a public holiday in Vietnam"
    )
    @RateLimit(capacity = 5,refillRate = 5,keyPrefix = "ratelimit:holiday-check",strategy = RateLimit.RateLimitStrategy.GLOBAL)
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
                .httpStatusCode(HttpStatus.OK)
                .message(messageService.getMessage("success"))
                .data(data)
                .build();
    }
}

