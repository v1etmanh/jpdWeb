package com.jpd.web.transform;

import com.jpd.web.dto.CourseInfDto;
import com.jpd.web.model.Course;

public class CourseInfoTransform {

    public static CourseInfDto toCourseInfoDto(Course course) {
        return CourseInfDto.builder()
                .id(course.getCourseId())
                .img(course.getUrlImg())
                .name(course.getName())
                .price(course.getPrice())
                .language(course.getLanguage())
                .instructor(course.getCreator().getFullName())
                .build();
    }

}
