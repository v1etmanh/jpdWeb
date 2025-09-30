package com.jpd.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Date;
import java.time.LocalDateTime;

@Entity

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class CustomerTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private long transactionID;

    //link to Enrollment
    @OneToOne
    @JoinColumn(name = "enroll_id")
    private Enrollment enrollment;

   

    @Column(name = "content", length = 255)
    private String content;
    private String status; // CREATED, PENDING, COMPLETED, FAILED
    private Double amount;
    private String currency;
   
    private Date createdAt;
    private Date updatedAt;

    @Column(name = "admin_get", nullable = false)
    private double adminGet;

    @Column(name = "creator_get", nullable = false)
    private double creatorGet;
}
