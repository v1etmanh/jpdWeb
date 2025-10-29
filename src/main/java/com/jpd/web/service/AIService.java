package com.jpd.web.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.dto.WritingScores;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AIService {
    
    @Autowired
    private GeminiAiService geminiAiService;
    
    @Autowired
    private FireBaseService fireBaseService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public String generateFeedback(String question, String answer) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a multilingual language tutor.\n");
        sb.append("Your task is to analyze the learner's answer in ANY language.\n");
        sb.append("Feedback must be written in clear and natural Vietnamese.\n");
        sb.append("Feedback length: 1–3 sentences, no lists, no JSON, no extra formatting.\n\n");
        sb.append("The feedback must include:\n");
        sb.append("1. Confirmation: nói câu trả lời đúng hay sai.\n");
        sb.append("2. Explanation: giải thích ngắn gọn về ngữ pháp hoặc từ vựng.\n");
        sb.append("Câu hỏi: \"").append(escapeForPrompt(question)).append("\"\n");
        sb.append("Câu trả lời: \"").append(escapeForPrompt(answer)).append("\"\n");
        
        return geminiAiService.generateContent(sb.toString());
    }
    
    private String escapeForPrompt(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");
    }
    
    /**
     * Làm sạch JSON response từ AI (loại bỏ markdown code blocks)
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.isEmpty()) {
            return "{}";
        }
        
        String cleaned = response.trim()
            .replaceAll("```json\\s*", "")
            .replaceAll("```\\s*", "")
            .trim();
        
        // Extract JSON object nếu có text xung quanh
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        
        return cleaned;
    }
    
    public Map<String, Object> evaluateWriting(String writingText, String language) {
        String prompt = buildEvaluationPrompt(writingText, language);
        String aiResponse = geminiAiService.generateContent(prompt);
        
        log.info("Raw AI Response: {}", aiResponse);
        
        try {
            String cleanJson = cleanJsonResponse(aiResponse);
            log.info("Cleaned JSON: {}", cleanJson);
            
            Map<String, Object> result = objectMapper.readValue(cleanJson, Map.class);
            
            // Normalize field names để match frontend
            return Map.of(
                "grammar", getScore(result, "grammar", "grammar_score"),
                "vocabulary", getScore(result, "vocabulary", "vocabulary_score"),
                "feedback", result.getOrDefault("feedback", "No feedback provided")
            );
        } catch (Exception e) {
            log.error("Error parsing AI response: {}", aiResponse, e);
            return Map.of(
                "grammar", 0,
                "vocabulary", 0,
                "feedback", "Error evaluating writing. Please try again."
            );
        }
    }
    
    private String buildEvaluationPrompt(String writingText, String language) {
        return String.format("""
            You are an expert %s language teacher. Evaluate this writing:
            
            '''
            %s
            '''
            
            Return ONLY valid JSON (no markdown, no extra text):
            {
                "grammar": <score 0-10>,
                "vocabulary": <score 0-10>,
                "feedback": "<detailed constructive feedback in Vietnamese>"
            }
            
            Be specific about grammar errors and vocabulary usage.
            """, language, writingText);
    }
    
    /**
     * Phiên bản đơn giản - chỉ trả về điểm số
     */
    public WritingScores evaluateWritingSimple(String writingText, String language) {
        try {
            Map<String, Object> result = evaluateWriting(writingText, language);
            
            return new WritingScores(
                ((Number) result.get("grammar")).doubleValue(),
                ((Number) result.get("vocabulary")).doubleValue(),
                (String) result.get("feedback")
            );
        } catch (Exception e) {
            log.error("Error evaluating writing", e);
            return new WritingScores(
                5.0, 
                5.0, 
                "Could not evaluate. Please try again."
            );
        }
    }
    
    private double getScore(Map<String, Object> result, String... keys) {
        for (String key : keys) {
            Object value = result.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }
        return 0.0;
    }
}