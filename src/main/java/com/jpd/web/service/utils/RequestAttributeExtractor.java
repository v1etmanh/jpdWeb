package com.jpd.web.service.utils;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
@Component
public class RequestAttributeExtractor {


    //code Manh
//	 public static Long extractCreatorId(HttpServletRequest request) {
//	        Object creatorIdObj = request.getAttribute("creatorId");
//	        if (creatorIdObj == null) {
//	            throw new IllegalArgumentException("Creator ID not found in request");
//	        }
//	        return (Long) creatorIdObj;
//	    }


    //code test api
    public static Long extractCreatorId(HttpServletRequest request) {
        String creatorIdObj = request.getParameter("creatorId");
        if (creatorIdObj == null) {
            throw new IllegalArgumentException("Creator ID not found in request");
        }
        return Long.parseLong(creatorIdObj);
    }
}
