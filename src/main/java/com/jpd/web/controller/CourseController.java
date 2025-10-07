package com.jpd.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.dto.CourseFormDto;
import com.jpd.web.dto.GenerateFeedbackForm;
import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.dto.ModuleDto;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.service.CourseService;
import com.jpd.web.service.FireBaseService;
import com.jpd.web.service.utils.RequestAttributeExtractor;
import com.jpd.web.transform.CourseTransForm;
import com.nimbusds.jose.proc.SecurityContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/creator/course")
public class CourseController {
	@Autowired
	private CourseService courseService;

	@PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CourseCardDto> createCourse(@Valid @ModelAttribute CourseFormDto entity,
			HttpServletRequest request) {
		// TODO: process POST request
		Course c;
		
		long creatorId= RequestAttributeExtractor.extractCreatorId(request);
			c = this.courseService.createCourse(entity, creatorId);
			return ResponseEntity.status(HttpStatus.CREATED).body(CourseTransForm.transformToCourseCardDto(c));
		

	}

	@GetMapping()
	public ResponseEntity<List<CourseCardDto>> retrieveAll(HttpServletRequest httpServletRequest) {
		
			long creatorId=RequestAttributeExtractor.extractCreatorId(httpServletRequest);
			List<CourseCardDto> courses = this.courseService.retrieveCourseByemail(creatorId);
			return ResponseEntity.status(HttpStatus.OK).body(courses);
		
	}

	@GetMapping("/{id}")
	public ResponseEntity<CourseContentDto> retrieveCourseById(
			 @Positive(message = "Course ID must be positive") 
			@PathVariable("id") long id, 
			HttpServletRequest request ) {
		
			long creatorId= RequestAttributeExtractor.extractCreatorId(request);
			return ResponseEntity.status(HttpStatus.OK).body(this.courseService.getCourseById(id, creatorId));
		

	}

	public CourseController() {
		// TODO Auto-generated constructor stub
	}
	 
}
