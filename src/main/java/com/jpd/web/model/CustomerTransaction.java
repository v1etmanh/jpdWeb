package com.jpd.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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
    private long id;

    //link to Enrollment
    @OneToOne
    @JoinColumn(name = "enroll_id")
    private Enrollment enrollment;

    @Column(nullable = false)
    private double total; // số tiền (âm cho REFUND)

    @Column(name = "content", length = 255)
    private String content;

    @CreationTimestamp
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "admin_get", nullable = false)
    private double adminGet;

    @Column(name = "creator_get", nullable = false)
    private double creatorGet;
}
