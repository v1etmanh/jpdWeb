package com.jpd.web.controller.admin;

import com.jpd.web.dto.AdminCourseDetailDto;
import com.jpd.web.dto.AdminCourseDto;
import com.jpd.web.dto.ApiResponse;
import com.jpd.web.service.AdminCourseService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
@Slf4j
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    // Lấy danh sách tất cả khóa học có phân trang
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminCourseDto>>> getAllCourses(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search
    ) {
        log.info("Admin fetching courses. page={}, size={}, search={}", page, size, search);
        Page<AdminCourseDto> courses = adminCourseService.getAllCourses(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    // Lấy chi tiết khóa học theo ID
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<AdminCourseDetailDto>> getCourseById(@PathVariable Long courseId) {
    	AdminCourseDetailDto course = adminCourseService.getCourseById(courseId);
        return ResponseEntity.ok(ApiResponse.success(course));
    }

    // Khóa khóa học    
    @PostMapping("/{courseId}/ban")
    public ResponseEntity<ApiResponse<String>> banCourse(@PathVariable Long courseId) {
        adminCourseService.banCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success("Course banned successfully"));
    }

    // Mở khóa khóa học
    @PostMapping("/{courseId}/unban")
    public ResponseEntity<ApiResponse<String>> unbanCourse(@PathVariable Long courseId) {
        adminCourseService.unbanCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success("Course unbanned successfully"));
    }

    // Thay đổi trạng thái public/private
    
}