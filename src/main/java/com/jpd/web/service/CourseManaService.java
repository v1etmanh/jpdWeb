package com.jpd.web.service;

import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.model.Course;
import com.jpd.web.repository.CourseManaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CourseManaService {
    @Autowired
    private CourseManaRepository courseManaRepository;

    public List<Course> getAllCourses() {
        return courseManaRepository.findAll();
    }

    public Optional<Course> getCourseById(Long courseId) {
        return courseManaRepository.findById(courseId);
    }

    public Optional<Course> updateCourse(Long courseId, Course updatedCourse) {
        return courseManaRepository.findById(courseId).map(course -> {

            course.setName(updatedCourse.getName());
            course.setDescription(updatedCourse.getDescription());
            course.setPrice(updatedCourse.getPrice());
            course.setRequirements(updatedCourse.getRequirements());
            course.setLearningObject(updatedCourse.getLearningObject());
            course.setTargetAudience(updatedCourse.getTargetAudience());
            course.setUrlImg(updatedCourse.getUrlImg());
            course.setTeachingLanguage(updatedCourse.getTeachingLanguage());
            course.setAccessMode(updatedCourse.getAccessMode());
            course.setPublic(updatedCourse.isPublic());
            return courseManaRepository.save(course);
        });
    }



    public void deleteCourse(Long courseId) {
        if (!courseManaRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        courseManaRepository.deleteById(courseId);
    }

    // ban khóa học
    @Transactional
    public void banCourse(Long courseId) {
        if (!courseManaRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        courseManaRepository.banCourse(courseId);
    }

    // bỏ ban khóa học
    @Transactional
    public void unbanCourse(Long courseId) {
        if (!courseManaRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        courseManaRepository.unbanCourse(courseId);
    }

    //đếm số người đã đăng ký khóa học
    public long countEnrollmentCourse(Long courseId) {
        if (!courseManaRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        return courseManaRepository.countEnrollmentCourse(courseId);
    }


}
