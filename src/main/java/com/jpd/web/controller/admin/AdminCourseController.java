package com.jpd.web.controller.admin;

import com.jpd.web.dto.CourseBriefDto;
import com.jpd.web.dto.CourseEditRequestDto;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import com.jpd.web.service.CourseManaService;
import com.jpd.web.service.CustomerManaService;
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
    @Autowired
    private CustomerManaService customerManaService;

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseManaService.getAllCourses());
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long courseId) {
        return courseManaService.getCourseById(courseId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<String> deleteCourse(@PathVariable Long courseId) {
        courseManaService.deleteCourse(courseId);
        return ResponseEntity.ok("Course deleted successfully.");
    }

    @GetMapping("/{courseId}/enrollments")
    public ResponseEntity<List<Enrollment>> getCourseEnrollmentHistory(@PathVariable Long courseId) {
        log.info("Fetching enrollment history for course ID: {}", courseId);
        List<Enrollment> enrollments = customerManaService.getCourseEnrollmentHistory(courseId);

        if (enrollments.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(enrollments);
    }

    // Ban khóa học
    @PostMapping("/{courseId}/ban")
    public ResponseEntity<String> banCourse(@PathVariable Long courseId) {
        log.info("Banning course ID: {}", courseId);
        try {
            courseManaService.banCourse(courseId);
            return ResponseEntity.ok("Course banned successfully");
        } catch (CourseNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Bỏ ban khóa học
    @PostMapping("/{courseId}/unban")
    public ResponseEntity<String> unbanCourse(@PathVariable Long courseId) {
        log.info("Unbanning course ID: {}", courseId);
        try {
            courseManaService.unbanCourse(courseId);
            return ResponseEntity.ok("Course unbanned successfully");
        } catch (CourseNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Đếm số người đăng ký khóa học
    @GetMapping("/{courseId}/enrollments/count")
    public ResponseEntity<Long> countEnrollmentCourse(@PathVariable Long courseId) {
        log.info("Counting enrollments for course ID: {}", courseId);
        try {
            long count = courseManaService.countEnrollmentCourse(courseId);
            return ResponseEntity.ok(count);
        } catch (CourseNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    // Endpoint lấy top 3 khóa học đề xuất
    @GetMapping("/recommended")
    public ResponseEntity<List<CourseBriefDto>> getRecommendedCourses() {
        log.info("Fetching top 3 recommended courses...");
        List<CourseBriefDto> recommendedCourses = customerManaService.getRecommendedCourses();

        if (recommendedCourses.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(recommendedCourses);
    }
}
