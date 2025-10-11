package com.jpd.web.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorDashboardDTO {
    private double totalRevenue;
    private long totalStudents;
    private long totalCourses;
    private double avgRating;
   
    private double completionRate;
    private long newEnrollments;
    private long totalReviews;
  private List<PopularCourseDTO>ppc;
}