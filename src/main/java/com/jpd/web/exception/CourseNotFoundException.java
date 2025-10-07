package com.jpd.web.exception;

public class CourseNotFoundException extends BusinessException {
    public CourseNotFoundException(Long id) {
        super("Course not found with id: " + id);
    }
}