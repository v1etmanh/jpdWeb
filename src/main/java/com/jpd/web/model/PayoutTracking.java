package com.jpd.web.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
@Entity
@Table(name = "payout_tracking")
@AllArgsConstructor
@Data
public class PayoutTracking {
	@Id
    private String payoutBatchId; 
  
    private String recipientEmail;
    private Double amount;
    private String currency;
    private String note;
    private String status; // PENDING, SUCCESS, FAILED, CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private TargetPayout targetPayout;
    @ManyToOne
    @JoinColumn(name="creator_id")
    @JsonBackReference("creator-payout")
    private Creator creator;
    // constructors, getters, setters
 public PayoutTracking() {}
    
    public PayoutTracking(String payoutBatchId, String recipientEmail, 
                         Double amount, String currency, String note) {
        this.payoutBatchId = payoutBatchId;
        this.recipientEmail = recipientEmail;
        this.amount = amount;
        this.currency = currency;
        this.note = note;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }
    
    // getters and setters
   
    
  }