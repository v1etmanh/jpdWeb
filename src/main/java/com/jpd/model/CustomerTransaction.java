package com.jpd.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_transaction",
        indexes = @Index(name = "idx_tx_enroll", columnList = "enroll_id"))
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class CustomerTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    //link to Enrollment
    @OneToOne
    @JoinColumn(name = "enroll_id")
    private Enrollment enrollment;

    @Column(precision = 12, scale = 2, nullable = false)
    private Double total; // số tiền (âm cho REFUND)

    @Column(name = "content", length = 255)
    private String content;

    @CreationTimestamp
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "admin_get", precision = 12, scale = 2, nullable = false)
    private Double adminGet;

    @Column(name = "creator_get", precision = 12, scale = 2, nullable = false)
    private Double creatorGet;
}
