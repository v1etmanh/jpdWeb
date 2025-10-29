package com.jpd.web.dto;

import com.google.firebase.database.annotations.NotNull;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.ReportType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class StartQuestionResponse {
    private Long questionId;
    private int questionNumber; // Câu hỏi thứ mấy (1, 2, 3...)
    private int totalQuestions;
    private ModuleContent question; // Chi tiết câu hỏi
    private Integer timeLimit; // Giây
    private LocalDateTime serverTime; // Để client sync time
}