package com.jpd.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.NotFoundException;
import com.jpd.web.dto.Request.AiWritingRequest;
import com.jpd.web.dto.Response.AiWritingResponse;
import com.jpd.web.exception.AIResponseParsingException;
import com.jpd.web.model.WritingQuestion;
import com.jpd.web.repository.WritingQuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.jpd.web.repository.CustomerRepository;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiWritingService {
    @Autowired
    private GeminiAiService geminiAiService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private WritingQuestionRepository writingQuestionRepository;

    // WebClient dùng để tải ảnh
    private final WebClient http = WebClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, "ImageFetcher/1.0")
            .build();


    public String generateFeedback(String question, String answer) throws IllegalAccessException {

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
        return this.geminiAiService.generateContent(sb.toString());
    }
    private String escapeForPrompt1(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");
    }
    // ------------------------------------------------------------------------------------------------------------------
    //                       AI Writing
    private static String safe(String s) { return s == null ? "" : s.trim(); }
    public AiWritingResponse gradeEssay(long writingQuestionId, AiWritingRequest req){
        Optional<WritingQuestion> wqOpt = writingQuestionRepository.findById(writingQuestionId);
        WritingQuestion writingQuestion = wqOpt.orElse(null);
        log.info("writingQuestionId:{}", writingQuestionId);
        String requirement = (req.getRequirement()==null ||req.getRequirement().isBlank())
                ? "Không có" : req.getRequirement().trim();
        String taskType = (writingQuestion != null && writingQuestion.getTaskTypeCategory()!=null)
                ? writingQuestion.getTaskTypeCategory().name()
                : "GENERAL";
        String prompt = buildPrompt(
                safe(req.getLanguage()),
                safe(taskType),
                safe(req.getQuestion()),
                requirement,
                writingQuestion != null ? writingQuestion.getFeatures() : Collections.emptyList(),
                (writingQuestion.getImageUrl() != null) && !writingQuestion.getImageUrl().isBlank(),
                safe(req.getText()),
                writingQuestion.getCriterias(),
                writingQuestion.getImageUrl()
        );
        log.info("prompt:{}", prompt);
        String aiRaw;
        // Nếu có ảnh: tải → base64 → gửi inlineData
        if (writingQuestion.getImageUrl() != null && !writingQuestion.getImageUrl().isBlank()) {
            try {
                ImagePayload img = fetchImageAsBase64(writingQuestion.getImageUrl());
                aiRaw = geminiAiService.generateContentWithImage(prompt, img.base64, img.mime);
                log.info("aiRaw:{}", aiRaw);
            } catch (Exception e) {
                log.warn("Không gửi được ảnh cho Gemini, fallback về text-only. Lỗi: {}", e.getMessage());
                aiRaw = geminiAiService.generateContent(prompt); // fallback
            }
        } else {
            // Không có ảnh → text-only
            aiRaw = geminiAiService.generateContent(prompt);
            log.info("aiRaw:{}", aiRaw);
        }

        log.info("AI Grade Essay Response: {}", truncate(aiRaw, 2000));

        String json = extractFirstJsonObject(aiRaw);
        log.info("AI Grade Essay JSON: {}", json);

        try {
            return objectMapper.readValue(json, AiWritingResponse.class);
        } catch (Exception e) {
            throw new AIResponseParsingException("Gemini returned invalid JSON", aiRaw, e);
        }

    }
    private ImagePayload fetchImageAsBase64(String imageUrl) {
        ResponseEntity<byte[]> entity = http.get()
                .uri(imageUrl)
                .retrieve()
                .toEntity(byte[].class)
                .block(Duration.ofSeconds(20));

        if (entity == null || entity.getBody() == null || entity.getBody().length == 0) {
            throw new RuntimeException("Không tải được ảnh từ URL: " + imageUrl);
        }

        byte[] bytes = entity.getBody();
        String headerMime = Optional.ofNullable(entity.getHeaders().getContentType())
                .map(MediaType::toString)
                .orElse(null);
        String mime = guessMime(headerMime, bytes);
        String b64 = Base64.getEncoder().encodeToString(bytes);

        log.info("Ảnh tải OK. mime={}, base64Len={}", mime, b64.length());
        return new ImagePayload(b64, mime);
    }

    private static String guessMime(String headerMime, byte[] bytes) {
        if (headerMime != null && !headerMime.isBlank()) return headerMime;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) return "image/jpeg";
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return "image/png";
        if (bytes.length >= 6 && bytes[0]=='G' && bytes[1]=='I' && bytes[2]=='F') return "image/gif";
        return "application/octet-stream";
    }
    private record ImagePayload(String base64, String mime) {}

    /** Cắt log dài để tránh spam */
    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }


    /**
     * Trích JSON đầu tiên trong chuỗi trả về (nếu AI lỡ bao bọc bởi text/markdown).
     */

    private String extractFirstJsonObject(String text) {
        if (text == null || text.isBlank()) {
            throw new AIResponseParsingException("Empty response from Gemini", text, null);
        }
        int start = text.indexOf('{');
        if (start < 0) {
            throw new AIResponseParsingException("No JSON object found in Gemini response", text, null);
        }
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        throw new AIResponseParsingException("Unbalanced JSON braces in Gemini response", text, null);
    }

    public static   String buildPrompt(
            String language,
            String taskType,
            String question,
            String requirement,
            List<String> features,
            boolean hasImage,
            String essayText,
            List<String> criteria,
            String imageUrl
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("Bạn là một giám khảo chấm thi AI đa năng, có khả năng đánh giá nhiều loại bài viết (IELTS, JLPT, email, báo cáo, v.v.) bằng nhiều ngôn ngữ. Hãy phân tích bài luận dưới đây một cách toàn diện dựa trên tất cả thông tin được cung cấp.")
                .append("\n\n");

        sb.append("**1. Bối cảnh bài viết:**").append("\n");
        sb.append("* **Ngôn ngữ:** ").append(escapeForPrompt(language)).append("\n");
        sb.append("* **Loại bài viết (Task Type):** ").append(escapeForPrompt(taskType)).append("\n");
        sb.append("* **Đề bài (Question):**").append("\n");
        sb.append("    ---").append("\n");
        sb.append(escapeForPrompt(question)).append("\n");
        sb.append("    ---").append("\n");

        // Nếu requirement không rỗng thì thêm
        if (requirement != null && !requirement.isBlank()) {
            sb.append("\n");
            sb.append("* **Yêu cầu cụ thể (Requirements):**").append("\n");
            sb.append("    ---").append("\n");
            sb.append(escapeForPrompt(requirement)).append("\n");
            sb.append("    ---").append("\n");
        }

        // Nếu features có phần tử thì hiển thị
        if (features != null && !features.isEmpty()) {
            String featuresList = features.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(", "));
            if (!featuresList.isBlank()) {
                sb.append("\n");
                sb.append("* **Các yếu tố cần tập trung (Features):** ").append(featuresList).append("\n");
            }
        }

        sb.append("\n");
        sb.append("**2. Nội dung cần đánh giá:**").append("\n");

        if (hasImage) {
            sb.append("* **Phân tích hình ảnh đi kèm:** ").append("\n");
            sb.append(imageUrl);
        }

        sb.append("* **Bài luận:**").append("\n");
        sb.append("    ---").append("\n");
        sb.append(escapeForPrompt(essayText)).append("\n");
        sb.append("    ---").append("\n\n");

        sb.append("**3. Yêu cầu chấm điểm(criteria):**").append("\n\n");

        // Criteria list: nếu rỗng => để trống (AI sẽ tự chọn)
        if (criteria != null && !criteria.isEmpty()) {
            String criteriaList = criteria.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(", "));
            sb.append("Hãy chấm điểm bài viết dựa trên các tiêu chí sau: **")
                    .append(criteriaList)
                    .append("**.")
                    .append("\n");
        } else {
            sb.append("Hãy chấm điểm bài viết dựa trên các tiêu chí chuẩn phù hợp với loại bài (nếu không có list cụ thể).").append("\n");
        }

        sb.append("\n");

        sb.append("**4. Định dạng Output:**").append("\n\n");
        sb.append("Trả về kết quả dưới dạng một đối tượng JSON **duy nhất và hợp lệ**. Không thêm bất kỳ văn bản, giải thích hay markdown nào trước hoặc sau đối tượng JSON.").append("\n\n");
        sb.append("**QUAN TRỌNG:** Phần `overallFeedback` và nội dung trong `tips` (bao gồm cả `title` và `detail`) **phải luôn được viết bằng tiếng Việt**, bất kể ngôn ngữ của bài luận là gì.").append("\n\n");

        sb.append("Cấu trúc JSON phải tuân thủ nghiêm ngặt như sau:").append("\n");
        sb.append("{\n")
                .append("  \"score\": {\n")
                .append("    \"overall\": <tính toán điểm tổng thể dựa trên các tiêu chí>,\n")
                .append("    \"scale\": 9,\n")
                .append("    \"criteria\": [\n")
                .append("      { \"key\": \"<key_cua_tieu_chi_1>\", \"label\": \"<ten_tieu_chi_1>\", \"score\": <điểm>, \"max\": 9 },\n")
                .append("      { \"key\": \"<key_cua_tieu_chi_2>\", \"label\": \"<ten_tieu_chi_2>\", \"score\": <điểm>, \"max\": 9 }\n")
                .append("    ]\n")
                .append("  },\n")
                .append("  \"overallFeedback\": \"<Nhận xét tổng quan bằng tiếng Việt>\",\n")
                .append("  \"tips\": [\n")
                .append("    { \"title\": \"<Tiêu đề mẹo bằng tiếng Việt>\", \"detail\": \"<Chi tiết mẹo bằng tiếng Việt>\" },\n")
                .append("    { \"title\": \"<Tiêu đề mẹo bằng tiếng Việt>\", \"detail\": \"<Chi tiết mẹo bằng tiếng Việt>\" }\n")
                .append("  ]\n")
                .append("}").append("\n");

        // Kết thúc
        return sb.toString();
    }

    /**
     * Escape đơn giản cho prompt: thay newline thành space để tránh phá cấu trúc,
     * và escape dấu ngoặc kép/backslash. Nếu bạn muốn giữ newline trong phần
     * nội dung essay, có thể thay đổi behavior này.
     */
    private static String escapeForPrompt(String s) {
        if (s == null) return "";
        // thay newline bằng space để tránh format phá prompt
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
    }


    // ------------------------------------------------------------------------------------------------------------------

}
