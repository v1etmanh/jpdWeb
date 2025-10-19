package com.jpd.web.exception;


public class CreatorNotFoundException extends BusinessException {
	 public CreatorNotFoundException(Long id) {
	        super("CREATOR_NOT_FOUND", "Creator not found with id: " + id);
	    }
}
