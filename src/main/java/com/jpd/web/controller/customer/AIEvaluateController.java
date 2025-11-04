package com.jpd.web.controller.customer;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.WritingScores;
import com.jpd.web.dto.WritingTextEvaluateForm;
import com.jpd.web.model.Language;
import com.jpd.web.model.SemanticResult;
import com.jpd.web.service.AIService;
import com.jpd.web.service.AiEvaluateService;


import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/customer/evaluate")
@Slf4j
public class AIEvaluateController {
@Autowired
private AiEvaluateService aiEvaluateService;
@Autowired
private AIService aiService;
@PostMapping("/evaluate")
public ResponseEntity<SemanticResult> evaluateAnswer(
		@RequestParam("audio") MultipartFile file,
                                                   @RequestParam("sentence") String expectedAnswer,
                                                 @RequestParam(value = "language", required = false) String language,@AuthenticationPrincipal Jwt jwt) {
	 String email=jwt.getClaimAsString("email");


    
	try {
        
        SemanticResult result = aiEvaluateService.evaluateSpeaking(file, expectedAnswer, language);
        
        
        return ResponseEntity.ok(result);
        
    } catch (Exception e) {
        log.error("Lỗi khi xử lý audio: ", e);
        return ResponseEntity.badRequest().build();
    }
}
@PostMapping("/evaluateWriting")
public ResponseEntity<?> evaluateWritingText(@RequestBody WritingTextEvaluateForm form)
{ 
	WritingScores score=this.aiService.evaluateWritingSimple(form.getWritingText(), form.getLanguage());
	
	return ResponseEntity.ok(score);
}
}
