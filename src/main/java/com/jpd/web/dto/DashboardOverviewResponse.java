package com.jpd.web.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {
    private Long totalUsers;
//  private Long totalLearners;
  private Long totalCreators;
  private Long totalCourses;
  private Long totalEnrollments;
  private Double totalRevenue;
  private Double totalCreatorRevenue;
  private Map<String, Long> transactionStatusCount;
  private List<TopCreatorData> topCreators;
}
