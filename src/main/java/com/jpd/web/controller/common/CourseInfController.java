package com.jpd.web.controller.common;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.CourseInfDto;
import com.jpd.web.dto.CourseLearningCardDto;
import com.jpd.web.service.CourseInfService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/course")
public class CourseInfController 
{
	@Autowired
	private CourseInfService courseInfService;
@GetMapping("/recommend_courses")
public ResponseEntity<List<CourseInfDto>> getMethodName() {
    return ResponseEntity.ok(courseInfService.getRecommendCourses());
}
@GetMapping("/search")
public ResponseEntity<?> searchCourses(
        @RequestParam String name,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
) {  
    Page<CourseInfDto> result = this.courseInfService.searchByKey(name, page, size);

    return ResponseEntity.ok(Map.of(
        "content", result.getContent(),
        "currentPage", result.getNumber(),
        "totalPages", result.getTotalPages(),
        "totalElements", result.getTotalElements(),
        "size", result.getSize(),
        "hasNext", result.hasNext(),
        "hasPrevious", result.hasPrevious()
    ));
}

@GetMapping("/{id}")
public ResponseEntity<?>retrieveCourseDetail(@PathVariable("id") long id){
	
	return ResponseEntity.ok( this.courseInfService.getCourseDescription(id));
}


}
