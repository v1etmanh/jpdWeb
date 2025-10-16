package com.jpd.web.dto;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningCourseDto {

    String name;
    String urlImg;
    int progress;


}
