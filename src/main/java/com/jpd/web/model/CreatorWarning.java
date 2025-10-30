package com.jpd.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;

@Entity
@Table(name = "creator_warning")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorWarning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warning_id")
    private Long warningId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonBackReference("creator-warning")
    private Creator creator;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "issued_by_admin", nullable = false)
    private String issuedByAdmin;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
