package com.jpd.web.service;

import com.jpd.web.model.*;
import com.jpd.web.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlyBalanceService {
    
    private final MonthlyCreatorBalanceRepository balanceRepository;
    private final CreatorRepository creatorRepository;
    
    /**
     * Chạy tự động vào 00:05 ngày đầu tiên của mỗi tháng
     */
    @Scheduled(cron = "0 5 0 1 * ?")
    @Transactional
    public void calculateMonthlyBalances() {
        log.info("Starting monthly balance calculation...");
        
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        int year = lastMonth.getYear();
        int month = lastMonth.getMonthValue();
        
        List<Creator> allCreators = creatorRepository.findAll();
        
        for (Creator creator : allCreators) {
            try {
                calculateAndSaveMonthlyBalance(creator, year, month);
            } catch (Exception e) {
                log.error("Error calculating balance for creator {}: {}", 
                    creator.getCreatorId(), e.getMessage());
            }
        }
        
        log.info("Monthly balance calculation completed for {}-{}", year, month);
    }
    
    /**
     * Calculate và save monthly balance
     */
    @Transactional
    public MonthlyCreatorBalance calculateAndSaveMonthlyBalance(
        Creator creator, int year, int month
    ) {
        // Kiểm tra đã tồn tại chưa
        MonthlyCreatorBalance balance = balanceRepository
            .findByCreator_CreatorIdAndYearAndMonth(creator.getCreatorId(), year, month)
            .orElse(MonthlyCreatorBalance.builder()
                .creator(creator)
                .year(year)
                .month(month)
                .build());
        
        // Lấy danh sách courses có phí
        List<Course> paidCourses = creator.getCourses().stream()
            .filter(c -> c.getAccessMode() == AccessMode.PAID)
            .toList();
        
        // Tính toán các metrics
        int totalCourses = paidCourses.size();
        
        // Lấy tất cả enrollments của tháng đó
        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);
        
        List<Enrollment> monthEnrollments = paidCourses.stream()
            .flatMap(c -> c.getEnrollments().stream())
            .filter(e -> {
                LocalDateTime createDate = e.getCreateDate();
                return !createDate.isBefore(startOfMonth) && !createDate.isAfter(endOfMonth);
            })
            .toList();
        
        // Tất cả enrollments (để tính tổng students)
        List<Enrollment> allEnrollments = paidCourses.stream()
            .flatMap(c -> c.getEnrollments().stream())
            .toList();
        
        long totalStudents = allEnrollments.stream()
            .map(e -> e.getCustomer().getCustomerId())
            .distinct()
            .count();
        
        long newEnrollments = monthEnrollments.size();
        
        // Tính completion rate
        long completed = allEnrollments.stream()
            .filter(Enrollment::isFinish)
            .count();
        double completionRate = totalStudents == 0 ? 0 : (double) completed / allEnrollments.size() * 100;
        
        // Tính rating
        List<Integer> ratings = allEnrollments.stream()
            .filter(e -> e.getFeedback() != null)
            .map(e -> e.getFeedback().getRate())
            .toList();
        
        double avgRating = ratings.isEmpty()
            ? 0
            : ratings.stream().mapToInt(Integer::intValue).average().orElse(0);
        
        long totalReviews = ratings.size();
        
        // Tính revenue của tháng
        double totalRevenue = monthEnrollments.stream()
            .mapToDouble(e -> e.getCourse().getPrice())
            .sum();
        
        // Lấy top 4 popular courses (theo số students)
        List<Long> popularCourseIds = paidCourses.stream()
            .sorted((c1, c2) -> Integer.compare(
                c2.getEnrollments().size(), 
                c1.getEnrollments().size()
            ))
            .limit(4)
            .map(Course::getCourseId)
            .collect(Collectors.toList());
        
        // Update balance
        balance.setTotalRevenue(totalRevenue);
        balance.setTotalStudents(totalStudents);
        balance.setTotalCourses(totalCourses);
        balance.setAvgRating(avgRating);
        balance.setCompletionRate(completionRate);
        balance.setNewEnrollments(newEnrollments);
        balance.setTotalReviews(totalReviews);
        balance.setPopularCourses(popularCourseIds);
        balance.setLastUpdated(LocalDateTime.now());
        
        return balanceRepository.save(balance);
    }
    
    /**
     * Get dashboard data cho creator
     */
    public MonthlyCreatorBalance getCurrentMonthDashboard(Long creatorId) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        
        // Tìm hoặc tạo mới cho tháng hiện tại
        return balanceRepository.findCurrentMonth(creatorId, year, month)
            .orElseGet(() -> {
                Creator creator = creatorRepository.findById(creatorId)
                    .orElseThrow(() -> new RuntimeException("Creator not found"));
                return calculateAndSaveMonthlyBalance(creator, year, month);
            });
    }
    
    /**
     * Get historical data (12 tháng gần nhất)
     */
    public List<MonthlyCreatorBalance> getLast12Months(Long creatorId) {
        return balanceRepository.findLast12Months(
            creatorId, 
            org.springframework.data.domain.PageRequest.of(0, 12)
        );
    }
}