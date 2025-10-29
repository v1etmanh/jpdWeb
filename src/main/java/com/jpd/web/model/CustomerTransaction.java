package com.jpd.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
<<<<<<< HEAD
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_transaction", indexes = {
        @Index(name = "idx_transaction_status", columnList = "status"),
        @Index(name = "idx_transaction_created_at", columnList = "created_at"),
        @Index(name = "idx_transaction_enrollment", columnList = "enroll_id")
})
=======

import java.sql.Date;
import java.time.LocalDateTime;

@Entity

>>>>>>> jpdWeb6/master
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class CustomerTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private long transactionID;

<<<<<<< HEAD
    // link to Enrollment
=======
    //link to Enrollment
>>>>>>> jpdWeb6/master
    @OneToOne
    @JoinColumn(name = "enroll_id")
    private Enrollment enrollment;

<<<<<<< HEAD
    @Column(name = "content", length = 255)
    private String content;

    @Column(name = "status", length = 20)
    private String status; // SUCCESS, FAILED, PENDING, CANCELLED

    @Column(name = "amount")
    private Double amount;

    @Column(name = "currency", length = 10)
    private String currency;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
=======


    @Column(name = "content", length = 255)
    private String content;
    private String status; // CREATED, PENDING, COMPLETED, FAILED
    private Double amount;
    private String currency;

    private Date createdAt;
    private Date updatedAt;
>>>>>>> jpdWeb6/master

    @Column(name = "admin_get", nullable = false)
    private double adminGet;

    @Column(name = "creator_get", nullable = false)
    private double creatorGet;
<<<<<<< HEAD

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;
=======
    private String paymentId;
>>>>>>> jpdWeb6/master
}
