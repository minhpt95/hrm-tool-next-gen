package com.minhpt.hrmtoolnextgen.service;

import com.minhpt.hrmtoolnextgen.dto.holiday.HolidayDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class HolidayService {

    private final CalendarificService calendarificService;

    public List<HolidayDto> getHolidaysByYear(int year) {
        return calendarificService.getHolidays(year);
    }

    /**
     * Get Vietnam holidays for a date range
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of holidays in the date range
     */

    public List<HolidayDto> getHolidaysByRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Getting holidays from {} to {}", startDate, endDate);
        List<HolidayDto> allHolidays = new ArrayList<>();
        int startYear = startDate.getYear();
        int endYear = endDate.getYear();

        for (int year = startYear; year <= endYear; year++) {
            List<HolidayDto> yearHolidays = getHolidaysByYear(year);
            allHolidays.addAll(yearHolidays.stream()
                    .filter(holiday -> {
                        LocalDate holidayDate = holiday.getDate();
                        return !holidayDate.isBefore(startDate) && !holidayDate.isAfter(endDate);
                    })
                    .toList());
        }

        return allHolidays;
    }

    /**
     * Check if a specific date is a holiday in Vietnam
     *
     * @param date The date to check
     * @return true if the date is a holiday
     */
    public boolean isHoliday(LocalDate date) {
        log.debug("Checking if date {} is a holiday", date);
        List<HolidayDto> holidays = getHolidaysByYear(date.getYear());
        return holidays.stream().anyMatch(holiday -> holiday.getDate().equals(date));
    }

    /**
     * Get holidays for the current year
     *
     * @return List of holidays for current year
     */
    public List<HolidayDto> getCurrentYearHolidays() {
        log.debug("Getting current year holidays");
        return getHolidaysByYear(LocalDate.now().getYear());
    }


}

