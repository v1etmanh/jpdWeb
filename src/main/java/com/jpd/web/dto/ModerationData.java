package com.jpd.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ModerationData {
    
    @JsonProperty("content_id")
    private String content_id;
    
    private String status; // "APPROVED" or "REJECTED"
    
    @JsonProperty("rejection_reason")
    private String rejection_reason;
    
    // ✅ FIXED: Should be LanguageInfo object, not Language enum
    private LanguageInfo language;
}