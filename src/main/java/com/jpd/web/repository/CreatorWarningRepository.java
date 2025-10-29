package com.jpd.web.repository;

import com.jpd.web.model.Creator;
import com.jpd.web.model.CreatorWarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CreatorWarningRepository extends JpaRepository<CreatorWarning, Long> {

    List<CreatorWarning> findByCreatorAndIsActiveTrue(Creator creator);

    @Query("SELECT COUNT(cw) FROM CreatorWarning cw " +
            "WHERE cw.creator = :creator AND cw.isActive = true " +
            "AND cw.issuedAt >= :since")
    Long countByCreatorAndIssuedAtAfter(
            @Param("creator") Creator creator,
            @Param("since") LocalDateTime since);

    List<CreatorWarning> findByCreatorOrderByIssuedAtDesc(Creator creator);
}
