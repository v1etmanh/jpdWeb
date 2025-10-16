package com.jpd.web.service;

import com.jpd.web.dto.CourseInfDto;
import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.transform.CourseInfoTransform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseInforService {

    @Autowired
    CourseRepository courseRepository;

    private final double rateWeight = 0.7;
    private final double numberOfStudentWeight = 0.3;

    @Cacheable(value = "recommendations", unless = "#result == null or #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<CourseInfDto> getAllRecommendCourse() {
        List<String> languageList = courseRepository.findDistinctLanguages();
        List<CourseInfDto> res = new ArrayList<>();
        for (String language : languageList) {
            List<Course> langCourseList = courseRepository.findByLanguage(language);
            langCourseList.sort((c1, c2) -> {
                int numberOfStudent1 = calculateNumberOfStudent(c1);
                double averageRate1 = calculateAverageRate(c1);
                int numberOfStudent2 = calculateNumberOfStudent(c2);
                double averageRate2 = calculateAverageRate(c2);
                double weight1 = calculateWeightOfCourse(averageRate1, numberOfStudent1);
                double weight2 = calculateWeightOfCourse(averageRate2, numberOfStudent2);
                return Double.compare(weight1, weight2);
            });

            List<Course> topThreeCourse = langCourseList.stream().limit(3).toList();
            for (Course c : topThreeCourse) {
                long numberOfStudent = calculateNumberOfStudent(c);
                double averageRate = calculateAverageRate(c);
                CourseInfDto dto = CourseInfoTransform.toCourseInfoDto(c);
                dto.setNumberOfStudent(numberOfStudent);
                dto.setRating(averageRate);
                res.add(dto);
            }
        }
        return res;
    }


    private int calculateNumberOfStudent(Course course) {
        return course.getEnrollments().size();
    }

    private double calculateAverageRate(Course course) {
        List<Enrollment> enrollmentsList = course.getEnrollments();
        double totalOfScoreFeedback = 0.0;
        double totalOfFeedback = 0.0;
        for (Enrollment e : enrollmentsList) {
            if (e.getFeedback() != null) {
                totalOfFeedback++;
                totalOfScoreFeedback += e.getFeedback().getRate();
            }
        }
        return (totalOfFeedback != 0) ? totalOfScoreFeedback / totalOfFeedback : 0;
    }


    private double calculateWeightOfCourse(double avrRating, int numberOfStudent) {
        return rateWeight * avrRating + numberOfStudentWeight * numberOfStudent;
    }


}
