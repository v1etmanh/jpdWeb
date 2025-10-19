package com.jpd.web.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GeminiAiService {
    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final WebClient webClient = WebClient.builder().build();

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

    public String generateContentWithImage(String prompt, String imageBase64, String mimeType) {
        // Lưu ý: imageBase64 KHÔNG kèm "data:image/png;base64,"
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> imagePart = Map.of(
                "inlineData", Map.of("mimeType", mimeType, "data", imageBase64)
        );

        Map<String, Object> requestBody = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", "Hãy trả lời ngắn gọn và súc tích. Tối đa ~400 từ."))
                ),
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(textPart, imagePart))
                ),
                "generationConfig", genCfg(4096) // tăng token trả về
        );

        return callWithRetry(requestBody, prompt, /*hasImage=*/true);
    }

    // ======= Core call + retry =======

    private String callWithRetry(Map<String, Object> requestBody, String originalPrompt, boolean hasImage) {
        Map<String, Object> body = postJson(requestBody);
        Parsed parsed = parse(body);

        if (parsed.text != null && !parsed.text.isBlank()) {
            // Có text -> trả luôn (kể cả finishReason khác STOP)
            return parsed.text;
        }

        // Nếu bị MAX_TOKENS hoặc không có text -> Retry 1 lần:
        if ("MAX_TOKENS".equalsIgnoreCase(parsed.finishReason) || parsed.text == null) {
            Map<String, Object> retryBody = Map.of(
                    "systemInstruction", Map.of(
                            "parts", List.of(Map.of(
                                    "text", "Hãy tóm tắt ngắn gọn, đi thẳng vào ý chính, tối đa ~250 từ."
                            ))
                    ),
                    "contents", List.of(
                            Map.of("role", "user", "parts", hasImage
                                    ? List.of(Map.of("text", originalPrompt + "\n\n(Tóm tắt ngắn gọn)"))
                                    : List.of(Map.of("text", originalPrompt + "\n\n(Tóm tắt ngắn gọn)"))
                            )
                    ),
                    "generationConfig", genCfg(8192) // đẩy trần token cao hơn cho chắc
            );

            Map<String, Object> retryResp = postJson(retryBody);
            Parsed retryParsed = parse(retryResp);
            if (retryParsed.text != null && !retryParsed.text.isBlank()) {
                return retryParsed.text;
            }

            // Thông báo lý do hữu ích
            return msgFromParsed(retryParsed, "No text after retry");
        }

        return msgFromParsed(parsed, "No text");
    }

    // ======= Low-level HTTP =======

    private Map<String, Object> postJson(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, new HttpEntity<>(requestBody, headers), Map.class
        );
        return response.getBody();
    }

    private Map<String, Object> genCfg(int maxOutputTokens) {
        return Map.of(
                "temperature", 0.2,
                "maxOutputTokens", maxOutputTokens,
                // giúp model “ưu tiên text thuần”
                "responseMimeType", "text/plain"
        );
    }

    // ======= Parsing & helpers =======

    private static class Parsed {
        final String text;
        final String finishReason;
        final String blockReason;
        Parsed(String text, String finishReason, String blockReason) {
            this.text = text; this.finishReason = finishReason; this.blockReason = blockReason;
        }
    }

    @SuppressWarnings("unchecked")
    private static Parsed parse(Map<String, Object> body) {
        if (body == null) return new Parsed(null, null, null);

        // promptFeedback.blockReason (nếu bị chặn)
        String blockReason = null;
        Map<String, Object> promptFeedback = (Map<String, Object>) body.get("promptFeedback");
        if (promptFeedback != null && promptFeedback.get("blockReason") != null) {
            blockReason = String.valueOf(promptFeedback.get("blockReason"));
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return new Parsed(null, null, blockReason);
        }

        Map<String, Object> first = candidates.get(0);
        String finishReason = first.get("finishReason") != null ? String.valueOf(first.get("finishReason")) : null;

        // Cố gắng trích text dù finishReason != STOP
        String text = tryExtractText(first);

        return new Parsed(text, finishReason, blockReason);
    }

    @SuppressWarnings("unchecked")
    private static String tryExtractText(Map<String, Object> candidate) {
        if (candidate == null) return null;
        Map<String, Object> content = (Map<String, Object>) candidate.get("content");
        if (content == null) return null;
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) return null;
        for (Map<String, Object> p : parts) {
            Object t = p.get("text");
            if (t instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }

    private static String msgFromParsed(Parsed p, String fallback) {
        if (p.blockReason != null) {
            return "Request was blocked: " + p.blockReason;
        }
        if (p.finishReason != null) {
            return fallback + ". finishReason=" + p.finishReason;
        }
        return fallback;
    }

}
