package com.jpd.web.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinSessionRequest {
    private String sessionCode;
    private String participantName;
}