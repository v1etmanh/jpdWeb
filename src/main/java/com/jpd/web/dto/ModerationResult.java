package com.jpd.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModerationResult {
    private int index;
    private boolean success;
    private ModerationData data;
    private String error;
    
    // Helper method
    public boolean isApproved() {
        return success 
            && data != null 
            && "APPROVED".equals(data.getStatus());
    }
    
    public boolean isRejected() {
        return success 
            && data != null 
            && "REJECTED".equals(data.getStatus());
    }
}