package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class StandardizedAnswerDto {
    private String answerText; // nội dung đáp án
    private boolean correct;   // đánh dấu đúng/sai


}

