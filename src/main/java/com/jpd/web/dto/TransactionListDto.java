package com.jpd.web.dto;

import com.jpd.web.model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionListDto {
    private Long transactionId;
    private Double amount;
    private String currency;
    private String status;

    // Customer info
    private String customerName;
    private String customerEmail;
    private Long customerId;

    // Course info
    private String courseName;
    private Long courseId;

    // Creator info
    private String creatorName;
    private Long creatorId;

    // Revenue split
    private Double adminGet;
    private Double creatorGet;

    // Payment info
    private PaymentMethod paymentMethod;
    private String paymentId;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


