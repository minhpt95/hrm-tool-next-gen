package com.vatek.hrmtoolnextgen.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates remaining loggable working hours per day when a day-off interval overlaps
 * with fixed work windows (09:00–12:00, 13:30–18:30), excluding weekends and configured holidays.
 */
@Service
@RequiredArgsConstructor
public class WorkHoursCalculatorService {

    private final HolidayService holidayService;

    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 30);
    private static final LocalTime WORK_END = LocalTime.of(18, 30);

    private static final List<TimeWindow> WORK_WINDOWS = List.of(
            new TimeWindow(WORK_START, LUNCH_START),
            new TimeWindow(LUNCH_END, WORK_END)
    );

    private static final double DAILY_WORK_HOURS = WORK_WINDOWS.stream()
            .mapToDouble(TimeWindow::hours)
            .sum();

    public double getDailyWorkHours() {
        return DAILY_WORK_HOURS;
    }

    /**
     * Computes remaining loggable hours for each working day affected by a day-off interval.
     * Weekends and holidays return 0.
     *
     * @param dayOffStart start of the day-off interval
     * @param dayOffEnd   end of the day-off interval
     * @return ordered map of date -> remaining hours for that date
     */
    public Map<LocalDate, Double> calculateRemainingHours(LocalDateTime dayOffStart, LocalDateTime dayOffEnd) {
        if (dayOffEnd.isBefore(dayOffStart) || dayOffEnd.isEqual(dayOffStart)) {
            throw new IllegalArgumentException("dayOffEnd must be after dayOffStart");
        }

        Map<LocalDate, Double> remainingByDate = new LinkedHashMap<>();

        LocalDate current = dayOffStart.toLocalDate();
        LocalDate endDate = dayOffEnd.toLocalDate();

        while (!current.isAfter(endDate)) {
            if (isWeekend(current)) {
                remainingByDate.put(current, 0.0);
            } else {
                double remaining = remainingHoursForDate(current, dayOffStart, dayOffEnd);
                remainingByDate.put(current, remaining);
            }
            current = current.plusDays(1);
        }

        return remainingByDate;
    }

    private double remainingHoursForDate(LocalDate date, LocalDateTime dayOffStart, LocalDateTime dayOffEnd) {
        LocalDateTime dayStart = date.atTime(WORK_START);
        LocalDateTime dayEnd = date.atTime(WORK_END);

        // If the day-off interval does not intersect this workday, full hours remain
        if (dayOffEnd.isBefore(dayStart) || dayOffStart.isAfter(dayEnd)) {
            return DAILY_WORK_HOURS;
        }

        long overlapSeconds = 0;
        for (TimeWindow window : WORK_WINDOWS) {
            LocalDateTime windowStart = date.atTime(window.start());
            LocalDateTime windowEnd = date.atTime(window.end());

            LocalDateTime overlapStart = max(windowStart, dayOffStart);
            LocalDateTime overlapEnd = min(windowEnd, dayOffEnd);

            if (overlapEnd.isAfter(overlapStart)) {
                overlapSeconds += Duration.between(overlapStart, overlapEnd).getSeconds();
            }
        }

        double overlapHours = overlapSeconds / 3600.0;
        double remaining = DAILY_WORK_HOURS - overlapHours;
        return remaining < 0 ? 0 : remaining;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    private record TimeWindow(LocalTime start, LocalTime end) {
        double hours() {
            return Duration.between(start, end).toMinutes() / 60.0;
        }
    }
}

