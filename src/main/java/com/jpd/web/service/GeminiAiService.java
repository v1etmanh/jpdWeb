package com.jpd.web.service;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiAiService {
    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateContent(String prompt) {
        // Request body JSON
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                Map.class
        );

        // Trả về phần text từ response
        Map<String, Object> body = response.getBody();
        if (body != null && body.containsKey("candidates")) {
            var candidates = (java.util.List<Map<String, Object>>) body.get("candidates");
            if (!candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                var parts = (java.util.List<Map<String, Object>>) content.get("parts");
                return (String) parts.get(0).get("text");
            }
        }
        return "No response from Gemini";
    }
    public String generateContentWithImage(String prompt, String base64Image, String mimeType) {
        // Thêm hướng dẫn cụ thể hơn
        String enhancedPrompt = prompt + "\n\nIMPORTANT: Your response must start with [ and end with ] only. Nothing else.";
        
        Map<String, Object> requestBody = Map.of(
            "contents", new Object[]{
                Map.of("parts", new Object[]{
                    Map.of("text", enhancedPrompt),
                    Map.of("inline_data", Map.of(
                        "mime_type", mimeType,
                        "data", base64Image
                    ))
                })
            },
            "generationConfig", Map.of(
                "temperature", 0.1,  // Minimize randomness
                "topK", 1,           // Only pick highest probability
                "topP", 0.1,
                "maxOutputTokens", 500
            )
        );
        
        return sendRequest(requestBody);
    }
     private String sendRequest(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        if (body != null && body.containsKey("candidates")) {
            var candidates = (java.util.List<Map<String, Object>>) body.get("candidates");
            if (!candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                var parts = (java.util.List<Map<String, Object>>) content.get("parts");
                return (String) parts.get(0).get("text");
            }
        }
        return "No response from Gemini";
    }
     
}
