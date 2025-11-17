package com.jpd.web.controller.admin;


import com.jpd.web.dto.DashboardChartResponse;
import com.jpd.web.dto.DashboardOverviewResponse;
import com.jpd.web.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;


    /**
     * Lấy dữ liệu overview cho dashboard
     *
     */
    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverviewDashboard() {
        try {
            DashboardOverviewResponse response = dashboardService.getOverviewDashboard();
            log.info("Overview Dashboard Data: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy dữ liệu chart
     *
     * @param period: WEEK, MONTH, YEAR (default: MONTH)
     */
    @GetMapping("/chart")
    public ResponseEntity<DashboardChartResponse> getChartDashboard(
            @RequestParam(defaultValue = "MONTH") String period) {
        try {
            DashboardChartResponse response = dashboardService.getChartDashboard(period);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}