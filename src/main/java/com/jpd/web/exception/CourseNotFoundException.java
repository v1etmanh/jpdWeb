package com.jpd.web.exception;

public class CourseNotFoundException extends BusinessException {
	 public CourseNotFoundException(Long id) {
	        super("COURSE_NOT_FOUND", "Course not found with id: " + id);
	    }
}