package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdraw", indexes = @Index(columnList = "creator_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Withdraw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdraw_id")
    private long withdrawId;
    @Column(name = "payout_batch_id")
    private String payoutBatchId;
  
    private double amount;
    private String currency;
    private String content;
    @Enumerated(EnumType.STRING)
    private Status status; // PENDING, SUCCESS, FAILED, CANCELLED
    @CreationTimestamp
    private LocalDateTime createdAt;
    private Date updatedAt;


    //link to Creator
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    @JsonBackReference("creator-withdraw")
    private Creator creator;


}
