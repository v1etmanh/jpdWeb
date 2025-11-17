package com.jpd.web.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopCreatorData {
    private Long creatorId;
    private String creatorName;
    private Double totalRevenue;
}
