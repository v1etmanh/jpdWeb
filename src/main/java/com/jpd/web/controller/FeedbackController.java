package com.jpd.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.jpd.web.service.FeedbackService;

import jakarta.websocket.server.PathParam;

@RestController
@RequestMapping("/api/customer/feedback")
public class FeedbackController {
    @Autowired
    private FeedbackService feedbackService;

    @PostMapping("/{courseId}")
    public ResponseEntity<?> addFeedback(@PathVariable("courseId") long courseId,
                                         @RequestParam("rate") int rate
            , @RequestParam("detail") String detail,
                                         @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        this.feedbackService.addFeedback(email, courseId, detail, rate);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> deleteFeedback(@PathVariable("courseId") long courseId,
                                            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        this.feedbackService.deleteFeedback(email, courseId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<?> updateFeedback(@RequestParam("courseId") long courseId,
                                            @RequestParam("rate") int rate,
                                            @RequestParam("detail") String detail,
                                            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok().body(this.feedbackService.updateFeedback(email, courseId, detail, rate)
        );

    }
}
