package com.jpd.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseFormDto;
import com.jpd.web.model.Course;
import com.jpd.web.service.CourseService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/course")
public class CourseController {
@Autowired
private CourseService courseService;
@PostMapping(value =  "/create",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<?> createCourse(@Valid @ModelAttribute  CourseFormDto entity,@AuthenticationPrincipal Jwt jwt) {
    //TODO: process POST request
    Course c;
	try {
		c = this.courseService.createCourse(entity, jwt.getClaimAsString("email"));
		 return ResponseEntity.status(HttpStatus.OK).body(c);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		 return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}
   
}
@GetMapping("/retrieveByEmail")
public ResponseEntity<List<CourseCardDto>>retrieveAll(@AuthenticationPrincipal Jwt jwt) {
    try {
    List<CourseCardDto>courses=	this.courseService.retrieveCourseByemail(jwt.getClaimAsString("email"));
    return ResponseEntity.status(HttpStatus.OK).body(courses);
    }
    catch (Exception e) {
		// TODO: handle exception
    	System.out.print(e);
    	return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    	
	}
}


}
