package com.jpd.web.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFilterDto {

    private String status; // SUCCESS, FAILED, PENDING, CANCELLED

    private Long customerId;

    private Long courseId;

    private Long creatorId;

    @PastOrPresent(message = "Start date must be in the past or present")
    private LocalDateTime startDate;

    @PastOrPresent(message = "End date must be in the past or present")
    private LocalDateTime endDate;

    @DecimalMin(value = "0.0", message = "Minimum amount must be greater than or equal to 0")
    private Double minAmount;

    @DecimalMax(value = "1000000.0", message = "Maximum amount must be less than 1,000,000")
    private Double maxAmount;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    @Builder.Default
    private Integer size = 20;

    @Min(value = 0, message = "Page number must be non-negative")
    @Builder.Default
    private Integer page = 0;

    @Pattern(regexp = "createdAt|amount|status|updatedAt", message = "Invalid sort field. Allowed: createdAt, amount, status, updatedAt")
    @Builder.Default
    private String sortBy = "createdAt";

    @Pattern(regexp = "ASC|DESC", message = "Invalid sort direction. Allowed: ASC, DESC")
    @Builder.Default
    private String sortDirection = "DESC";

    @AssertTrue(message = "Start date must be before or equal to end date")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }

    @AssertTrue(message = "Minimum amount must be less than or equal to maximum amount")
    public boolean isValidAmountRange() {
        if (minAmount == null || maxAmount == null) {
            return true;
        }
        return minAmount <= maxAmount;
    }
}
