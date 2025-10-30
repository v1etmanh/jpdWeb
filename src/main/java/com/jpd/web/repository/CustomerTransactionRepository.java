package com.jpd.web.repository;

import com.jpd.web.model.CustomerTransaction;
import com.jpd.web.repository.projection.CourseRevenueProjection;
import com.jpd.web.repository.projection.CreatorRevenueProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerTransactionRepository
        extends JpaRepository<CustomerTransaction, Long>,
        JpaSpecificationExecutor<CustomerTransaction> {
    
    // ===== OVERLOADED METHODS - Default to SUCCESS status =====
    
    // Revenue statistics - SUCCESS only (default)
    @Query("SELECT COALESCE(SUM(ct.amount), 0.0) FROM CustomerTransaction ct " +
           "WHERE ct.status = 'SUCCESS' AND ct.createdAt BETWEEN :start AND :end")
    Double getTotalRevenue(
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end
    );
    
    @Query("SELECT COALESCE(SUM(ct.adminGet), 0.0) FROM CustomerTransaction ct " +
           "WHERE ct.status = 'SUCCESS' AND ct.createdAt BETWEEN :start AND :end")
    Double getAdminRevenue(
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end
    );
    
    @Query("SELECT COALESCE(SUM(ct.creatorGet), 0.0) FROM CustomerTransaction ct " +
           "WHERE ct.status = 'SUCCESS' AND ct.createdAt BETWEEN :start AND :end")
    Double getCreatorRevenue(
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end
    );
    
    // Count by status
    @Query("SELECT COUNT(ct) FROM CustomerTransaction ct " +
           "WHERE ct.status = :status AND ct.createdAt BETWEEN :start AND :end")
    Long countByStatusAndDateRange(
            @Param("status") String status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    
    // ===== FLEXIBLE METHODS - With status parameter =====
    
    @Query("SELECT COALESCE(SUM(ct.amount), 0.0) FROM CustomerTransaction ct " +
           "WHERE ct.status = :status AND ct.createdAt BETWEEN :start AND :end")
    Double getTotalRevenueByStatus(
            @Param("status") String status,
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end
    );
    
    @Query("SELECT COALESCE(SUM(ct.adminGet), 0.0) FROM CustomerTransaction ct " +
           "WHERE ct.status = :status AND ct.createdAt BETWEEN :start AND :end")
    Double getAdminRevenueByStatus(
            @Param("status") String status,
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end
    );
    
    @Query("SELECT COALESCE(SUM(ct.creatorGet), 0.0) FROM CustomerTransaction ct " +
           "WHERE ct.status = :status AND ct.createdAt BETWEEN :start AND :end")
    Double getCreatorRevenueByStatus(
            @Param("status") String status,
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end
    );
    
    // ===== TOP COURSES & CREATORS =====
    
    @Query("SELECT c.courseId as courseId, " +
           "c.name as courseName, " +
           "c.urlImg as imageUrl, " +
           "SUM(ct.amount) as totalRevenue, " +
           "COUNT(ct) as enrollmentCount " +
           "FROM CustomerTransaction ct " +
           "JOIN ct.enrollment e " +
           "JOIN e.course c " +
           "WHERE ct.status = 'SUCCESS' " +
           "AND ct.createdAt BETWEEN :start AND :end " +
           "GROUP BY c.courseId, c.name, c.urlImg " +
           "ORDER BY totalRevenue DESC")
    Page<CourseRevenueProjection> getTopCoursesByRevenue(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );
    
    @Query("SELECT cr.creatorId as creatorId, " +
           "cr.fullName as creatorName, " +
           "SUM(ct.creatorGet) as totalRevenue, " +
           "COUNT(DISTINCT c.courseId) as courseCount " +
           "FROM CustomerTransaction ct " +
           "JOIN ct.enrollment e " +
           "JOIN e.course c " +
           "JOIN c.creator cr " +
           "WHERE ct.status = 'SUCCESS' " +
           "AND ct.createdAt BETWEEN :start AND :end " +
           "GROUP BY cr.creatorId, cr.fullName " +
           "ORDER BY totalRevenue DESC")
    Page<CreatorRevenueProjection> getTopCreatorsByRevenue(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );
    
    // ===== UTILITY QUERIES =====
    
    @Query("SELECT ct FROM CustomerTransaction ct " +
           "WHERE ct.createdAt BETWEEN :start AND :end " +
           "ORDER BY ct.createdAt DESC")
    List<CustomerTransaction> findAllInDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    
    Optional<CustomerTransaction> findByPaymentId(String paymentId);
    
    List<CustomerTransaction> findByStatusOrderByCreatedAtDesc(String status);
    
    @Query("SELECT ct FROM CustomerTransaction ct " +
           "JOIN FETCH ct.enrollment e " +
           "JOIN FETCH e.course " +
           "WHERE ct.transactionID = :id")
    Optional<CustomerTransaction> findByIdWithDetails(@Param("id") Long id);
}