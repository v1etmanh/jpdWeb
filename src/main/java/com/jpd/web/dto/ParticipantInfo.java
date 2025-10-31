package com.jpd.web.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantInfo {
    private String participantId;
    private String name;
    private String sessionCode;
    private LocalDateTime joinedAt;
    private Integer currentScore;
}

