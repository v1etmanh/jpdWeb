package com.jpd.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.exception.AIHandlerException;
import com.jpd.web.repository.CustomerRepository;

import lombok.extern.slf4j.Slf4j;


import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AIService {
	@Autowired
	private GeminiAiService geminiAiService;
	@Autowired
	private FireBaseService fireBaseService;
	private final ObjectMapper objectMapper = new ObjectMapper();
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
	  public List<String> analyzeTask1ImageFromUrl(String imageUrl, String question) 
	            throws AIHandlerException {
	        
	        validateInputs(imageUrl, question);
	        
	        try {
	            log.info("📥 Bắt đầu tải ảnh từ URL: {}", imageUrl);
	            
	            // Bước 1: Tải ảnh từ URL Firebase
	            byte[] imageBytes = fireBaseService.getFileFromUrl(imageUrl);
	            log.info("✅ Tải ảnh thành công, kích thước: {} bytes", imageBytes.length);
	            
	            // Bước 2: Xác định MIME type
	            String mimeType = detectMimeType(imageUrl);
	            log.info("📋 MIME type: {}", mimeType);
	            
	            // Bước 3: Convert to base64
	            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
	            log.info("🔄 Convert to base64, kích thước: {} chars", base64Image.length());
	            
	            // Bước 4: Gửi đến Gemini AI
	            List<String> features = analyzeTask1ImageWithBase64(base64Image, mimeType, question);
	            log.info("✅ Phân tích thành công, tìm được {} features", features.size());
	            
	            return features;
	            
	        } catch (IOException e) {
	            log.error("❌ Lỗi tải ảnh từ URL: {}", e.getMessage());
	            throw new AIHandlerException("Lỗi tải ảnh từ URL: " + e.getMessage());
	        } catch (AIHandlerException e) {
	            throw e;
	        } catch (Exception e) {
	            log.error("❌ Lỗi phân tích ảnh: {}", e.getMessage());
	            throw new AIHandlerException("Lỗi phân tích ảnh: " + e.getMessage());
	        }
	    }

	    /**
	     * Analyze IELTS Task 1 từ base64 image
	     */
	  private List<String> analyzeTask1ImageWithBase64(String base64Image, String mimeType, String question)
		        throws AIHandlerException {
		    
		    validateInputs(base64Image, question);
		    
		    try {
		        String prompt = """
		You are an IELTS Task 1 analyzer. Extract 5-8 key features students MUST mention.

		Question: %s

		Respond with ONLY a JSON array. Format:
		["Feature 1", "Feature 2", "Feature 3"]

		Rules:
		- Start with [
		- End with ]
		- No markdown code blocks
		- No explanations
		- Only JSON array
		""".formatted(question);

		        String response = geminiAiService.generateContentWithImage(prompt, base64Image, mimeType);
		        log.info("🤖 Response received, length: {}", response.length());
		        
		        List<String> features = parseFeatures(response);
		        
		        if (features.isEmpty()) {
		            log.warn("⚠️ No features extracted, returning empty list");
		            return new ArrayList<>();
		        }
		        
		        return features;
		        
		    } catch (Exception e) {
		        log.error("❌ Error analyzing image: {}", e.getMessage());
		        // Return empty list instead of throwing
		        return new ArrayList<>();
		    }
		}
	    /**
	     * Parse JSON response từ Gemini
	     */
	  private List<String> parseFeatures(String response) throws AIHandlerException {
		    try {
		        if (response == null || response.trim().isEmpty()) {
		            throw new AIHandlerException("Response trống từ Gemini");
		        }
		        
		        String cleanedResponse = response.trim();
		        log.info("📥 Raw response: {}", cleanedResponse);
		        log.info("📊 Response length: {}", cleanedResponse.length());
		        
		        // Bước 1: Xóa markdown wrappers
		        cleanedResponse = cleanedResponse
		            .replaceAll("(?s)```[\\w]*\\n?", "")  // Xóa ```
		            .replaceAll("(?s)```\\n?", "")
		            .trim();
		        
		        log.info("🧹 After cleanup: {}", cleanedResponse);
		        
		        // Bước 2: Tìm JSON array
		        int jsonStart = cleanedResponse.indexOf('[');
		        int jsonEnd = cleanedResponse.lastIndexOf(']');
		        
		        log.info("🔍 JSON start index: {}, end index: {}", jsonStart, jsonEnd);
		        
		        if (jsonStart == -1 || jsonEnd == -1) {
		            log.error("❌ Không tìm thấy JSON array []");
		            log.error("Response content: {}", cleanedResponse);
		            
		            // Fallback: Nếu không tìm được array, trả về empty list
		            return new ArrayList<>();
		        }
		        
		        if (jsonStart > jsonEnd) {
		            throw new AIHandlerException("JSON array không hợp lệ: ] trước [");
		        }
		        
		        String jsonArray = cleanedResponse.substring(jsonStart, jsonEnd + 1);
		        log.info("✂️ Extracted JSON: {}", jsonArray);
		        
		        // Bước 3: Validate JSON syntax
		        if (!jsonArray.startsWith("[") || !jsonArray.endsWith("]")) {
		            throw new AIHandlerException("JSON không bắt đầu bằng [ hoặc kết thúc bằng ]");
		        }
		        
		        // Bước 4: Parse với error detail
		        List<String> features = objectMapper.readValue(
		            jsonArray, 
		            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
		        );
		        
		        log.info("✅ Parsed {} features thành công", features.size());
		        features.forEach(f -> log.info("  - {}", f));
		        
		        return features;
		        
		    } catch (JsonParseException e) {
		        log.error("❌ JSON Parse Error: {} at line {}, col {}", 
		            e.getOriginalMessage(), 
		            e.getLocation().getLineNr(),
		            e.getLocation().getColumnNr());
		        log.error("Response snippet: {}", response.substring(0, Math.min(500, response.length())));
		        
		        return new ArrayList<>();  // Return empty list thay vì throw
		        
		    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
		        log.error("❌ JSON Mapping Error: {}", e.getMessage());
		        log.error("Full response: {}", response);
		        
		        return new ArrayList<>();
		        
		    } catch (Exception e) {
		        log.error("❌ Unexpected error parsing features: {}", e.getMessage());
		        log.error("Exception class: {}", e.getClass().getName());
		        log.error("Response: {}", response);
		        
		        return new ArrayList<>();
		    }
		}

	    /**
	     * Phát hiện MIME type từ URL
	     */
	    private String detectMimeType(String url) {
	        String lowerUrl = url.toLowerCase();
	        
	        if (lowerUrl.contains(".png")) return "image/png";
	        if (lowerUrl.contains(".gif")) return "image/gif";
	        if (lowerUrl.contains(".webp")) return "image/webp";
	        if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) return "image/jpeg";
	        
	        return "image/jpeg"; // Default
	    }

	    /**
	     * Validation inputs
	     */
	    private void validateInputs(String... inputs) throws AIHandlerException {
	        for (String input : inputs) {
	            if (!StringUtils.hasText(input)) {
	                throw new AIHandlerException("Input không hợp lệ");
	            }
	        }
	    }
    
    
}
