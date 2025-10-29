package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionRequest {
    private Long creatorId;
    private String actionType; // WARN, BAN, UNBAN, APPROVE_CERT, REJECT_CERT
    private String reason;
    private Integer durationDays; // Optional, only for BAN
}
