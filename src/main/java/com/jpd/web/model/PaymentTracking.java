package com.jpd.web.model;

import java.time.LocalDateTime;



import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "payment_tracking")
@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Builder
public class PaymentTracking {
    @Id
    private String paymentId;
    private String status; // CREATED, PENDING, COMPLETED, FAILED
    private Double amount;
    private String currency;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long customerId;
    private long courseId;
    // constructors, getters, setters
}