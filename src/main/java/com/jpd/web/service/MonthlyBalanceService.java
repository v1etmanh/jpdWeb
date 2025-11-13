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
import java.util.Map;
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
        MonthlyCreatorBalance balance = balanceRepository
            .findByCreator_CreatorIdAndYearAndMonth(creator.getCreatorId(), year, month)
            .orElse(MonthlyCreatorBalance.builder()
                .creator(creator)
                .year(year)
                .month(month)
                .build());
        
        List<Course> paidCourses = creator.getCourses().stream()
            
            .toList();
        
        // Time range của tháng
        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);
        
        // ✅ Lấy enrollments TRONG THÁNG
        List<Enrollment> monthEnrollments = paidCourses.stream()
            .flatMap(c -> c.getEnrollments().stream())
            .filter(e -> {
                LocalDateTime createDate = e.getCreateDate();
                return !createDate.isBefore(startOfMonth) && !createDate.isAfter(endOfMonth);
            })
            .toList();
        
        // ✅ Số học viên MỚI trong tháng (distinct customers)
        long totalStudents = monthEnrollments.stream()
            .map(e -> e.getCustomer().getCustomerId())
            .distinct()
            .count();
        
        // ✅ Số khóa học ĐƯỢC MUA trong tháng (distinct courses)
        int totalCourses = (int) monthEnrollments.stream()
            .map(e -> e.getCourse().getCourseId())
            .distinct()
            .count();
        
        long newEnrollments = monthEnrollments.size();
        
        // ✅ Completion rate chỉ tính cho enrollments trong tháng
        long completed = monthEnrollments.stream()
            .filter(Enrollment::isFinish)
            .count();
        double completionRate = monthEnrollments.isEmpty() 
            ? 0 
            : (double) completed / monthEnrollments.size() * 100;
        
        // ✅ Rating chỉ tính cho feedbacks trong tháng
        List<Integer> ratings = monthEnrollments.stream()
            .filter(e -> e.getFeedback() != null)
            .map(e -> e.getFeedback().getRate())
            .toList();
        
        double avgRating = ratings.isEmpty()
            ? 0
            : ratings.stream().mapToInt(Integer::intValue).average().orElse(0);
        
        long totalReviews = ratings.size();
        
        // ✅ Revenue của tháng (đã đúng)
        double totalRevenue = monthEnrollments.stream()
            .mapToDouble(e -> e.getCourse().getPrice())
            .sum();
        
        // ✅ Popular courses TRONG THÁNG (theo số enrollments trong tháng)
        List<Long> popularCourseIds = monthEnrollments.stream()
            .collect(Collectors.groupingBy(
                e -> e.getCourse().getCourseId(),
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
            .limit(4)
            .map(Map.Entry::getKey)
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
    public MonthlyCreatorBalance getCurrentMonthDashboard(Long creatorId,int month, int year) {
        LocalDateTime now = LocalDateTime.now();
        
        
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