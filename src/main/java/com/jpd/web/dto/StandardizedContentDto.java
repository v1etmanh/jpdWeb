package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandardizedContentDto {
    private String lang;      // Ngôn ngữ (nếu có)
    private String content;   // Nội dung sau khi chuẩn hóa (removed special chars, HTML...)
}
