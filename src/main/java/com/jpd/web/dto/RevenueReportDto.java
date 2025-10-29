package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportDto {
    // Period info
    private String periodType; // DAY, WEEK, MONTH, QUARTER, YEAR
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Revenue summary
    private Double totalRevenue;
    private Double adminRevenue;
    private Double creatorRevenue;

    // Transaction counts
    private Long totalTransactions;
    private Long successfulTransactions;
    private Long failedTransactions;
    private Long pendingTransactions;

    // Metrics
    private Double averageTransactionValue;
    private Double successRate; // Percentage

    // Top performers
    private List<CourseRevenueSummary> topCourses;
    private List<CreatorRevenueSummary> topCreators;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseRevenueSummary {
        private Long courseId;
        private String courseName;
        private String imageUrl;
        private Double totalRevenue;
        private Long enrollmentCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatorRevenueSummary {
        private Long creatorId;
        private String creatorName;
        private Double totalRevenue;
        private Long courseCount;
    }
}


