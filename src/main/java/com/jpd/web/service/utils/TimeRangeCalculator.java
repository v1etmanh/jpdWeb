package com.jpd.web.service.utils;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public class TimeRangeCalculator {
    
    @Data
    public static class TimeRange {
        private final LocalDateTime start;
        private final LocalDateTime end;
    }
    
    public static TimeRange getDayRange(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return new TimeRange(start, end);
    }
    
    public static TimeRange getWeekRange(LocalDate date) {
        LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate endOfWeek = date.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        return new TimeRange(startOfWeek.atStartOfDay(), endOfWeek.atTime(LocalTime.MAX));
    }
    
    public static TimeRange getMonthRange(int month, int year) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.with(TemporalAdjusters.lastDayOfMonth());
        return new TimeRange(startOfMonth.atStartOfDay(), endOfMonth.atTime(LocalTime.MAX));
    }
    
    public static TimeRange getQuarterRange(int quarter, int year) {
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("Quarter must be between 1 and 4");
        }
        
        int startMonth = (quarter - 1) * 3 + 1;
        int endMonth = startMonth + 2;
        
        LocalDate startOfQuarter = LocalDate.of(year, startMonth, 1);
        LocalDate endOfQuarter = LocalDate.of(year, endMonth, 1)
                .with(TemporalAdjusters.lastDayOfMonth());
        
        return new TimeRange(startOfQuarter.atStartOfDay(), endOfQuarter.atTime(LocalTime.MAX));
    }
    
    public static TimeRange getYearRange(int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        return new TimeRange(startOfYear.atStartOfDay(), endOfYear.atTime(LocalTime.MAX));
    }
    
    public static String getPeriodLabel(String periodType, int value, int year) {
        return switch (periodType.toUpperCase()) {
            case "DAY" -> String.format("Day %d, %d", value, year);
            case "WEEK" -> String.format("Week %d, %d", value, year);
            case "MONTH" -> String.format("Month %d, %d", value, year);
            case "QUARTER" -> String.format("Q%d %d", value, year);
            case "YEAR" -> String.format("Year %d", year);
            default -> "Unknown Period";
        };
    }
}



