package com.jpd.web.exception;


public class CreatorNotFoundException extends RuntimeException{
	   public CreatorNotFoundException(Long id) {
	        super("Course not found with id: " + id);
	    }
}
