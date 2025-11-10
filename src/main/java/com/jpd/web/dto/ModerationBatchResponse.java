package com.jpd.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModerationBatchResponse {
    private boolean success;
    private int total;
    private int processed;
    private int failed;
    private List<ModerationResult> results;
    
    // Helper methods
    public int getApprovedCount() {
        return (int) results.stream()
            .filter(ModerationResult::isApproved)
            .count();
    }
    
    public int getRejectedCount() {
        return (int) results.stream()
            .filter(ModerationResult::isRejected)
            .count();
    }
}
