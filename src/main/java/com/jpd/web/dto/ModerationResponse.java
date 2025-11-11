package com.jpd.web.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;
@Data
public class ModerationResponse {
    private String decision; // "ALLOWED" | "BLOCKED"
    private String reason;   // lý do (nếu BLOCKED)

    // Dưới đây là trường chẩn đoán (nếu bạn giữ trong Lambda)
    private List<Map<String, Object>> labels;
    private List<Map<String, Object>> objects;
    private String ocr_text_sample;

    // getters/setters
}