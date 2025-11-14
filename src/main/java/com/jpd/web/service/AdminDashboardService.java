package com.jpd.web.service;


import com.jpd.web.dto.DashboardChartResponse;
import com.jpd.web.dto.DashboardOverviewResponse;
import com.jpd.web.dto.RevenueChartData;
import com.jpd.web.dto.TopCreatorData;
import com.jpd.web.repository.*;
import com.jpd.web.service.utils.TimeRangeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final CustomerTransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CreatorRepository creatorRepository;


    public DashboardOverviewResponse getOverviewDashboard() {
        DashboardOverviewResponse response = new DashboardOverviewResponse();
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        TimeRangeCalculator.TimeRange yearToDate = TimeRangeCalculator.getYearRange(currentYear);
        LocalDateTime now = LocalDateTime.now();
        response.setTotalUsers(customerRepository.count());
        response.setTotalCreators(creatorRepository.count());
        response.setTotalCourses(courseRepository.count());
        response.setTotalEnrollments(enrollmentRepository.count());

        //doanh thu tu dau nam den hien tai
        response.setTotalRevenue(transactionRepository.getAdminRevenue(yearToDate.getStart(), now));
        response.setTotalCreatorRevenue(transactionRepository.getCreatorRevenue(yearToDate.getStart(), now));

        // Thống kê trạng thái giao dịch (từ đầu năm)
        Map<String, Long> statusCount = new HashMap<>();
        statusCount.put("SUCCESS", transactionRepository.countByStatusAndDateRange("SUCCESS", yearToDate.getStart(), now));
        statusCount.put("FAILED", transactionRepository.countByStatusAndDateRange("FAILED", yearToDate.getStart(), now));
        response.setTransactionStatusCount(statusCount);

        // Top 5 creators có doanh thu cao nhất
        List<TopCreatorData> topCreators = transactionRepository
                .getTopCreatorsByRevenue(yearToDate.getStart(), now, PageRequest.of(0, 5))
                .stream()
                .map(projection -> new TopCreatorData(
                        projection.getCreatorId(),
                        projection.getCreatorName(),
                        projection.getTotalRevenue()
                ))
                .toList();
        response.setTopCreators(topCreators);
        return response;
    }


    public DashboardChartResponse getChartDashboard(String period) {
        DashboardChartResponse response = new DashboardChartResponse();
        // Lấy dữ liệu biểu đồ theo period
        RevenueChartData chartData = getRevenueChartData(period);
        response.setRevenueChart(chartData);
        return response;
    }


    private RevenueChartData getRevenueChartData(String period) {
        switch (period.toUpperCase()) {
            case "WEEK":
                return getWeeklyRevenue();
            case "MONTH":
                return getMonthlyRevenue();
            case "YEAR":
                return getYearlyRevenue();
            default:
                return getMonthlyRevenue(); // Default
        }
    }


    /**
     * Lấy doanh thu theo tuần - 12 tuần gần nhất
     * Mỗi tuần bắt đầu từ Thứ Hai và kết thúc Chủ Nhật
     */
    private RevenueChartData getWeeklyRevenue() {
        LocalDate today = LocalDate.now();
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        // Lấy 12 tuần gần nhất (từ tuần cũ nhất đến tuần hiện tại)
        for (int i = 12; i >= 0; i--) {
            // Tính ngày của tuần đó (i tuần trước)
            LocalDate dateInWeek = today.minusWeeks(i);

            // Sử dụng TimeRangeCalculator để lấy range của tuần đó
            TimeRangeCalculator.TimeRange weekRange = TimeRangeCalculator.getWeekRange(dateInWeek);

            // Query doanh thu trong khoảng thời gian đó
            Double revenue = transactionRepository.getAdminRevenue(
                    weekRange.getStart(),
                    weekRange.getEnd()
            );

            // Tính số tuần và năm để hiển thị label
            int weekNumber = dateInWeek.get(WeekFields.ISO.weekOfYear());
            int year = dateInWeek.getYear();

            // Thêm vào kết quả
            labels.add(String.format("Week %d/%d", weekNumber, year));
            data.add(revenue != null ? revenue : 0.0);
        }

        return new RevenueChartData(labels, data, "WEEK");
    }


    /**
     * Lấy doanh thu theo tháng - 12 tháng gần nhất
     */
    private RevenueChartData getMonthlyRevenue() {
        LocalDate today = LocalDate.now();
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        // Lấy 12 tháng gần nhất
        for (int i = 11; i >= 0; i--) {
            LocalDate dateInMonth = today.minusMonths(i);
            int month = dateInMonth.getMonthValue();
            int year = dateInMonth.getYear();

            // Sử dụng TimeRangeCalculator
            TimeRangeCalculator.TimeRange monthRange = TimeRangeCalculator.getMonthRange(month, year);

            // Query doanh thu
            Double revenue = transactionRepository.getAdminRevenue(
                    monthRange.getStart(),
                    monthRange.getEnd()
            );

            // Format label với tên tháng tiếng Việt
            labels.add(String.format("%s %d", "Thg " + month, year));
            data.add(revenue != null ? revenue : 0.0);
        }

        return new RevenueChartData(labels, data, "MONTH");
    }


    /**
     * Lấy doanh thu theo năm - 5 năm gần nhất
     */
    private RevenueChartData getYearlyRevenue() {
        int currentYear = LocalDate.now().getYear();
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        // Lấy 5 năm gần nhất
        for (int i = 4; i >= 0; i--) {
            int year = currentYear - i;

            // Sử dụng TimeRangeCalculator
            TimeRangeCalculator.TimeRange yearRange = TimeRangeCalculator.getYearRange(year);

            // Query doanh thu
            Double revenue = transactionRepository.getAdminRevenue(
                    yearRange.getStart(),
                    yearRange.getEnd()
            );

            labels.add(String.valueOf(year));
            data.add(revenue != null ? revenue : 0.0);
        }

        return new RevenueChartData(labels, data, "YEAR");
    }


}
