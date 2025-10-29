package com.jpd.web.repository;

import com.jpd.web.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT r FROM Report r JOIN r.course c WHERE c.creator.creatorId = :creatorId")
    List<Report> findByCreator_CreatorId(@Param("creatorId") Long creatorId);

    List<Report> findByStatus(String status);

    @Query("SELECT COUNT(r) FROM Report r JOIN r.course c WHERE c.creator.creatorId = :creatorId AND r.createdAt >= :since")
    Long countByCreator_CreatorIdAndCreatedAtAfter(
            @Param("creatorId") Long creatorId,
            @Param("since") LocalDateTime since);
}
