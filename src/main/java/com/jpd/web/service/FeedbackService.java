package com.jpd.web.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.exception.BusinessException;
import com.jpd.web.exception.ExceedLimitRequestException;
import com.jpd.web.exception.FeedBackIligalException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Course;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.repository.FeedbackRepository;
import com.jpd.web.service.utils.CommentFilterService;
import com.jpd.web.service.utils.ValidationResources;

@Service
public class FeedbackService {
@Autowired
private FeedbackRepository feedbackRepository;
@Autowired
private EnrollmentRepository enrollmentRepository;
@Autowired
private ValidationResources validationResources;
@Autowired
private CommentFilterService commentFilterService;
public void addFeedback(String email,long courseId,String detail,int rate) {

	Customer c=	this.validationResources.validateCustomerExist(email);
	Optional<Enrollment> eo=this.enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(courseId, c.getCustomerId());
	if(eo.isEmpty())throw new UnauthorizedException("you dont own this cours");
 Feedback f1=eo.get().getFeedback();

	if(f1!=null)throw new ExceedLimitRequestException("use can only feedback 1 per");
	if(commentFilterService.isToxic(detail))throw new FeedBackIligalException(detail);
	Feedback f=Feedback.builder()
			.content(detail)
			.enrollment(eo.get())
			.rate(rate)
			.build();
	this.feedbackRepository.save(f);
	return ;
		
}
}
