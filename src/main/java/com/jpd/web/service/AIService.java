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
public class AIService {
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
	private String escapeForPrompt(String s) {
	    if (s == null) return "";
	    return s.replace("\\", "\\\\")
	            .replace("\"", "\\\"")
	            .replace("\n", " ");
	}

}
