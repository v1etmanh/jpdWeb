package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SemanticResult {
    @JsonProperty("match")
    private boolean match;
    
    @JsonProperty("similarity_score")
    private double similarityScore;
    
    @JsonProperty("user_answer")
    private String userAnswer;
    
    @JsonProperty("expected_answer")
    private String expectedAnswer;
    
    @JsonProperty("feedback")
    private String feedback;
    
    @JsonProperty("has_error")
    private boolean hasError;
    
    @JsonProperty("error_message")
    private String errorMessage;
}
