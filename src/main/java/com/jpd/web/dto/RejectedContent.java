package com.jpd.web.dto;

import com.jpd.web.model.TypeOfContent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public  class RejectedContent {
    private Long mcId;
    private String content;
    private String reason; // Từ moderation API
    private TypeOfContent type;
}
