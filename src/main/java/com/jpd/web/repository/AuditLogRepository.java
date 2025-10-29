package com.jpd.web.repository;

import com.jpd.web.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTargetCreatorIdOrderByTimestampDesc(Long creatorId);

    List<AuditLog> findByAdminEmailOrderByTimestampDesc(String adminEmail);

    @Query("SELECT al FROM AuditLog al WHERE al.targetCreatorId = :creatorId " +
            "AND al.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY al.timestamp DESC")
    List<AuditLog> findByCreatorAndDateRange(
            @Param("creatorId") Long creatorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
