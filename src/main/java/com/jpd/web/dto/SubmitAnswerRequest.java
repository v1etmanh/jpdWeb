package com.jpd.web.dto;

import java.util.List;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Language;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Builder
public class SubmitAnswerRequest {
    private String sessionCode;
    private String participantId;
    private Long questionId;
    private String answer;
}