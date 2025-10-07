package com.jpd.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.GenerateFeedbackForm;
import com.jpd.web.service.AIService;

@RequestMapping("/api/creator/AI")
@RestController
public class AiGenerateController {
	@Autowired
	private AIService aiService;
	@PostMapping("/generateFeeback")
	public ResponseEntity<?> generateFeedback( @RequestBody GenerateFeedbackForm form ) throws IllegalAccessException{
		
		String feedBack=this.aiService.generateFeedback(form.getQuestion(), form.getAnswer());
		
		return ResponseEntity.status(HttpStatus.CREATED).body(feedBack);
	}
}
