package com.jpd.web.mapper;


import com.jpd.web.dto.CourseProgressDto;
import com.jpd.web.model.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    CourseMapper INSTANCE = Mappers.getMapper(CourseMapper.class);

    @Mapping(source = "courseId",target = "courseId")
    @Mapping(source = "name",target = "course_name")
    @Mapping(source = "urlImg",target = "course_img")
    CourseProgressDto courseToCourseProgressDto(Course course);


}
