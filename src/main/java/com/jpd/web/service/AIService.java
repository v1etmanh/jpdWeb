package com.jpd.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.dto.Request.AiWritingRequest;
import com.jpd.web.dto.Response.AiWritingResponse;
import com.jpd.web.exception.AIResponseParsingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.repository.CustomerRepository;

@Service
@Slf4j
public class AIService {
	@Autowired
	private GeminiAiService geminiAiService;
    @Autowired
    private ObjectMapper objectMapper;
	@Autowired
	private CustomerRepository customerRepository;

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
	private String escapeForPrompt(String s) {
	    if (s == null) return "";
	    return s.replace("\\", "\\\\")
	            .replace("\"", "\\\"")
	            .replace("\n", " ");
	}
    // ------------------------------------------------------------------------------------------------------------------
    //                       AI Writing

    public AiWritingResponse gradeEssay(AiWritingRequest request){
        String requirement = (request.getRequirement()==null ||request.getRequirement().isBlank())
                ? "Không có" : request.getRequirement().trim();
        String promt = buildPrompt(request.getLanguage().trim(),request.getQuestion().trim(),requirement,request.getText().trim());

        String aiRaw = geminiAiService.generateContent(promt);
        log.info("AI Grade Essay Response: {}", aiRaw);
        String json = extractFirstJsonObject(aiRaw);
        log.info("AI Grade Essay JSON: {}", json);
        try{
            return objectMapper.readValue(json, AiWritingResponse.class);
        }catch (Exception e){
            throw new AIResponseParsingException("Gemini returned invalid JSON", aiRaw, e);
        }

    }

    private String buildPrompt(String language, String question, String requirement, String essay) {
        // Prompt chính xác theo yêu cầu của bạn
        return """
Bạn là một giám khảo chấm thi viết IELTS (cho tiếng Anh) và JLPT (cho tiếng Nhật) giàu kinh nghiệm. Hãy phân tích bài luận dưới đây dựa trên các tiêu chí chấm điểm chuẩn, có xem xét kỹ đề bài và các yêu cầu đi kèm.

Ngôn ngữ bài viết: %s

Đề bài (Question):
---
%s
---

Yêu cầu cụ thể (Requirements):
---
%s
---

Bài luận cần chấm:
---
%s
---

Hãy trả về kết quả dưới dạng một đối tượng JSON **duy nhất và hợp lệ**. Không thêm bất kỳ văn bản, giải thích hay markdown nào trước hoặc sau đối tượng JSON. Cấu trúc JSON phải tuân thủ nghiêm ngặt như sau:

{
  "score": {
    "overall": <số điểm tổng thể, ví dụ: 6.5>,
    "scale": 9,
    "criteria": [
      { "key": "task_response", "label": "Task Response", "score": <điểm>, "max": 9 },
      { "key": "coherence_cohesion", "label": "Coherence & Cohesion", "score": <điểm>, "max": 9 },
      { "key": "lexical_resource", "label": "Lexical Resource", "score": <điểm>, "max": 9 },
      { "key": "grammar", "label": "Grammatical Range & Accuracy", "score": <điểm>, "max": 9 }
    ]
  },
  "overallFeedback": "<Nhận xét tổng quan về bài viết, khoảng 2-3 câu>",
  "tips": [
    { "title": "<Tiêu đề ngắn gọn cho mẹo cải thiện 1>", "detail": "<Mô tả chi tiết và cụ thể cho mẹo 1>" },
    { "title": "<Tiêu đề ngắn gọn cho mẹo cải thiện 2>", "detail": "<Mô tả chi tiết và cụ thể cho mẹo 2>" }
  ]
}
""".formatted(language, question, requirement, essay);
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

    // ------------------------------------------------------------------------------------------------------------------

}
