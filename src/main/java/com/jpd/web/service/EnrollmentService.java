package com.jpd.web.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.EnrollmentDto;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.EnrollmentExistException;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Enrollment;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.utils.ValidationResources;

@Service
public class EnrollmentService {
	@Autowired
	private ValidationResources validationResources;
	@Autowired
	private CourseRepository courseRepository;
	@Autowired
	private EnrollmentRepository enrollmentRepository;
	@Autowired
	private CustomerRepository customerRepository;
public List<Enrollment> findByCourseId(long courseId,long creatorId){
	Course c=validationResources.validateCourseExists(courseId);
	validationResources.validateCourseOwnership(courseId, creatorId);
 List<Enrollment>ers=	this.enrollmentRepository.findByCourse(c);
 ers.forEach(e->e.getFeedback());
 return ers;
}
public boolean handlePrivateCourse(long courseId,String code,String email) {
	
	Optional<Customer>cus=this.customerRepository.findByEmail(email);
	
	Optional<Course> course=this.courseRepository.findById(courseId);
	if(course.isEmpty()) {
		throw new CourseNotFoundException(courseId);
	}
	Optional<Enrollment> e1=this.enrollmentRepository.findByCourse_CourseIdAndCustomer_CustomerId(courseId, cus.get().getCustomerId());
	if(e1.isPresent()) {throw new EnrollmentExistException("you have been enrroll");}
	if(course.get().getAccessMode()==AccessMode.PAID) {
		throw new CourseNotFoundException(courseId);
	}
	
	else if(course.get().getAccessMode()==AccessMode.PRIVATE) {
	if(!course.get().getJoinKey().equals(code)) {
		return false;
	}
	}
	Enrollment e=Enrollment.builder()
			.course(course.get())
			.customer(cus.get())
			.build();
	this.enrollmentRepository.save(e);
	return true;
	
	
}
}
