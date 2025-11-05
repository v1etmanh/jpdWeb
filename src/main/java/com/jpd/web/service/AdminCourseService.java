package com.jpd.web.service;
import com.jpd.web.dto.AdminCourseDto;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.model.Course;
import com.jpd.web.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCourseService {

    private final CourseRepository courseRepository;

    // Lấy danh sách tất cả khóa học có phân trang và search
    public Page<AdminCourseDto> getAllCourses(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Course> coursePage;

        if (search == null || search.isEmpty()) {
            coursePage = courseRepository.findAll(pageable);
        } else {
            coursePage = courseRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search, search, pageable
            );
        }

        return coursePage.map(this::transformToAdminDTO);
    }

    // Lấy chi tiết khóa học theo ID
    public AdminCourseDto getCourseById(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        return transformToAdminDTO(course);
    }

    // Khóa khóa học
    @Transactional
    public void banCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        course.setBan(true);
        courseRepository.save(course);
    }

    // Mở khóa khóa học
    @Transactional
    public void unbanCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        course.setBan(false);
        courseRepository.save(course);
    }

    // Đổi trạng thái public/private
    @Transactional
    public void toggleCoursePublicStatus(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        course.setPublic(!course.isPublic());
        courseRepository.save(course);
    }

    // Transform Course -> AdminCourseDTO
    private AdminCourseDto transformToAdminDTO(Course course) {
        return AdminCourseDto.builder()
                .courseId(course.getCourseId())
                .name(course.getName())
                .creatorName(course.getCreator() != null ? course.getCreator().getFullName() : "Unknown")
                .isBan(course.isBan())
                .isPublic(course.isPublic())
                .price(course.getPrice())
                .language(course.getLanguage())
                .teachingLanguage(course.getTeachingLanguage())
                .build();
    }
}
