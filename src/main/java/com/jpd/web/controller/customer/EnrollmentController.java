package com.jpd.web.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.service.EnrollmentService;

@RestController
@RequestMapping("/api/enroll")
public class EnrollmentController {
@Autowired
private EnrollmentService enrollmentService;
	
	@PostMapping("/{id}")
	public ResponseEntity<?> enroll(@RequestParam("joinKey") String key,
			@PathVariable("id")long courseId, @AuthenticationPrincipal Jwt jwt) {
	    //TODO: process POST request
	    String email=jwt.getClaimAsString("email");
	    boolean a=this.enrollmentService.handlePrivateCourse(courseId, key, email);
	    if(a) return ResponseEntity.status(HttpStatus.OK).build();
	    else return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}
	
}
