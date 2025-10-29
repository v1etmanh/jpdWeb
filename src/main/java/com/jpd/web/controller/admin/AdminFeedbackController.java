package com.jpd.web.controller.admin;

import com.jpd.web.dto.FeedbackSimpleDto;
import com.jpd.web.service.FeedbackManaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/feedback")
@Slf4j
public class AdminFeedbackController {

    @Autowired
    private FeedbackManaService feedbackManaService;

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<List<FeedbackSimpleDto>> getFeedbackOfCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(feedbackManaService.getFeedbackOfCourse(courseId));
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<List<FeedbackSimpleDto>> getFeedbackHistory(@PathVariable Long customerId) {
        return ResponseEntity.ok(feedbackManaService.getFeedbackHistory(customerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFeedback(@PathVariable Long id) {
        feedbackManaService.deleteFeedback(id);
        return ResponseEntity.ok("Feedback deleted successfully.");
    }

    @GetMapping("/courses/{courseId}/average-rating")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long courseId) {
        double avg = feedbackManaService.getAverageRatingByCourseId(courseId);
        return ResponseEntity.ok(avg);
    }
}
