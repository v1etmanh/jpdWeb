package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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
    private String payoutItemId;
  
    private double amount;
    private String currency;
    private String content;
    private String status; // PENDING, SUCCESS, FAILED, CANCELLED
    private Date createdAt;
    private Date updatedAt;


    //link to Creator
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private Creator creator;


}
