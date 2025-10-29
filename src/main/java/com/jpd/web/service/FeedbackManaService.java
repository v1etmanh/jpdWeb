package com.jpd.web.service;

import com.jpd.web.dto.CustomerSimpleDto;
import com.jpd.web.dto.FeedbackSimpleDto;
import com.jpd.web.exception.FeedbackNotFoundException;
import com.jpd.web.model.Feedback;
import com.jpd.web.repository.FeedbackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FeedbackManaService {
    @Autowired
    private FeedbackRepository feedbackRepository;

    // 1 khóa học: lấy tất cả feedback
    public List<FeedbackSimpleDto> getFeedbackOfCourse(Long courseId) {
        log.info("Getting feedback of course ID: {}", courseId);

        return feedbackRepository.findByEnrollment_Course_CourseId(courseId)
                .stream()
                .filter(f -> f.getEnrollment() != null && f.getEnrollment().getCustomer() != null)
                .map(f -> new FeedbackSimpleDto(
                        f.getFeedbackId(),
                        f.getContent(),
                        f.getRate(),
                        f.getCreateAt(), // feedback.createAt
                        new CustomerSimpleDto(
                                f.getEnrollment().getCustomer().getCustomerId(),
                                f.getEnrollment().getCustomer().getUsername(),
                                null
                        )
                ))
                .toList();
    }

    // Lịch sử feedback của 1 customer
    public List<FeedbackSimpleDto> getFeedbackHistory(Long customerId) {
        log.info("Getting feedback history for customer ID: {}", customerId);

        return feedbackRepository.findByEnrollment_Customer_CustomerId(customerId)
                .stream()
                .filter(f -> f.getEnrollment() != null && f.getEnrollment().getCustomer() != null)
                .map(f -> new FeedbackSimpleDto(
                        f.getFeedbackId(),
                        f.getContent(),
                        f.getRate(),
                        f.getCreateAt(), // feedback.createAt
                        new CustomerSimpleDto(
                                f.getEnrollment().getCustomer().getCustomerId(),
                                f.getEnrollment().getCustomer().getUsername(),
                                null
                        )
                ))
                .toList();
    }

    //delete feedback
    public void deleteFeedback(Long feedbackId) {
        log.info("Deleting feedback: {}", feedbackId);
        if (!feedbackRepository.existsById(feedbackId)) {
            throw new FeedbackNotFoundException(feedbackId);
        }
        feedbackRepository.deleteById(feedbackId);
    }

    //average feedback của 1 khóa học
    public double getAverageRatingByCourseId (Long courseId) {
        log.info("Getting average ratings for course ID: {}", courseId);
        Double avg = feedbackRepository.getAverageRatingByCourseId(courseId);
        return avg != null ? avg : 0.0;
    }
}
