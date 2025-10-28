package com.jpd.web.repository;

import com.jpd.web.model.MonthlyCreatorBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyCreatorBalanceRepository extends JpaRepository<MonthlyCreatorBalance, Long> {
    
    Optional<MonthlyCreatorBalance> findByCreator_CreatorIdAndYearAndMonth(
        Long creatorId, int year, int month
    );
    
    List<MonthlyCreatorBalance> findByCreator_CreatorIdOrderByYearDescMonthDesc(
        Long creatorId
    );
    
    @Query("SELECT m FROM MonthlyCreatorBalance m WHERE m.creator.creatorId = :creatorId " +
           "AND m.year = :year ORDER BY m.month DESC")
    List<MonthlyCreatorBalance> findByCreatorAndYear(
        @Param("creatorId") Long creatorId, 
        @Param("year") int year
    );
    
    @Query("SELECT m FROM MonthlyCreatorBalance m WHERE m.creator.creatorId = :creatorId " +
           "ORDER BY m.year DESC, m.month DESC")
    List<MonthlyCreatorBalance> findLast12Months(
        @Param("creatorId") Long creatorId, 
        org.springframework.data.domain.Pageable pageable
    );
    
    // Lấy tháng hiện tại
    @Query("SELECT m FROM MonthlyCreatorBalance m WHERE m.creator.creatorId = :creatorId " +
           "AND m.year = :year AND m.month = :month")
    Optional<MonthlyCreatorBalance> findCurrentMonth(
        @Param("creatorId") Long creatorId,
        @Param("year") int year,
        @Param("month") int month
    );
}