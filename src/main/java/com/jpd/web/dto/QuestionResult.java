package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResult {
    private String participantId;
    private String participantName;
    private String answer;
    private boolean correct;
    private int points;
    private int totalScore;
}