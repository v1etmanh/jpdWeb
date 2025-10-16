package com.jpd.web.dto;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseProgressDto {

    Long courseId;
    String course_name;
    String course_img;
    int progress;

}
