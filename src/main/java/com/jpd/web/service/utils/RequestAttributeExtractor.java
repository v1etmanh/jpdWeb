package com.jpd.web.service.utils;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
@Component
public class RequestAttributeExtractor {
	
	 public static Long extractCreatorId(HttpServletRequest request) {
	        Object creatorIdObj = request.getAttribute("creatorId");
	        if (creatorIdObj == null) {
	            throw new IllegalArgumentException("Creator ID not found in request");
	        }
	        return (Long) creatorIdObj;
	    }
}
