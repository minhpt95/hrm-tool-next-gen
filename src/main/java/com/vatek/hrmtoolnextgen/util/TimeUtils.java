package com.vatek.hrmtoolnextgen.util;

import lombok.extern.log4j.Log4j2;

import java.time.LocalTime;

@Log4j2
public class TimeUtils {
    
    /**
     * Converts LocalTime to total hours as double
     * @param time LocalTime object (e.g., 08:30 = 8.5 hours)
     * @return total hours as double
     */
    public static double convertTimeToHours(LocalTime time) {
        if (time == null) {
            return 0.0;
        }
        return time.getHour() + (time.getMinute() / 60.0);
    }
    
    /**
     * Converts total hours (double) to LocalTime
     * @param hours total hours as double (e.g., 8.5 = 08:30)
     * @return LocalTime object
     */
    public static LocalTime convertHoursToTime(double hours) {
        if (hours < 0) {
            throw new IllegalArgumentException("Hours cannot be negative");
        }
        
        int totalMinutes = (int) Math.round(hours * 60);
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        
        // Handle overflow (e.g., if hours > 24)
        if (h >= 24) {
            h = 23;
            m = 59;
        }
        
        return LocalTime.of(h, m);
    }
    
    /**
     * Adds two LocalTime objects
     * @param time1 first time
     * @param time2 second time
     * @return sum as LocalTime
     */
    public static LocalTime addTimes(LocalTime time1, LocalTime time2) {
        if (time1 == null) time1 = LocalTime.MIN;
        if (time2 == null) time2 = LocalTime.MIN;
        
        double hours1 = convertTimeToHours(time1);
        double hours2 = convertTimeToHours(time2);
        return convertHoursToTime(hours1 + hours2);
    }
    
    /**
     * Compares two LocalTime objects
     * @param time1 first time
     * @param time2 second time
     * @return positive if time1 > time2, negative if time1 < time2, 0 if equal
     */
    public static int compareTimes(LocalTime time1, LocalTime time2) {
        if (time1 == null && time2 == null) return 0;
        if (time1 == null) return -1;
        if (time2 == null) return 1;
        return time1.compareTo(time2);
    }
}

