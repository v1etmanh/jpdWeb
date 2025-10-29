package com.jpd.web.dto;

import com.jpd.web.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreatorDetailDto {
    // Basic info
    private Long creatorId;
    private String fullName;
    private String email;
    private String phone;
    private String titleSelf;
    private String imageUrl;

    // Status & verification
    private Status status;
    private List<String> certificateUrls;
    private String paymentEmail;

    // Statistics
    private Double balance;
    private Double totalRevenue;
    private Integer totalCourses;
    private Integer totalStudents;
    private Double avgRating;

    // Moderation
    private Integer warningCount;
    private Integer reputationScore;
    private Boolean isBanned;
    private Date bannedUntil;

    // Recent activity
    private List<ReportSummaryDto> recentReports; // last 10
    private List<CourseCardDto> recentCourses; // top 5

    // Audit
    private Date createDate;
    private Date lastActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportSummaryDto {
        private Long reportId;
        private String reportType;
        private String detail;
        private String status;
        private LocalDate createdAt;
        private LocalDate reviewedAt;
    }
}
