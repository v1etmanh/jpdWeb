package com.jpd.web.controller.common;

import java.util.List;
import java.util.Map;

import com.jpd.web.dto.CourseSearchDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public ResponseEntity<?> etMethodName(@RequestParam String name, Pageable pageable) {
        Page<CourseSearchDto> coursePage = courseInfService.searchAndPagination(name, pageable);
        return ResponseEntity.ok(coursePage);
    }

@GetMapping("/{id}")
public ResponseEntity<?>retrieveCourseDetail(@PathVariable("id") long id){
	
	return ResponseEntity.ok( this.courseInfService.getCourseDescription(id));
}


}
