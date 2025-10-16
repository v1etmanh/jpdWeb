package com.jpd.web.controller;


import com.jpd.web.dto.CourseInfDto;
import com.jpd.web.service.CourseInforService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController()
@RequestMapping("/api/course_inf")
public class CourseInfController {
    private final CourseInforService courseInforService;

    // Sử dụng constructor injection cho dễ testing
    @Autowired
    public CourseInfController(CourseInforService courseInforService) {
        this.courseInforService = courseInforService;
    }


    @GetMapping("")
    public ResponseEntity<List<CourseInfDto>> getAllRecommendCourses() {
        List<CourseInfDto> list = courseInforService.getAllRecommendCourse();
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }


}
