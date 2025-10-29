package com.jpd.web.controller.admin;

import com.jpd.web.dto.CourseEditRequestDto;
import com.jpd.web.model.Course;
import com.jpd.web.service.CourseManaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/courses")
@Slf4j
public class AdminCourseController {

    @Autowired
    private CourseManaService courseManaService;

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseManaService.getAllCourses());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCourse(@PathVariable Long id) {
        courseManaService.deleteCourse(id);
        return ResponseEntity.ok("Course deleted successfully.");
    }
}
