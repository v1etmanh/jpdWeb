package com.jpd.web.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.service.FeedbackService;

@RestController
@RequestMapping("/api/customer/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    // Constructor injection thay vì field injection
    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/{courseId}")
    public ResponseEntity<?> addFeedback(
            @PathVariable("courseId") long courseId,
            @RequestParam("rate") int rate,
            @RequestParam("detail") String detail,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        this.feedbackService.addFeedback(email, courseId, detail, rate);
        return ResponseEntity.noContent().build();
    }
}