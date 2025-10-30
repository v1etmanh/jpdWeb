package com.jpd.web.controller.creator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.model.Enrollment;
import com.jpd.web.service.EnrollmentService;
import com.jpd.web.service.utils.RequestAttributeExtractor;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("api/creator/enrollment")
public class EnrollmentCreatorController {
@Autowired
private EnrollmentService enrollmentService;
@GetMapping("/{courseId}")
public ResponseEntity<List<Enrollment>> retrieveByCourse(@PathVariable("courseId") long courseId,
		HttpServletRequest request) {
	 long creatorId=RequestAttributeExtractor.extractCreatorId(request);
  return ResponseEntity.status(HttpStatus.OK).body( this.enrollmentService.findByCourseId(courseId,creatorId));
}

}
