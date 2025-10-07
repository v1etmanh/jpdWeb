package com.jpd.web.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.repository.CustomerRepository;

@Service
public class AIService {
	@Autowired
	private GeminiAiService geminiAiService;
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
}
