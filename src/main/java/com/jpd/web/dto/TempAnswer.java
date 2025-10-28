package com.jpd.web.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.jpd.web.model.SessionInfo;
import com.jpd.web.model.SessionStatus;
import com.jpd.web.model.TypeOfContent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TempAnswer {
    private String participantId;
    private String participantName;
    private Long questionId;
    private String answer; // Đáp án của participant
    private LocalDateTime submittedAt;
}