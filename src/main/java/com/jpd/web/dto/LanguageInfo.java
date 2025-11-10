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
public class LanguageInfo {
    
    @JsonProperty("detected_code")
    private String detectedCode;
    
    @JsonProperty("detected_name")
    private String detectedName;
    
    @JsonProperty("is_allowed")
    private boolean isAllowed;
    
    @JsonProperty("allowed_languages")
    private java.util.List<String> allowedLanguages;
    
    private String message;
}