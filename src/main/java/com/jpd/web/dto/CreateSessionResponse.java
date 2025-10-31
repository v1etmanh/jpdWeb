package com.jpd.web.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSessionResponse {
    private String sessionCode;
    private String sessionId;
    private String qrCodeUrl;
    private String joinUrl;
    private String title;
    private Integer totalQuestions;
}