package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardChartResponse {
    private RevenueChartData revenueChart;
    private String period; //MONTH / YEAR / WEEK
}