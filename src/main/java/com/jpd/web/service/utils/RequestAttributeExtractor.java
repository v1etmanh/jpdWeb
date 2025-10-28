package com.jpd.web.service.utils;

import org.springframework.stereotype.Component;

import com.jpd.web.exception.CreatorIdNotFoundInRequestException;

import jakarta.servlet.http.HttpServletRequest;
@Component
public class RequestAttributeExtractor {
	
	 public static Long extractCreatorId(HttpServletRequest request) {
	        Object creatorIdObj = request.getAttribute("creatorId");
	        if (creatorIdObj == null) {
	        	 throw new CreatorIdNotFoundInRequestException(
	                     "Creator ID has invalid type: " + creatorIdObj.getClass().getName()
	                 );
	        }
	        return (Long) creatorIdObj;
	    }
	
}
