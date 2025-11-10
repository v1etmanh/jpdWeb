package com.jpd.web.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public  class ModerationBatchResult {
    private List<StandardizedContentDto> approvedContents;
    private List<RejectedContentInfo> rejectedContents;
}