package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public  class RejectedContentInfo {
    private Long contentId;
    private String content;
    private String reason;
}