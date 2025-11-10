package com.jpd.web.service;
import com.jpd.web.dto.AdminCourseDetailDto;
import com.jpd.web.dto.AdminCourseDto;
import com.jpd.web.dto.CourseDescriptionDto;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.transform.CourseTransForm;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service

public class AdminCourseService {
	@Autowired
   private  CourseInfService courseInfService;
	@Autowired
    private  CourseRepository courseRepository;

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
    public AdminCourseDetailDto getCourseById(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
      CourseDescriptionDto c1=this.courseInfService.mapToCourseDescriptionDto(course);
      AdminCourseDetailDto c2= new AdminCourseDetailDto(c1,course.getReports());
      return c2;
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
    	RatingInfo r=calculateAvtRatingAndNumberStudent(course);
        return AdminCourseDto.builder()
                .courseId(course.getCourseId())
                .name(course.getName())
                .creatorName(course.getCreator() != null ? course.getCreator().getFullName() : "Unknown")
                .isBan(course.isBan())
                
                .price(course.getPrice())
                .language(course.getLanguage())
                
                .numberReports(course.getReports().size())
                .numberStudent(r.numStudent)
                .avtRating(r.avgRating)
                
                .build();
    }
    private record RatingInfo(double avgRating, int numStudent) {
	}

	private RatingInfo calculateAvtRatingAndNumberStudent(Course course) {
		List<Enrollment> enrollments = course.getEnrollments();

		if (enrollments.isEmpty())
			return new RatingInfo(0, 0);

		int total = 0;
		double sumRating = 0;

		for (Enrollment e : enrollments) {
			if (e.getFeedback() != null) {
				total++;
				sumRating += e.getFeedback().getRate();
			}
		}

		double avgRating = total > 0 ? sumRating / total : 0;
		return new RatingInfo(avgRating, enrollments.size());
	}
	

}