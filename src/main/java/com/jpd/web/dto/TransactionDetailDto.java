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
public class TransactionDetailDto {
    // Basic transaction info
    private Long transactionId;
    private Double amount;
    private String currency;
    private String status;
    private String content;
    private PaymentMethod paymentMethod;
    private String paymentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Revenue split
    private Double adminGet;
    private Double creatorGet;
    private Double adminPercentage;
    private Double creatorPercentage;

    // Customer info
    private CustomerInfo customerInfo;

    // Course info
    private CourseInfo courseInfo;

    // Creator info
    private CreatorInfo creatorInfo;

    // Enrollment info
    private Long enrollmentId;
    private LocalDateTime enrollmentDate;
    private Boolean isCompleted;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private Long customerId;
        private String name;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseInfo {
        private Long courseId;
        private String name;
        private String imageUrl;
        private Double price;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatorInfo {
        private Long creatorId;
        private String name;
        private String email;
        private Boolean isFrozen;
    }
}


