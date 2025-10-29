package com.jpd.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long auditLogId;

    @Column(name = "action_type", nullable = false)
    private String actionType; // APPROVE_CERT, REJECT_CERT, BAN_CREATOR, WARN_CREATOR, UNBAN_CREATOR

    @Column(name = "target_creator_id", nullable = false)
    private Long targetCreatorId;

    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
