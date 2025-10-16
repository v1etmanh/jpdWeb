package com.jpd.web.dto;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MyLearningDto {

    List<CourseProgressDto> myLearningCourse;
    List<CourseProgressDto> wishListCourses;
    List<CourseProgressDto> myCourses;


}
