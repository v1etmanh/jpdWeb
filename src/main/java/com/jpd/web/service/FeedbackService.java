package com.jpd.web.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import com.jpd.web.repository.FeedbackRepository;

@Service
public class FeedbackService {
@Autowired
private FeedbackRepository feedbackRepository;

public List<Feedback> getFeedbackBycourseId(Enrollment e){
	return this.feedbackRepository.findByEnrollment(e);
}
}
