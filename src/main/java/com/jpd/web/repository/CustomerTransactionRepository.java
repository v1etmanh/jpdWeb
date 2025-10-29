package com.jpd.web.repository;

import com.jpd.web.model.CustomerTransaction;
<<<<<<< HEAD
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

@Repository
public interface CustomerTransactionRepository
                extends JpaRepository<CustomerTransaction, Long>,
                JpaSpecificationExecutor<CustomerTransaction> {

        // Simple queries
        List<CustomerTransaction> findByStatusOrderByCreatedAtDesc(String status);

        // Revenue statistics
        @Query("SELECT SUM(ct.amount) FROM CustomerTransaction ct " +
                        "WHERE ct.status = 'SUCCESS' AND ct.createdAt BETWEEN :start AND :end")
        Double getTotalRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("SELECT SUM(ct.adminGet) FROM CustomerTransaction ct " +
                        "WHERE ct.status = 'SUCCESS' AND ct.createdAt BETWEEN :start AND :end")
        Double getAdminRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("SELECT SUM(ct.creatorGet) FROM CustomerTransaction ct " +
                        "WHERE ct.status = 'SUCCESS' AND ct.createdAt BETWEEN :start AND :end")
        Double getCreatorRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("SELECT COUNT(ct) FROM CustomerTransaction ct " +
                        "WHERE ct.status = :status AND ct.createdAt BETWEEN :start AND :end")
        Long countByStatusAndDateRange(@Param("status") String status,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // Top courses with Projection (type-safe, no N+1)
        @Query("SELECT c.courseId as courseId, c.name as courseName, c.urlImg as imageUrl, " +
                        "SUM(ct.amount) as totalRevenue, COUNT(ct) as enrollmentCount " +
                        "FROM CustomerTransaction ct " +
                        "JOIN ct.enrollment e " +
                        "JOIN e.course c " +
                        "WHERE ct.status = 'SUCCESS' AND ct.createdAt BETWEEN :start AND :end " +
                        "GROUP BY c.courseId, c.name, c.urlImg " +
                        "ORDER BY totalRevenue DESC")
        Page<CourseRevenueProjection> getTopCoursesByRevenue(@Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        Pageable pageable);

        // Top creators with Projection
        @Query("SELECT cr.creatorId as creatorId, cr.fullName as creatorName, " +
                        "SUM(ct.creatorGet) as totalRevenue, COUNT(DISTINCT e.course.courseId) as courseCount " +
                        "FROM CustomerTransaction ct " +
                        "JOIN ct.enrollment e " +
                        "JOIN e.course c " +
                        "JOIN c.creator cr " +
                        "WHERE ct.status = 'SUCCESS' AND ct.createdAt BETWEEN :start AND :end " +
                        "GROUP BY cr.creatorId, cr.fullName " +
                        "ORDER BY totalRevenue DESC")
        Page<CreatorRevenueProjection> getTopCreatorsByRevenue(@Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        Pageable pageable);
=======
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Repository
public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, Long> {

    // findTransactionsByEnrollmentId(bigint)
    List<CustomerTransaction> findByEnrollment_EnrollId(Long enrollId);

    // findTransactionsByCustomerId(bigint)
    List<CustomerTransaction> findByEnrollment_Customer_CustomerId(Long customerId);

    @Query("""
    SELECT SUM(t.amount) 
    FROM CustomerTransaction t 
    JOIN t.enrollment e 
    WHERE e.course.courseId = :courseId 
      AND t.status = 'SUCCESS'
""")
    Double getTotalRevenueByCourseId(@Param("courseId") Long courseId);



    // updateTransactionStatus(bigint, varchar)
    @Modifying
    @Transactional
    @Query("UPDATE CustomerTransaction t SET t.status = :status WHERE t.transactionID = :transactionId")
    int updateTransactionStatus(Long transactionId, String status);


>>>>>>> jpdWeb6/master
}
