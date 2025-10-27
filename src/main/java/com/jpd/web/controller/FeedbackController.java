package com.jpd.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.service.FeedbackService;

import jakarta.websocket.server.PathParam;

@RestController
@RequestMapping("/api/customer/feedback")
public class FeedbackController {
@Autowired
private FeedbackService feedbackService;
@PostMapping("/{courseId}")
public ResponseEntity<?>addFeedback(@PathVariable("courseId") long courseId, @RequestParam("detail")String detail,
		@AuthenticationPrincipal Jwt jwt){
	String email=jwt.getClaimAsString("email");
	this.feedbackService.addFeedback(email, courseId, detail);
	return ResponseEntity.noContent().build();
}
}
