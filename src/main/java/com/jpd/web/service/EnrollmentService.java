package com.jpd.web.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.utils.ValidationResources;

@Service
public class EnrollmentService {
	@Autowired
	private ValidationResources validationResources;
	
	@Autowired
	private EnrollmentRepository enrollmentRepository;
public List<Enrollment> findByCourseId(long courseId){
	Course c=validationResources.validateCourseExists(courseId);
 List<Enrollment>ers=	this.enrollmentRepository.findByCourse(c);
 ers.forEach(e->e.getFeedback());
 return ers;
}
}
